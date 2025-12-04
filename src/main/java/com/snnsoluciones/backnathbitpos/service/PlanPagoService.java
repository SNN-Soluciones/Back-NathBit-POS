// src/main/java/com/snnsoluciones/backnathbitpos/service/PlanPagoService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.pagos.EstadoPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.HistorialPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.PlanPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.RegistrarPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.ResumenPagosEmpresaDTO;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.EstadoPlan;
import com.snnsoluciones.backnathbitpos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanPagoService {
    
    private final PlanPagoRepository planPagoRepository;
    private final HistorialPagoRepository historialPagoRepository;
    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Verificar estado de pago de una sucursal
     * Lógica:
     * - > 3 días antes: Sin alerta
     * - 1-3 días antes: Próximo a vencer
     * - Día 0: Día de pago
     * - Días negativos: En período de gracia (dinámico según plan.diasGracia)
     * - Gracia agotada: Suspendido
     */
    public EstadoPagoDTO verificarEstadoPago(Long sucursalId) {
        PlanPago plan = planPagoRepository.findBySucursalId(sucursalId).orElse(null);

        // Si no tiene plan, está activo (sucursales sin cobro)
        if (plan == null) {
            return EstadoPagoDTO.builder()
                .activo(true)
                .suspendido(false)
                .mostrarAlerta(false)
                .tipoAlerta(EstadoPagoDTO.TipoAlerta.SIN_ALERTA)
                .build();
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVencimiento = plan.getFechaProximoVencimiento();
        int diasParaVencimiento = (int) ChronoUnit.DAYS.between(hoy, fechaVencimiento);

        // Si ya está marcado como suspendido en BD
        if (plan.getEstado() == EstadoPlan.SUSPENDIDO) {
            return buildEstadoSuspendido(fechaVencimiento, diasParaVencimiento);
        }

        // Más de 3 días antes - Sin alerta
        if (diasParaVencimiento > 3) {
            return EstadoPagoDTO.builder()
                .activo(true)
                .suspendido(false)
                .mostrarAlerta(false)
                .fechaVencimiento(fechaVencimiento)
                .diasParaVencimiento(diasParaVencimiento)
                .tipoAlerta(EstadoPagoDTO.TipoAlerta.SIN_ALERTA)
                .build();
        }

        // 1-3 días antes - Próximo a vencer
        if (diasParaVencimiento >= 1 && diasParaVencimiento <= 3) {
            return buildProximoVencer(fechaVencimiento, diasParaVencimiento);
        }

        // Día exacto de pago
        if (diasParaVencimiento == 0) {
            return buildDiaPago(fechaVencimiento);
        }

        // Vencido - Calcular días de gracia restantes
        int diasMora = Math.abs(diasParaVencimiento);
        int diasGracia = plan.getDiasGracia();
        int diasGraciaRestantes = diasGracia - diasMora;

        // Todavía tiene días de gracia
        if (diasGraciaRestantes > 0) {
            return buildEnGracia(fechaVencimiento, diasParaVencimiento, diasGraciaRestantes);
        }

        // Último día de gracia
        if (diasGraciaRestantes == 0) {
            return buildUltimoDiaGracia(fechaVencimiento, diasParaVencimiento);
        }

        // Gracia agotada - Suspender
        plan.setEstado(EstadoPlan.SUSPENDIDO);
        planPagoRepository.save(plan);
        log.warn("Sucursal {} suspendida automáticamente - {} días de mora", sucursalId, diasMora);

        return buildEstadoSuspendido(fechaVencimiento, diasParaVencimiento);
    }
    
    /**
     * Registrar un pago
     */
    @Transactional
    public void registrarPago(RegistrarPagoDTO dto, Long usuarioId) {
        PlanPago plan = planPagoRepository.findBySucursalId(dto.getSucursalId())
            .orElseThrow(() -> new RuntimeException("No se encontró plan de pago para esta sucursal"));
        
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        
        // Calcular período
        LocalDate periodoInicio = plan.getFechaProximoVencimiento().withDayOfMonth(1);
        LocalDate periodoFin = periodoInicio.plusMonths(1).minusDays(1);
        
        // Crear historial
        HistorialPago pago = HistorialPago.builder()
            .planPago(plan)
            .sucursal(sucursal)
            .empresa(plan.getEmpresa())
            .fechaPago(dto.getFechaPago())
            .monto(dto.getMonto())
            .periodoInicio(periodoInicio)
            .periodoFin(periodoFin)
            .metodoPago(dto.getMetodoPago())
            .comprobante(dto.getComprobante())
            .notas(dto.getNotas())
            .usuario(usuario)
            .build();
        
        historialPagoRepository.save(pago);
        
        // Actualizar plan
        plan.setFechaUltimoPago(dto.getFechaPago());
        plan.setEstado(EstadoPlan.ACTIVO);
        
        // Calcular próximo vencimiento
        LocalDate proximoVencimiento = plan.getFechaProximoVencimiento()
            .plusMonths(1)
            .withDayOfMonth(plan.getDiaVencimiento());
        plan.setFechaProximoVencimiento(proximoVencimiento);
        
        planPagoRepository.save(plan);
        
        log.info("Pago registrado - Sucursal: {}, Monto: {}", dto.getSucursalId(), dto.getMonto());
    }
    
    /**
     * Crear plan de pago
     */
    @Transactional
    public PlanPagoDTO crearPlan(PlanPagoDTO dto) {
        if (planPagoRepository.existsBySucursalId(dto.getSucursalId())) {
            throw new RuntimeException("Ya existe un plan de pago para esta sucursal");
        }
        
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        
        // Calcular primer vencimiento
        LocalDate primerVencimiento = dto.getFechaInicio()
            .plusMonths(1)
            .withDayOfMonth(dto.getDiaVencimiento() != null ? dto.getDiaVencimiento() : 1);
        
        PlanPago plan = PlanPago.builder()
            .sucursal(sucursal)
            .empresa(empresa)
            .cuotaMensual(dto.getCuotaMensual())
            .fechaInicio(dto.getFechaInicio())
            .diaVencimiento(dto.getDiaVencimiento() != null ? dto.getDiaVencimiento() : 1)
            .diasGracia(dto.getDiasGracia() != null ? dto.getDiasGracia() : 3)
            .estado(EstadoPlan.ACTIVO)
            .fechaProximoVencimiento(primerVencimiento)
            .notas(dto.getNotas())
            .build();
        
        plan = planPagoRepository.save(plan);
        
        log.info("Plan creado - Sucursal: {}, Cuota: {}", dto.getSucursalId(), dto.getCuotaMensual());
        
        return convertirADTO(plan);
    }
    
    /**
     * Actualizar plan
     */
    @Transactional
    public PlanPagoDTO actualizarPlan(Long id, PlanPagoDTO dto) {
        PlanPago plan = planPagoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        
        plan.setCuotaMensual(dto.getCuotaMensual());
        plan.setDiaVencimiento(dto.getDiaVencimiento());
        plan.setDiasGracia(dto.getDiasGracia());
        plan.setNotas(dto.getNotas());
        
        plan = planPagoRepository.save(plan);
        
        return convertirADTO(plan);
    }
    
    /**
     * Obtener plan de una sucursal
     */
    public PlanPagoDTO obtenerPlanSucursal(Long sucursalId) {
        PlanPago plan = planPagoRepository.findBySucursalId(sucursalId)
            .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        return convertirADTO(plan);
    }
    
    /**
     * Obtener planes por empresa
     */
    public List<PlanPagoDTO> obtenerPlanesPorEmpresa(Long empresaId) {
        return planPagoRepository.findByEmpresaId(empresaId)
            .stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Listar todos los planes
     */
    public List<PlanPagoDTO> listarTodos() {
        return planPagoRepository.findAll()
            .stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener historial de pagos de una sucursal
     */
    public List<HistorialPagoDTO> obtenerHistorial(Long sucursalId) {
        return historialPagoRepository.findBySucursalIdOrderByFechaPagoDesc(sucursalId)
            .stream()
            .map(this::convertirHistorialADTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener resumen de empresa
     */
    public ResumenPagosEmpresaDTO obtenerResumenEmpresa(Long empresaId) {
        Long total = planPagoRepository.countByEmpresaId(empresaId);
        Long activas = planPagoRepository.countSucursalesActivasByEmpresa(empresaId);
        Long suspendidas = planPagoRepository.countSucursalesSuspendidasByEmpresa(empresaId);
        BigDecimal montoTotal = planPagoRepository.sumCuotasMensualesActivasByEmpresa(empresaId);
        
        return ResumenPagosEmpresaDTO.builder()
            .totalSucursales(total)
            .sucursalesActivas(activas)
            .sucursalesSuspendidas(suspendidas)
            .montoMensualTotal(montoTotal)
            .build();
    }
    
    /**
     * Suspender plan
     */
    @Transactional
    public void suspenderPlan(Long sucursalId) {
        PlanPago plan = planPagoRepository.findBySucursalId(sucursalId)
            .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        
        plan.setEstado(EstadoPlan.SUSPENDIDO);
        planPagoRepository.save(plan);
        
        log.warn("Plan suspendido - Sucursal: {}", sucursalId);
    }
    
    /**
     * Reactivar plan
     */
    @Transactional
    public void reactivarPlan(Long sucursalId) {
        PlanPago plan = planPagoRepository.findBySucursalId(sucursalId)
            .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        
        plan.setEstado(EstadoPlan.ACTIVO);
        planPagoRepository.save(plan);
        
        log.info("Plan reactivado - Sucursal: {}", sucursalId);
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    private PlanPagoDTO convertirADTO(PlanPago plan) {
        return PlanPagoDTO.builder()
            .id(plan.getId())
            .sucursalId(plan.getSucursal().getId())
            .nombreSucursal(plan.getSucursal().getNombre())
            .empresaId(plan.getEmpresa().getId())
            .nombreEmpresa(plan.getEmpresa().getNombreRazonSocial())
            .cuotaMensual(plan.getCuotaMensual())
            .fechaInicio(plan.getFechaInicio())
            .diaVencimiento(plan.getDiaVencimiento())
            .estado(plan.getEstado().name())
            .diasGracia(plan.getDiasGracia())
            .fechaUltimoPago(plan.getFechaUltimoPago())
            .fechaProximoVencimiento(plan.getFechaProximoVencimiento())
            .notas(plan.getNotas())
            .build();
    }
    
    private HistorialPagoDTO convertirHistorialADTO(HistorialPago pago) {
        return HistorialPagoDTO.builder()
            .id(pago.getId())
            .sucursalId(pago.getSucursal().getId())
            .nombreSucursal(pago.getSucursal().getNombre())
            .fechaPago(pago.getFechaPago())
            .monto(pago.getMonto())
            .periodoInicio(pago.getPeriodoInicio())
            .periodoFin(pago.getPeriodoFin())
            .metodoPago(pago.getMetodoPago())
            .comprobante(pago.getComprobante())
            .notas(pago.getNotas())
            .registradoPor(pago.getUsuario() != null ? pago.getUsuario().getNombre() : null)
            .build();
    }
    
    private String formatearFecha(LocalDate fecha) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", 
            new Locale("es", "CR"));
        return fecha.format(formatter);
    }

    private EstadoPagoDTO buildProximoVencer(LocalDate fechaVencimiento, int diasRestantes) {
        String mensaje = diasRestantes == 1
            ? "📅 Falta 1 día para el vencimiento de la cuota.\nFecha: " + formatearFecha(fechaVencimiento)
            : "📅 Faltan " + diasRestantes + " días para el vencimiento de la cuota.\nFecha: " + formatearFecha(fechaVencimiento);

        return EstadoPagoDTO.builder()
            .activo(true)
            .suspendido(false)
            .mostrarAlerta(true)
            .fechaVencimiento(fechaVencimiento)
            .diasParaVencimiento(diasRestantes)
            .tipoAlerta(EstadoPagoDTO.TipoAlerta.PROXIMO_VENCER)
            .mensaje(mensaje)
            .build();
    }

    private EstadoPagoDTO buildDiaPago(LocalDate fechaVencimiento) {
        return EstadoPagoDTO.builder()
            .activo(true)
            .suspendido(false)
            .mostrarAlerta(true)
            .fechaVencimiento(fechaVencimiento)
            .diasParaVencimiento(0)
            .tipoAlerta(EstadoPagoDTO.TipoAlerta.DIA_PAGO)
            .mensaje("🔔 Hoy es día de pago.\nPor favor realice el pago para evitar inconvenientes.")
            .build();
    }

    private EstadoPagoDTO buildEnGracia(LocalDate fechaVencimiento, int diasParaVencimiento, int diasGraciaRestantes) {
        String mensaje = diasGraciaRestantes == 1
            ? "⚠️ Pago vencido.\nLe queda 1 día de gracia."
            : "⚠️ Pago vencido.\nLe quedan " + diasGraciaRestantes + " días de gracia.";

        return EstadoPagoDTO.builder()
            .activo(true)
            .suspendido(false)
            .mostrarAlerta(true)
            .fechaVencimiento(fechaVencimiento)
            .diasParaVencimiento(diasParaVencimiento)
            .diasGraciaRestantes(diasGraciaRestantes)
            .tipoAlerta(EstadoPagoDTO.TipoAlerta.EN_GRACIA)
            .mensaje(mensaje)
            .build();
    }

    private EstadoPagoDTO buildUltimoDiaGracia(LocalDate fechaVencimiento, int diasParaVencimiento) {
        return EstadoPagoDTO.builder()
            .activo(true)
            .suspendido(false)
            .mostrarAlerta(true)
            .fechaVencimiento(fechaVencimiento)
            .diasParaVencimiento(diasParaVencimiento)
            .diasGraciaRestantes(0)
            .tipoAlerta(EstadoPagoDTO.TipoAlerta.ULTIMO_DIA_GRACIA)
            .mensaje("🔴 ÚLTIMO DÍA DE GRACIA\nEl servicio será suspendido mañana si no realiza el pago.")
            .build();
    }

    private EstadoPagoDTO buildEstadoSuspendido(LocalDate fechaVencimiento, int diasParaVencimiento) {
        return EstadoPagoDTO.builder()
            .activo(false)
            .suspendido(true)
            .mostrarAlerta(true)
            .fechaVencimiento(fechaVencimiento)
            .diasParaVencimiento(diasParaVencimiento)
            .tipoAlerta(EstadoPagoDTO.TipoAlerta.SUSPENDIDO)
            .mensaje("⛔ SERVICIO SUSPENDIDO\n\nEl servicio ha sido suspendido por falta de pago.\nContacte con administración para reactivar.")
            .build();
    }
}