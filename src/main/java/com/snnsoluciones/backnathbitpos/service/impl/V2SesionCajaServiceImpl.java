// src/main/java/com/snnsoluciones/backnathbitpos/service/impl/V2SesionCajaServiceImpl.java

package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.v2sesion.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.V2SesionCajaService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class V2SesionCajaServiceImpl implements V2SesionCajaService {

    private final V2SesionCajaRepository      sesionRepo;
    private final V2TurnoCajeroRepository     turnoRepo;
    private final V2MovimientoCajaRepository  movimientoRepo;
    private final V2CierreDatafonoRepository  datafonoRepo;
    private final V2SesionPlataformaRepository plataformaRepo;
    private final TerminalRepository          terminalRepo;
    private final UsuarioRepository           usuarioRepo;
    private final FacturaRepository           facturaRepo;
    private final FacturaInternaRepository    facturaInternaRepo;
    private final V2TurnoDenominacionRepository denominacionRepo;

    // ── Helper ────────────────────────────────────────────────
    private LocalDateTime ahora() {
        return LocalDateTime.now(ZoneId.of("America/Costa_Rica"));
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // =========================================================
    // ABRIR SESIÓN
    // =========================================================

    @Override
    public V2AbrirSesionResponse abrirSesion(V2AbrirSesionRequest request, Long usuarioId) {
        log.info("Abriendo sesión v2 — terminal: {}, usuario: {}", request.getTerminalId(), usuarioId);

        // 1. Validar que no haya sesión abierta en esta terminal
        sesionRepo.findAbiertaByTerminalId(request.getTerminalId()).ifPresent(s -> {
            throw new RuntimeException(
                "La terminal ya tiene una sesión abierta (id: " + s.getId() + "). Usá 'Unirse a turno'."
            );
        });

        // 2. Validar que el usuario no tenga un turno activo en otra terminal
        turnoRepo.findActivoByUsuarioId(usuarioId).ifPresent(t -> {
            throw new RuntimeException(
                "Ya tenés un turno activo en la terminal: "
                + t.getSesion().getTerminal().getNombre()
                + ". Cerrá ese turno antes de abrir otra sesión."
            );
        });

        // 3. Cargar entidades
        Terminal terminal = terminalRepo.findById(request.getTerminalId())
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));

        if (!terminal.getActiva()) {
            throw new RuntimeException("La terminal no está activa");
        }

        Usuario usuario = usuarioRepo.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 4. Crear sesión
        V2SesionCaja sesion = V2SesionCaja.builder()
            .terminal(terminal)
            .sucursal(terminal.getSucursal())
            .usuarioApertura(usuario)
            .modoGaveta(request.getModoGaveta())
            .montoInicial(nvl(request.getMontoInicial()))
            .estado("ABIERTA")
            .observaciones(request.getObservaciones())
            .build();

        sesion = sesionRepo.save(sesion);

        // 5. Crear turno para el usuario que abre
        V2TurnoCajero turno = V2TurnoCajero.builder()
            .sesion(sesion)
            .usuario(usuario)
            .fondoInicio(nvl(request.getMontoInicial())) // primer cajero = monto inicial
            .estado("ACTIVO")
            .build();

        turno = turnoRepo.save(turno);

        log.info("Sesión v2 {} abierta — turno {} creado para usuario {}",
            sesion.getId(), turno.getId(), usuarioId);

        return V2AbrirSesionResponse.builder()
            .sesionId(sesion.getId())
            .turnoId(turno.getId())
            .terminal(terminal.getNombre())
            .modoGaveta(sesion.getModoGaveta())
            .montoInicial(sesion.getMontoInicial())
            .fechaApertura(sesion.getFechaApertura())
            .build();
    }

    @Override
    public V2CerrarTurnoResponse cerrarTurno(Long turnoId, V2CerrarTurnoRequest request, Long usuarioId) {
        log.info("Cerrando turno v2 {} por usuario {}", turnoId, usuarioId);

        // 1. Cargar turno
        V2TurnoCajero turno = turnoRepo.findById(turnoId)
            .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (turno.isCerrado()) {
            throw new RuntimeException("El turno ya fue cerrado");
        }

        // 2. Validar montos
        BigDecimal montoContado  = nvl(request.getMontoContado());
        BigDecimal montoRetirado = nvl(request.getMontoRetirado());
        BigDecimal fondoCaja     = nvl(request.getFondoCaja());

        if (montoRetirado.compareTo(montoContado) > 0) {
            throw new RuntimeException(
                "No podés retirar más de lo que contaste. Contado: "
                    + montoContado + " | Retirado: " + montoRetirado
            );
        }

        if (montoContado.subtract(montoRetirado).compareTo(fondoCaja) != 0) {
            throw new RuntimeException(
                "fondoCaja debe ser montoContado - montoRetirado. "
                    + "Esperado: " + montoContado.subtract(montoRetirado)
                    + " | Recibido: " + fondoCaja
            );
        }

        Long sesionId = turno.getSesion().getId();

        // 3. Calcular ventas reales desde facturas (no del campo guardado)
        BigDecimal ventasEf    = calcularVentasTurno(turnoId, "EFECTIVO");
        BigDecimal ventasTc    = calcularVentasTurno(turnoId, "TARJETA");
        BigDecimal ventasSinpe = calcularVentasTurno(turnoId, "SINPE");
        BigDecimal ventasTb    = calcularVentasTurno(turnoId, "TRANSFERENCIA");
        BigDecimal ventasCred  = calcularVentasTurno(turnoId, "CREDITO");

        // 4. Calcular monto esperado en gaveta para este cajero
        BigDecimal entradas = nvl(movimientoRepo.sumEntradasEfectivoByTurnoId(turnoId));
        BigDecimal salidas  = nvl(movimientoRepo.sumSalidasByTurnoId(turnoId));
        BigDecimal montoEsperado;
        if ("COMPARTIDA".equals(turno.getSesion().getModoGaveta())) {
            // En gaveta compartida: todo el efectivo actual de la gaveta
            montoEsperado = calcularFondoActualGaveta(turno.getSesion().getId());
        } else {
            // En gaveta individual: solo lo del cajero
            montoEsperado = nvl(turno.getFondoInicio())
                .add(ventasEf)
                .add(entradas)
                .subtract(salidas);
        }

        // 5. Calcular diferencias
        BigDecimal difEf  = montoContado.subtract(montoEsperado);
        BigDecimal difTc  = nvl(request.getTotalTarjetaDeclarado()).subtract(ventasTc);
        BigDecimal difSin = nvl(request.getTotalSinpeDeclarado()).subtract(ventasSinpe);
        BigDecimal difTb  = nvl(request.getTotalTransferenciaDeclarado()).subtract(ventasTb);

        LocalDateTime ahora = ahora();

        // 6. Persistir retiro como movimiento SALIDA_ARQUEO
        if (montoRetirado.compareTo(BigDecimal.ZERO) > 0) {
            Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            V2MovimientoCaja retiro = V2MovimientoCaja.builder()
                .sesion(turno.getSesion())
                .turno(turno)
                .usuario(usuario)
                .tipo("SALIDA_ARQUEO")
                .monto(montoRetirado)
                .concepto("Retiro en arqueo — " + turno.getUsuario().getNombre())
                .autorizadoPor(usuario)
                .build();

            movimientoRepo.save(retiro);
        }

        // 7. Guardar datafonos
        if (request.getDatafonos() != null && !request.getDatafonos().isEmpty()) {
            List<V2CierreDatafono> datafonos = request.getDatafonos().stream()
                .map(d -> V2CierreDatafono.builder()
                    .sesion(turno.getSesion())
                    .turno(turno)
                    .datafono(d.getDatafono())
                    .monto(d.getMonto())
                    .build())
                .collect(Collectors.toList());
            datafonoRepo.saveAll(datafonos);
        }

        // 7b. Guardar denominaciones
        if (request.getDenominaciones() != null && !request.getDenominaciones().isEmpty()) {
            List<V2TurnoDenominacion> denoms = request.getDenominaciones().stream()
                .filter(d -> d.getCantidad() != null && d.getCantidad() > 0)
                .map(d -> V2TurnoDenominacion.builder()
                    .turno(turno)
                    .tipo(d.getTipo())
                    .valor(d.getValor())
                    .cantidad(d.getCantidad())
                    .subtotal(BigDecimal.valueOf((long) d.getValor() * d.getCantidad()))
                    .build())
                .collect(Collectors.toList());
            denominacionRepo.saveAll(denoms);
        }

        // 8. Cerrar turno
        turno.setEstado("CERRADO");
        turno.setFechaFin(ahora);
        turno.setVentasEfectivo(ventasEf);
        turno.setVentasTarjeta(ventasTc);
        turno.setVentasSinpe(ventasSinpe);
        turno.setVentasTransferencia(ventasTb);
        turno.setVentasCredito(ventasCred);
        turno.setMontoEsperado(montoEsperado);
        turno.setMontoContado(montoContado);
        turno.setMontoRetirado(montoRetirado);
        turno.setFondoCaja(fondoCaja);
        turno.setDifEfectivo(difEf);
        turno.setDifTarjeta(difTc);
        turno.setDifSinpe(difSin);
        turno.setDifTransferencia(difTb);
        turno.setObservacionesCierre(request.getObservaciones());
        turnoRepo.save(turno);

        log.info("Turno v2 {} cerrado — esperado: {} contado: {} dif: {}",
            turnoId, montoEsperado, montoContado, difEf);

        // 9. Verificar si era el último turno activo
        boolean sesionCerrada = turnoRepo.todosLosTurnosCerrados(sesionId);
        if (sesionCerrada) {
            cerrarSesionInterna(turno.getSesion());
        }

        String mensaje = sesionCerrada
            ? "Eras el último cajero. La sesión fue cerrada automáticamente."
            : "Turno cerrado. La sesión continúa abierta para los demás cajeros.";

        return V2CerrarTurnoResponse.builder()
            .turnoId(turnoId)
            .sesionId(sesionId)
            .sesionCerrada(sesionCerrada)
            .mensaje(mensaje)
            .ventasEfectivo(ventasEf)
            .ventasTarjeta(ventasTc)
            .ventasSinpe(ventasSinpe)
            .ventasTransferencia(ventasTb)
            .ventasCredito(ventasCred)
            .montoEsperado(montoEsperado)
            .montoContado(montoContado)
            .montoRetirado(montoRetirado)
            .fondoCaja(fondoCaja)
            .difEfectivo(difEf)
            .difTarjeta(difTc)
            .difSinpe(difSin)
            .difTransferencia(difTb)
            .fechaFin(ahora)
            .build();
    }

    @Override
    public V2TurnoResponse unirseATurno(Long sesionId, Long usuarioId) {
        log.info("Usuario {} uniéndose a sesión v2 {}", usuarioId, sesionId);

        // 1. Validar sesión existe y está abierta
        V2SesionCaja sesion = sesionRepo.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (!sesion.isAbierta()) {
            throw new RuntimeException("La sesión ya está cerrada");
        }

        // 2. Validar que el usuario no tenga un turno activo en NINGUNA sesión
        turnoRepo.findActivoByUsuarioId(usuarioId).ifPresent(t -> {
            throw new RuntimeException(
                "Ya tenés un turno activo en la terminal: "
                    + t.getSesion().getTerminal().getNombre()
                    + ". Cerrá ese turno antes de unirte."
            );
        });

        Usuario usuario = usuarioRepo.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        //2.1 validar que no sea individual
        if ("INDIVIDUAL".equals(sesion.getModoGaveta())) {
            throw new RuntimeException(
                "Esta sesión es de gaveta INDIVIDUAL. Solo un cajero puede tener turno activo."
            );
        }

        // 3. Calcular fondoInicio desde facturas reales — nunca del campo guardado
        BigDecimal fondoInicio = calcularFondoActualGaveta(sesionId);

        log.info("fondoInicio calculado para usuario {} en sesión {}: {}",
            usuarioId, sesionId, fondoInicio);

        // 4. Crear turno
        V2TurnoCajero turno = V2TurnoCajero.builder()
            .sesion(sesion)
            .usuario(usuario)
            .fondoInicio(fondoInicio)
            .estado("ACTIVO")
            .build();

        turno = turnoRepo.save(turno);

        log.info("Turno v2 {} creado para usuario {} — fondoInicio: {}",
            turno.getId(), usuarioId, fondoInicio);

        return mapTurnoResponse(turno);
    }

    @Override
    public V2MovimientoResponse registrarMovimiento(
        Long turnoId, V2MovimientoRequest request, Long usuarioId) {

        log.info("Registrando movimiento v2 — turno: {} tipo: {} monto: {}",
            turnoId, request.getTipo(), request.getMonto());

        V2TurnoCajero turno = turnoRepo.findById(turnoId)
            .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (turno.isCerrado()) {
            throw new RuntimeException("No se pueden registrar movimientos en un turno cerrado");
        }

        Usuario usuario = usuarioRepo.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Usuario autorizadoPor = null;
        if (request.getAutorizadoPorId() != null) {
            autorizadoPor = usuarioRepo.findById(request.getAutorizadoPorId())
                .orElse(null);
        }

        V2MovimientoCaja movimiento = V2MovimientoCaja.builder()
            .sesion(turno.getSesion())
            .turno(turno)
            .usuario(usuario)
            .tipo(request.getTipo())
            .monto(request.getMonto())
            .concepto(request.getConcepto())
            .autorizadoPor(autorizadoPor)
            .build();

        movimiento = movimientoRepo.save(movimiento);

        log.info("Movimiento v2 {} registrado — tipo: {} monto: {}",
            movimiento.getId(), movimiento.getTipo(), movimiento.getMonto());

        return V2MovimientoResponse.builder()
            .id(movimiento.getId())
            .tipo(movimiento.getTipo())
            .monto(movimiento.getMonto())
            .concepto(movimiento.getConcepto())
            .cajeroNombre(usuario.getNombre() + " " + usuario.getApellidos())
            .fechaHora(movimiento.getFechaHora())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public V2EstadoSesionResponse obtenerEstado(Long sesionId, Long usuarioId) {
        V2SesionCaja sesion = sesionRepo.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        List<V2TurnoCajero> turnos = turnoRepo.findBySesionId(sesionId);

        // Totales de la sesión — sumando todos los turnos cerrados
        // + calculando en tiempo real los activos desde facturas
        BigDecimal totEf  = BigDecimal.ZERO;
        BigDecimal totTc  = BigDecimal.ZERO;
        BigDecimal totSin = BigDecimal.ZERO;
        BigDecimal totTb  = BigDecimal.ZERO;
        BigDecimal totCred = BigDecimal.ZERO;

        for (V2TurnoCajero t : turnos) {
            if (t.isCerrado()) {
                // Turno cerrado: usar campos persistidos (ya calculados al cerrar)
                totEf   = totEf.add(nvl(t.getVentasEfectivo()));
                totTc   = totTc.add(nvl(t.getVentasTarjeta()));
                totSin  = totSin.add(nvl(t.getVentasSinpe()));
                totTb   = totTb.add(nvl(t.getVentasTransferencia()));
                totCred = totCred.add(nvl(t.getVentasCredito()));
            } else {
                // Turno activo: calcular desde facturas en tiempo real
                totEf   = totEf.add(calcularVentasTurno(t.getId(), "EFECTIVO"));
                totTc   = totTc.add(calcularVentasTurno(t.getId(), "TARJETA"));
                totSin  = totSin.add(calcularVentasTurno(t.getId(), "SINPE"));
                totTb   = totTb.add(calcularVentasTurno(t.getId(), "TRANSFERENCIA"));
                totCred = totCred.add(calcularVentasTurno(t.getId(), "CREDITO"));
            }
        }

        // Movimientos de la sesión
        List<V2MovimientoResponse> movimientos = movimientoRepo
            .findBySesionIdOrderByFechaHoraDesc(sesionId)
            .stream()
            .map(m -> V2MovimientoResponse.builder()
                .id(m.getId())
                .tipo(m.getTipo())
                .monto(m.getMonto())
                .concepto(m.getConcepto())
                .cajeroNombre(m.getUsuario().getNombre() + " " + m.getUsuario().getApellidos())
                .fechaHora(m.getFechaHora())
                .build())
            .collect(Collectors.toList());

        // Armar turnos para el response — sin exponer montos de otros cajeros
        // La seguridad real se aplica en el controller según el rol
        final BigDecimal totEfFinal   = totEf;
        final BigDecimal totTcFinal   = totTc;
        final BigDecimal totSinFinal  = totSin;
        final BigDecimal totTbFinal   = totTb;
        final BigDecimal totCredFinal = totCred;

        List<V2EstadoSesionResponse.OtroTurnoDTO> otrosTurnos = turnos.stream()
            .map(t -> V2EstadoSesionResponse.OtroTurnoDTO.builder()
                .turnoId(t.getId())
                .cajeroNombre(t.getUsuario().getNombre() + " " + t.getUsuario().getApellidos())
                .estado(t.getEstado())
                .fechaInicio(t.getFechaInicio())
                .build())
            .collect(Collectors.toList());

        V2EstadoSesionResponse estado = V2EstadoSesionResponse.builder()
            .sesionId(sesion.getId())
            .terminal(sesion.getTerminal().getNombre())
            .modoGaveta(sesion.getModoGaveta())
            .estado(sesion.getEstado())
            .montoInicial(sesion.getMontoInicial())
            .fechaApertura(sesion.getFechaApertura())
            .otrosTurnos(otrosTurnos)
            .totalEfectivo(totEfFinal)
            .totalTarjeta(totTcFinal)
            .totalSinpe(totSinFinal)
            .totalTransferencia(totTbFinal)
            .totalCredito(totCredFinal)
            .movimientos(movimientos)
            .build();

        // Luego popular miTurno
        turnoRepo.findActivoByUsuarioIdAndSesionId(usuarioId, sesionId)
            .ifPresent(miT -> {
                BigDecimal mEf   = calcularVentasTurno(miT.getId(), "EFECTIVO");
                BigDecimal mTc   = calcularVentasTurno(miT.getId(), "TARJETA");
                BigDecimal mSin  = calcularVentasTurno(miT.getId(), "SINPE");
                BigDecimal mTb   = calcularVentasTurno(miT.getId(), "TRANSFERENCIA");
                BigDecimal mCred = calcularVentasTurno(miT.getId(), "CREDITO");
                BigDecimal ent   = nvl(movimientoRepo.sumEntradasEfectivoByTurnoId(miT.getId()));
                BigDecimal sal   = nvl(movimientoRepo.sumSalidasByTurnoId(miT.getId()));
                BigDecimal esp;
                if ("COMPARTIDA".equals(sesion.getModoGaveta())) {
                    esp = calcularFondoActualGaveta(sesionId);
                } else {
                    esp = nvl(miT.getFondoInicio()).add(mEf).add(ent).subtract(sal);
                }

                estado.setMiTurno(V2EstadoSesionResponse.MiTurnoDTO.builder()
                    .turnoId(miT.getId())
                    .fondoInicio(nvl(miT.getFondoInicio()))
                    .ventasEfectivo(mEf)
                    .ventasTarjeta(mTc)
                    .ventasSinpe(mSin)
                    .ventasTransferencia(mTb)
                    .ventasCredito(mCred)
                    .montoEsperado(esp)
                    .fechaInicio(miT.getFechaInicio())
                    .build());
            });

        return estado;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularFondoActualGaveta(Long sesionId) {
        V2SesionCaja sesion = sesionRepo.findById(sesionId)
            .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Base: monto inicial de la sesión
        BigDecimal fondo = nvl(sesion.getMontoInicial());

        // + efectivo cobrado en facturas electrónicas de la sesión
        BigDecimal efFacturas = nvl(facturaRepo.sumEfectivoByV2SesionId(sesionId));

        // + efectivo cobrado en facturas internas de la sesión
        BigDecimal efInternas = nvl(facturaInternaRepo.sumEfectivoByV2SesionId(sesionId));

        // + entradas manuales de efectivo (ENTRADA_EFECTIVO + ENTRADA_ABONO_CREDITO)
        BigDecimal entradas = nvl(movimientoRepo.sumEntradasEfectivoBySesionId(sesionId));

        // - todas las salidas (retiros, vales, pagos, depósitos)
        BigDecimal salidas = nvl(movimientoRepo.sumSalidasBySesionId(sesionId));

        fondo = fondo
            .add(efFacturas)
            .add(efInternas)
            .add(entradas)
            .subtract(salidas);

        log.debug("calcularFondoActualGaveta sesión {} → base:{} +efFact:{} +efInt:{} +ent:{} -sal:{} = {}",
            sesionId, sesion.getMontoInicial(), efFacturas, efInternas, entradas, salidas, fondo);

        return fondo;
    }

    @Override
    @Transactional(readOnly = true)
    public V2TurnoResponse obtenerMiTurnoActivo(Long usuarioId) {
        return turnoRepo.findActivoByUsuarioId(usuarioId)
            .map(this::mapTurnoResponse)
            .orElse(null);
    }

    @Override
    public V2EstadoSesionResponse obtenerEstadoPorTerminal(Long terminalId, Long usuarioId) {
        return sesionRepo.findAbiertaByTerminalId(terminalId)
            .map(sesion -> obtenerEstado(sesion.getId(), usuarioId))
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerUltimoFondoTerminal(Long terminalId) {
        return sesionRepo.findUltimaCerradaByTerminalId(terminalId)
            .map(sesion -> {
                BigDecimal fondo = turnoRepo.findBySesionId(sesion.getId())
                    .stream()
                    .filter(t -> t.isCerrado() && t.getFondoCaja() != null)
                    .max(java.util.Comparator.comparing(V2TurnoCajero::getFechaFin))
                    .map(V2TurnoCajero::getFondoCaja)
                    .orElse(BigDecimal.ZERO);

                return (Map<String, Object>) new java.util.LinkedHashMap<String, Object>() {{
                    put("fondoCaja", fondo);
                    put("sesionId", sesion.getId());
                }};
            })
            .orElse(Map.of("fondoCaja", BigDecimal.ZERO));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<V2BitacoraCajaSesionDTO> listarBitacora(V2BitacoraCajaFilterRequest filtros) {
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(filtros.getSortDir())
                ? Sort.Direction.DESC : Sort.Direction.ASC,
            filtros.getSortBy()
        );
        Pageable pageable = PageRequest.of(filtros.getPage(), filtros.getSize(), sort);

        Specification<V2SesionCaja> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (filtros.getSucursalId() != null)
                predicates.add(cb.equal(root.get("sucursal").get("id"), filtros.getSucursalId()));

            if (filtros.getTerminalId() != null)
                predicates.add(cb.equal(root.get("terminal").get("id"), filtros.getTerminalId()));

            if (filtros.getEstado() != null && !filtros.getEstado().isBlank())
                predicates.add(cb.equal(root.get("estado"), filtros.getEstado()));

            if (filtros.getModoGaveta() != null && !filtros.getModoGaveta().isBlank())
                predicates.add(cb.equal(root.get("modoGaveta"), filtros.getModoGaveta()));

            if (filtros.getFechaDesde() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaApertura"), filtros.getFechaDesde()));

            if (filtros.getFechaHasta() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaApertura"), filtros.getFechaHasta()));

            // Filtro por cajero — join con turnos
            if (filtros.getUsuarioId() != null) {
                var turnoJoin = root.join("turnos", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.equal(turnoJoin.get("usuario").get("id"), filtros.getUsuarioId()));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return sesionRepo.findAll(spec, pageable).map(this::mapearSesionDTO);
    }

    private V2BitacoraCajaSesionDTO mapearSesionDTO(V2SesionCaja sesion) {
        List<V2TurnoCajero> turnos = turnoRepo.findBySesionId(sesion.getId());

        BigDecimal totEf  = BigDecimal.ZERO;
        BigDecimal totTc  = BigDecimal.ZERO;
        BigDecimal totSin = BigDecimal.ZERO;
        BigDecimal totTb  = BigDecimal.ZERO;

        List<V2BitacoraCajaSesionDTO.TurnoResumenDTO> turnosDTO = turnos.stream()
            .map(t -> {
                BigDecimal tvEf  = t.isCerrado() ? nvl(t.getVentasEfectivo())      : calcularVentasTurno(t.getId(), "EFECTIVO");
                BigDecimal tvTc  = t.isCerrado() ? nvl(t.getVentasTarjeta())       : calcularVentasTurno(t.getId(), "TARJETA");
                BigDecimal tvSin = t.isCerrado() ? nvl(t.getVentasSinpe())         : calcularVentasTurno(t.getId(), "SINPE");
                BigDecimal tvTb  = t.isCerrado() ? nvl(t.getVentasTransferencia()) : calcularVentasTurno(t.getId(), "TRANSFERENCIA");
                BigDecimal tv    = tvEf.add(tvTc).add(tvSin).add(tvTb);

                return V2BitacoraCajaSesionDTO.TurnoResumenDTO.builder()
                    .turnoId(t.getId())
                    .cajero(t.getUsuario().getNombre() + " " + t.getUsuario().getApellidos())
                    .estado(t.getEstado())
                    .fechaInicio(t.getFechaInicio())
                    .fechaFin(t.getFechaFin())
                    .totalVentas(tv)
                    .fondoInicio(nvl(t.getFondoInicio()))
                    .fondoCaja(nvl(t.getFondoCaja()))
                    .build();
            })
            .collect(Collectors.toList());

        // Sumar totales desde los turnos
        for (V2BitacoraCajaSesionDTO.TurnoResumenDTO t : turnosDTO) {
            totEf  = totEf.add(nvl(null)); // se recalcula desde la sesión si está cerrada
        }

        // Usar totales persistidos si sesión cerrada, sino calcular
        BigDecimal totalEf  = sesion.getTotalEfectivo()      != null ? sesion.getTotalEfectivo()      : turnosDTO.stream().map(V2BitacoraCajaSesionDTO.TurnoResumenDTO::getTotalVentas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTc  = nvl(sesion.getTotalTarjeta());
        BigDecimal totalSin = nvl(sesion.getTotalSinpe());
        BigDecimal totalTb  = nvl(sesion.getTotalTransferencia());
        BigDecimal totalV   = totalEf.add(totalTc).add(totalSin).add(totalTb);

        return V2BitacoraCajaSesionDTO.builder()
            .sesionId(sesion.getId())
            .terminal(sesion.getTerminal().getNombre())
            .sucursal(sesion.getSucursal().getNombre())
            .usuarioApertura(sesion.getUsuarioApertura().getNombre()
                + " " + sesion.getUsuarioApertura().getApellidos())
            .modoGaveta(sesion.getModoGaveta())
            .estado(sesion.getEstado())
            .fechaApertura(sesion.getFechaApertura())
            .fechaCierre(sesion.getFechaCierre())
            .montoInicial(nvl(sesion.getMontoInicial()))
            .totalEfectivo(totalEf)
            .totalTarjeta(totalTc)
            .totalSinpe(totalSin)
            .totalTransferencia(totalTb)
            .totalVentas(totalV)
            .cantidadTurnos(turnos.size())
            .turnos(turnosDTO)
            .build();
    }

    public BigDecimal calcularVentasTurno(Long turnoId, String medioPago) {
        BigDecimal totalFacturas = BigDecimal.ZERO;
        BigDecimal totalInternas = BigDecimal.ZERO;

        // Facturas electrónicas — filtrar por v2_turno_id
        List<Factura> facturas = facturaRepo.findByV2TurnoId(turnoId);
        for (Factura f : facturas) {
            if (f.getMediosPago() == null) continue;
            for (var mp : f.getMediosPago()) {
                if (medioPago.equals(obtenerTipo(mp.getMedioPago().name()))) {
                    totalFacturas = totalFacturas.add(nvl(mp.getMonto()));
                }
            }
        }

        // Facturas internas — filtrar por v2_turno_id
        List<FacturaInterna> internas = facturaInternaRepo.findByV2TurnoId(turnoId);
        for (FacturaInterna fi : internas) {
            if (fi.getMediosPago() == null) continue;
            for (var mp : fi.getMediosPago()) {
                if (medioPago.equals(obtenerTipo(mp.getTipo()))) {
                    totalInternas = totalInternas.add(nvl(mp.getMonto()));
                }
            }
        }

        return totalFacturas.add(totalInternas);
    }

    private String obtenerTipo(String medioPago) {
        if (medioPago == null) return "";
        String m = medioPago.toUpperCase();
        if (m.contains("EFECTIVO"))         return "EFECTIVO";
        if (m.contains("TARJETA"))          return "TARJETA";
        if (m.contains("SINPE"))            return "SINPE";
        if (m.contains("TRANSFERENCIA"))    return "TRANSFERENCIA";
        if (m.contains("CREDITO"))          return "CREDITO";
        return m;
    }

    private void cerrarSesionInterna(V2SesionCaja sesion) {
        log.info("Cerrando sesión v2 {} automáticamente", sesion.getId());

        List<V2TurnoCajero> turnos = turnoRepo.findBySesionId(sesion.getId());

        BigDecimal totEf  = turnos.stream().map(t -> nvl(t.getVentasEfectivo())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totTc  = turnos.stream().map(t -> nvl(t.getVentasTarjeta())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totSin = turnos.stream().map(t -> nvl(t.getVentasSinpe())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totTb  = turnos.stream().map(t -> nvl(t.getVentasTransferencia())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totOtros = turnos.stream().map(t -> nvl(t.getVentasOtros())).reduce(BigDecimal.ZERO, BigDecimal::add);

        sesion.setEstado("CERRADA");
        sesion.setFechaCierre(ahora());
        sesion.setTotalEfectivo(totEf);
        sesion.setTotalTarjeta(totTc);
        sesion.setTotalSinpe(totSin);
        sesion.setTotalTransferencia(totTb);
        sesion.setTotalOtros(totOtros);
        sesionRepo.save(sesion);

        log.info("Sesión v2 {} cerrada — ef:{} tc:{} sinpe:{} tb:{}",
            sesion.getId(), totEf, totTc, totSin, totTb);
    }

// =========================================================
// MAPPER TURNO → RESPONSE
// =========================================================

    private V2TurnoResponse mapTurnoResponse(V2TurnoCajero turno) {
        return V2TurnoResponse.builder()
            .turnoId(turno.getId())
            .sesionId(turno.getSesion().getId())
            .usuarioId(turno.getUsuario().getId())
            .cajeroNombre(turno.getUsuario().getNombre()
                + " " + turno.getUsuario().getApellidos())
            .estado(turno.getEstado())
            .fondoInicio(turno.getFondoInicio())
            .fechaInicio(turno.getFechaInicio())
            .fechaFin(turno.getFechaFin())
            .terminal(turno.getSesion().getTerminal().getNombre())
            .modoGaveta(turno.getSesion().getModoGaveta())
            .build();
    }
}