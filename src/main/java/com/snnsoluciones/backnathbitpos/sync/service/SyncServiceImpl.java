package com.snnsoluciones.backnathbitpos.sync.service;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.sync.dto.*;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPullResponse.*;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPushRequest.*;
import com.snnsoluciones.backnathbitpos.sync.dto.SyncPushResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    // ===== REPOSITORIES =====
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final TerminalRepository terminalRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProvinciaRepository provinciaRepository;
    private final CantonRepository cantonRepository;
    private final DistritoRepository distritoRepository;
    private final BarrioRepository barrioRepository;
    private final ActividadEconomicaRepository actividadEconomicaRepository;
    private final CategoriaProductoRepository categoriaRepository;
    private final FamiliaProductoRepository familiaProductoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final FacturaInternaRepository facturaInternaRepository;
    private final SesionCajaRepository sesionCajaRepository;

    // =========================================================================
    // PULL - Descarga cambios desde el servidor
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public SyncPullResponse pull(SyncPullRequest request) {
        log.info("📥 PULL - Terminal: {}, Sucursal: {}, LastSync: {}", 
            request.getTerminalId(), request.getSucursalId(), request.getLastSync());
        
        LocalDateTime lastSync = request.getLastSync();
        Long sucursalId = request.getSucursalId();
        Long terminalId = request.getTerminalId();
        
        // Obtener sucursal para filtrar por empresa
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + sucursalId));
        Long empresaId = sucursal.getEmpresa().getId();
        
        SyncPullResponse response = SyncPullResponse.builder()
            .syncTimestamp(LocalDateTime.now())
            .build();
        
        int totalRegistros = 0;
        
        // ===== CONFIG =====
        response.setEmpresa(mapEmpresa(sucursal.getEmpresa()));
        response.setSucursal(mapSucursal(sucursal));
        
        Terminal terminal = terminalRepository.findById(terminalId)
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada: " + terminalId));
        response.setTerminal(mapTerminal(terminal));
        
        // Usuarios (filtrados por updated_at si hay lastSync)
        List<Usuario> usuarios = lastSync != null
            ? usuarioRepository.findByUsuarioEmpresas_Empresa_IdAndUpdatedAtAfter(empresaId, lastSync)
            : usuarioRepository.findByUsuarioEmpresas_Empresa_Id(empresaId);
        response.setUsuarios(usuarios.stream().map(this::mapUsuario).collect(Collectors.toList()));
        totalRegistros += usuarios.size();
        
        // ===== CATÁLOGOS (solo en full o si no hay lastSync) =====
        if (lastSync == null) {
            response.setProvincias(provinciaRepository.findAll().stream()
                .map(this::mapProvincia).collect(Collectors.toList()));
            response.setCantones(cantonRepository.findAll().stream()
                .map(this::mapCanton).collect(Collectors.toList()));
            response.setDistritos(distritoRepository.findAll().stream()
                .map(this::mapDistrito).collect(Collectors.toList()));
            response.setBarrios(barrioRepository.findAll().stream()
                .map(this::mapBarrio).collect(Collectors.toList()));
            response.setActividadesEconomicas(actividadEconomicaRepository.findAll().stream()
                .map(this::mapActividadEconomica).collect(Collectors.toList()));
        }

        // ===== PRODUCTOS =====
        List<CategoriaProducto> categorias = lastSync != null
            ? categoriaRepository.findBySucursalIdAndUpdatedAtAfter(sucursalId, lastSync)
            : categoriaRepository.findBySucursalId(sucursalId);
        response.setCategorias(categorias.stream().map(this::mapCategoria).collect(Collectors.toList()));
        totalRegistros += categorias.size();
        
        List<FamiliaProducto> familias = lastSync != null
            ? familiaProductoRepository.findBySucursalIdAndUpdatedAtAfter(sucursalId, lastSync)
            : familiaProductoRepository.findBySucursalId(sucursalId);
        response.setFamilias(familias.stream().map(this::mapFamilia).collect(Collectors.toList()));
        totalRegistros += familias.size();
        
        List<Producto> productos = lastSync != null
            ? productoRepository.findBySucursalIdAndUpdatedAtAfter(sucursalId, lastSync)
            : productoRepository.findBySucursalId(sucursalId);
        response.setProductos(productos.stream().map(this::mapProducto).collect(Collectors.toList()));
        totalRegistros += productos.size();
        
        // ===== CLIENTES =====
        List<Cliente> clientes = lastSync != null
            ? clienteRepository.findByEmpresaIdAndUpdatedAtAfter(empresaId, lastSync)
            : clienteRepository.findByEmpresaId(empresaId);
        response.setClientes(clientes.stream().map(this::mapCliente).collect(Collectors.toList()));
        totalRegistros += clientes.size();
        
        response.setTotalRegistros(totalRegistros);
        
        log.info("📥 PULL completado - {} registros", totalRegistros);
        return response;
    }

    // =========================================================================
    // FULL - Descarga ALL (primera sincronización)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public SyncPullResponse full(Long terminalId, Long sucursalId) {
        log.info("📦 FULL SYNC - Terminal: {}, Sucursal: {}", terminalId, sucursalId);
        
        SyncPullRequest request = SyncPullRequest.builder()
            .terminalId(terminalId)
            .sucursalId(sucursalId)
            .lastSync(null)  // NULL = traer all
            .build();
        
        return pull(request);
    }

    // =========================================================================
    // PUSH - Sube cambios del POS al servidor
    // =========================================================================
    @Override
    @Transactional
    public SyncPushResponse push(SyncPushRequest request) {
        log.info("📤 PUSH - Terminal: {}, Sucursal: {}", 
            request.getTerminalId(), request.getSucursalId());
        
        List<IdMapping> clientesMapeados = new ArrayList<>();
        List<IdMapping> facturasMapeadas = new ArrayList<>();
        List<IdMapping> sesionesMapeadas = new ArrayList<>();
        List<SyncError> errores = new ArrayList<>();
        
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        Empresa empresa = sucursal.getEmpresa();
        
        // ===== PROCESAR CLIENTES =====
        if (request.getClientes() != null) {
            for (ClientePush clientePush : request.getClientes()) {
                try {
                    Cliente cliente = processClientePush(clientePush, empresa, sucursal);
                    clientesMapeados.add(IdMapping.builder()
                        .uuid(clientePush.getUuid())
                        .serverId(cliente.getId())
                        .build());
                } catch (Exception e) {
                    log.error("Error procesando cliente {}: {}", clientePush.getUuid(), e.getMessage());
                    errores.add(SyncError.builder()
                        .uuid(clientePush.getUuid())
                        .tabla("clientes")
                        .error(e.getMessage())
                        .build());
                }
            }
        }
        
        // ===== PROCESAR SESIONES DE CAJA =====
        if (request.getSesionesCaja() != null) {
            for (SesionCajaPush sesionPush : request.getSesionesCaja()) {
                try {
                    SesionCaja sesion = processSesionCajaPush(sesionPush);
                    sesionesMapeadas.add(IdMapping.builder()
                        .uuid(sesionPush.getUuid())
                        .serverId(sesion.getId())
                        .build());
                } catch (Exception e) {
                    log.error("Error procesando sesión {}: {}", sesionPush.getUuid(), e.getMessage());
                    errores.add(SyncError.builder()
                        .uuid(sesionPush.getUuid())
                        .tabla("sesiones_caja")
                        .error(e.getMessage())
                        .build());
                }
            }
        }
        
        // ===== PROCESAR FACTURAS INTERNAS =====
        if (request.getFacturasInternas() != null) {
            for (FacturaInternaPush facturaPush : request.getFacturasInternas()) {
                try {
                    FacturaInterna factura = processFacturaInternaPush(
                        facturaPush, sucursal, clientesMapeados, sesionesMapeadas);
                    facturasMapeadas.add(IdMapping.builder()
                        .uuid(facturaPush.getUuid())
                        .serverId(factura.getId())
                        .build());
                } catch (Exception e) {
                    log.error("Error procesando factura {}: {}", facturaPush.getUuid(), e.getMessage());
                    errores.add(SyncError.builder()
                        .uuid(facturaPush.getUuid())
                        .tabla("facturas_internas")
                        .error(e.getMessage())
                        .build());
                }
            }
        }
        
        // ===== ACTUALIZAR CONSECUTIVOS =====
        if (request.getConsecutivos() != null) {
            updateConsecutivos(request.getConsecutivos());
        }
        
        log.info("📤 PUSH completado - Clientes: {}, Facturas: {}, Sesiones: {}, Errores: {}",
            clientesMapeados.size(), facturasMapeadas.size(), sesionesMapeadas.size(), errores.size());
        
        return SyncPushResponse.builder()
            .success(errores.isEmpty())
            .syncTimestamp(LocalDateTime.now())
            .mensaje(errores.isEmpty() ? "Sincronización exitosa" : "Sincronización con errores")
            .clientesMapeados(clientesMapeados)
            .facturasMapeadas(facturasMapeadas)
            .sesionesMapeadas(sesionesMapeadas)
            .errores(errores)
            .build();
    }

    // =========================================================================
    // METHODS PRIVADOS - Procesar Push
    // =========================================================================
    
    private Cliente processClientePush(ClientePush push, Empresa empresa, Sucursal sucursal) {
        Cliente cliente;
        
        if (push.getServerId() != null) {
            // Actualizar existente
            cliente = clienteRepository.findById(push.getServerId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + push.getServerId()));
        } else {
            // Crear nuevo
            cliente = new Cliente();
            cliente.setEmpresa(empresa);
            cliente.setSucursal(sucursal);
        }
        
        cliente.setTipoIdentificacion(
            com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion.valueOf(push.getTipoIdentificacion()));
        cliente.setNumeroIdentificacion(push.getNumeroIdentificacion());
        cliente.setRazonSocial(push.getRazonSocial());
        cliente.setTelefonoCodigoPais(push.getTelefonoCodigoPais());
        cliente.setTelefonoNumero(push.getTelefonoNumero());
        cliente.setInscritoHacienda(push.getInscritoHacienda());
        cliente.setPermiteCredito(push.getPermiteCredito());
        cliente.setLimiteCredito(push.getLimiteCredito());
        cliente.setActivo(push.getActivo());
        
        // Procesar emails
        if (push.getEmails() != null) {
            cliente.getClienteEmails().clear();
            for (ClienteEmailPush emailPush : push.getEmails()) {
                ClienteEmail email = ClienteEmail.builder()
                    .cliente(cliente)
                    .email(emailPush.getEmail())
                    .esPrincipal(emailPush.getEsPrincipal())
                    .build();
                cliente.getClienteEmails().add(email);
            }
        }
        
        return clienteRepository.save(cliente);
    }
    
    private SesionCaja processSesionCajaPush(SesionCajaPush push) {
        SesionCaja sesion;
        
        if (push.getServerId() != null) {
            sesion = sesionCajaRepository.findById(push.getServerId())
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada: " + push.getServerId()));
        } else {
            sesion = new SesionCaja();
            Terminal terminal = terminalRepository.findById(push.getTerminalId())
                .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
            Usuario usuario = usuarioRepository.findById(push.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            sesion.setTerminal(terminal);
            sesion.setUsuario(usuario);
        }
        
        sesion.setFechaHoraApertura(push.getFechaHoraApertura());
        sesion.setFechaHoraCierre(push.getFechaHoraCierre());
        sesion.setMontoInicial(push.getMontoInicial());
        sesion.setMontoCierre(push.getMontoCierre());
        sesion.setMontoRetirado(push.getMontoRetirado());
        sesion.setFondoCaja(push.getFondoCaja());
        sesion.setTotalVentas(push.getTotalVentas());
        sesion.setTotalDevoluciones(push.getTotalDevoluciones());
        sesion.setTotalEfectivo(push.getTotalEfectivo());
        sesion.setTotalTarjeta(push.getTotalTarjeta());
        sesion.setTotalTransferencia(push.getTotalTransferencia());
        sesion.setTotalOtros(push.getTotalOtros());
        sesion.setCantidadFacturas(push.getCantidadFacturas());
        sesion.setCantidadTiquetes(push.getCantidadTiquetes());
        sesion.setCantidadNotasCredito(push.getCantidadNotasCredito());
        sesion.setEstado(com.snnsoluciones.backnathbitpos.enums.EstadoSesion.valueOf(push.getEstado()));
        sesion.setObservacionesApertura(push.getObservacionesApertura());
        sesion.setObservacionesCierre(push.getObservacionesCierre());
        
        return sesionCajaRepository.save(sesion);
    }
    
    private FacturaInterna processFacturaInternaPush(
            FacturaInternaPush push, 
            Sucursal sucursal,
            List<IdMapping> clientesMapeados,
            List<IdMapping> sesionesMapeadas) {
        
        FacturaInterna factura;
        
        if (push.getServerId() != null) {
            factura = facturaInternaRepository.findById(push.getServerId())
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + push.getServerId()));
        } else {
            factura = new FacturaInterna();
            factura.setEmpresa(sucursal.getEmpresa());
            factura.setSucursal(sucursal);
        }
        
        factura.setNumero(push.getNumero());
        factura.setFecha(push.getFecha());
        factura.setNombreCliente(push.getNombreCliente());
        
        // Resolver cliente
        Long clienteServerId = resolveServerId(push.getClienteUuid(), push.getClienteServerId(), clientesMapeados);
        if (clienteServerId != null) {
            factura.setCliente(clienteRepository.findById(clienteServerId).orElse(null));
        }
        
        // Resolver sesión de caja
        Long sesionServerId = resolveServerId(push.getSesionCajaUuid(), push.getSesionCajaServerId(), sesionesMapeadas);
        if (sesionServerId != null) {
            factura.setSesionCaja(sesionCajaRepository.findById(sesionServerId).orElse(null));
        }
        
        // Cajero y mesero
        if (push.getCajeroId() != null) {
            factura.setCajero(usuarioRepository.findById(push.getCajeroId()).orElse(null));
        }
        if (push.getMeseroId() != null) {
            factura.setMesero(usuarioRepository.findById(push.getMeseroId()).orElse(null));
        }
        
        // Montos
        factura.setSubtotal(push.getSubtotal());
        factura.setDescuentoPorcentaje(push.getDescuentoPorcentaje());
        factura.setDescuento(push.getDescuento());
        factura.setPorcentajeServicio(push.getPorcentajeServicio());
        factura.setImpuestoServicio(push.getImpuestoServicio());
        factura.setTotal(push.getTotal());
        factura.setPagoRecibido(push.getPagoRecibido());
        factura.setVuelto(push.getVuelto());
        
        // Estado
        factura.setEstado(push.getEstado());
        factura.setAnuladaPorId(push.getAnuladaPorId());
        factura.setFechaAnulacion(push.getFechaAnulacion());
        factura.setMotivoAnulacion(push.getMotivoAnulacion());
        
        factura.setNumeroViper(push.getNumeroViper());
        factura.setNotas(push.getNotas());
        
        // Detalles
        if (push.getDetalles() != null) {
            factura.getDetalles().clear();
            for (FacturaInternaDetallePush detPush : push.getDetalles()) {
                FacturaInternaDetalle detalle = FacturaInternaDetalle.builder()
                    .facturaInterna(factura)
                    .producto(productoRepository.findById(detPush.getProductoId()).orElse(null))
                    .codigoProducto(detPush.getCodigoProducto())
                    .nombreProducto(detPush.getNombreProducto())
                    .cantidad(detPush.getCantidad())
                    .precioUnitario(detPush.getPrecioUnitario())
                    .subtotal(detPush.getSubtotal())
                    .descuento(detPush.getDescuento())
                    .total(detPush.getTotal())
                    .notas(detPush.getNotas())
                    .build();
                factura.getDetalles().add(detalle);
            }
        }
        
        // Medios de pago
        if (push.getMediosPago() != null) {
            factura.getMediosPago().clear();
            for (FacturaInternaMedioPagoPush mpPush : push.getMediosPago()) {
                FacturaInternaMedioPago medioPago = FacturaInternaMedioPago.builder()
                    .facturaInterna(factura)
                    .tipo(mpPush.getTipo())
                    .monto(mpPush.getMonto())
                    .referencia(mpPush.getReferencia())
                    .banco(mpPush.getBanco())
                    .notas(mpPush.getNotas())
                    .build();
                factura.getMediosPago().add(medioPago);
            }
        }
        
        return facturaInternaRepository.save(factura);
    }
    
    private Long resolveServerId(String uuid, Long serverId, List<IdMapping> mappings) {
        if (serverId != null) {
            return serverId;
        }
        if (uuid != null) {
            return mappings.stream()
                .filter(m -> uuid.equals(m.getUuid()))
                .map(IdMapping::getServerId)
                .findFirst()
                .orElse(null);
        }
        return null;
    }
    
    private void updateConsecutivos(TerminalConsecutivosPush consecutivos) {
        Terminal terminal = terminalRepository.findById(consecutivos.getTerminalId())
            .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
        
        // Solo actualizar si el valor del POS es mayor (para evitar retrocesos)
        if (consecutivos.getConsecutivoFacturaElectronica() != null &&
            consecutivos.getConsecutivoFacturaElectronica() > terminal.getConsecutivoFacturaElectronica()) {
            terminal.setConsecutivoFacturaElectronica(consecutivos.getConsecutivoFacturaElectronica());
        }
        if (consecutivos.getConsecutivoTiqueteElectronico() != null &&
            consecutivos.getConsecutivoTiqueteElectronico() > terminal.getConsecutivoTiqueteElectronico()) {
            terminal.setConsecutivoTiqueteElectronico(consecutivos.getConsecutivoTiqueteElectronico());
        }
        if (consecutivos.getConsecutivoTiqueteInterno() != null &&
            consecutivos.getConsecutivoTiqueteInterno() > terminal.getConsecutivoTiqueteInterno()) {
            terminal.setConsecutivoTiqueteInterno(consecutivos.getConsecutivoTiqueteInterno());
        }
        if (consecutivos.getConsecutivoFacturaInterna() != null &&
            consecutivos.getConsecutivoFacturaInterna() > terminal.getConsecutivoFacturaInterna()) {
            terminal.setConsecutivoFacturaInterna(consecutivos.getConsecutivoFacturaInterna());
        }
        // ... otros consecutivos
        
        terminalRepository.save(terminal);
    }

    // =========================================================================
    // METHODS PRIVADOS - Mappers (Entity → DTO)
    // =========================================================================
    
    private EmpresaSync mapEmpresa(Empresa e) {
        if (e == null) return null;
        
        List<ActividadEmpresaSync> actividades = new ArrayList<>();
        if (e.getActividades() != null) {
            actividades = e.getActividades().stream()
                .map(a -> ActividadEmpresaSync.builder()
                    .id(a.getId())
                    .codigoActividad(a.getActividad().getCodigo())
                    .descripcion(a.getActividad().getDescripcion())
                    .esPrincipal(a.getEsPrincipal())
                    .build())
                .collect(Collectors.toList());
        }
        
        return EmpresaSync.builder()
            .id(e.getId())
            .tipoIdentificacion(e.getTipoIdentificacion() != null ? e.getTipoIdentificacion().name() : null)
            .identificacion(e.getIdentificacion())
            .nombreComercial(e.getNombreComercial())
            .nombreRazonSocial(e.getNombreRazonSocial())
            .telefono(e.getTelefono())
            .email(e.getEmail())
            .provinciaId(e.getProvincia() != null ? e.getProvincia().getId().longValue() : null)
            .cantonId(e.getCanton() != null ? e.getCanton().getId().longValue() : null)
            .distritoId(e.getDistrito() != null ? e.getDistrito().getId().longValue() : null)
            .barrioId(e.getBarrio() != null ? e.getBarrio().getId().longValue() : null)
            .otrasSenas(e.getOtrasSenas())
            .requiereHacienda(e.getRequiereHacienda())
            .regimenTributario(e.getRegimenTributario() != null ? e.getRegimenTributario().name() : null)
            .logoUrl(e.getLogoUrl())
            .updatedAt(e.getUpdatedAt())
            .actividades(actividades)
            .build();
    }
    
    private SucursalSync mapSucursal(Sucursal s) {
        if (s == null) return null;
        return SucursalSync.builder()
            .id(s.getId())
            .nombre(s.getNombre())
            .numeroSucursal(s.getNumeroSucursal())
            .modoFacturacion(s.getModoFacturacion() != null ? s.getModoFacturacion().name() : null)
            .provinciaId(s.getProvincia() != null ? s.getProvincia().getId().longValue() : null)
            .cantonId(s.getCanton() != null ? s.getCanton().getId().longValue() : null)
            .distritoId(s.getDistrito() != null ? s.getDistrito().getId().longValue() : null)
            .barrioId(s.getBarrio() != null ? s.getBarrio().getId().longValue() : null)
            .otrasSenas(s.getOtrasSenas())
            .manejaInventario(s.getManejaInventario())
            .aplicaRecetas(s.getAplicaRecetas())
            .permiteNegativos(s.getPermiteNegativos())
            .updatedAt(s.getUpdatedAt())
            .build();
    }
    
    private TerminalSync mapTerminal(Terminal t) {
        if (t == null) return null;
        return TerminalSync.builder()
            .id(t.getId())
            .numeroTerminal(t.getNumeroTerminal())
            .nombre(t.getNombre())
            .descripcion(t.getDescripcion())
            .consecutivoFacturaElectronica(t.getConsecutivoFacturaElectronica())
            .consecutivoTiqueteElectronico(t.getConsecutivoTiqueteElectronico())
            .consecutivoNotaCredito(t.getConsecutivoNotaCredito())
            .consecutivoNotaDebito(t.getConsecutivoNotaDebito())
            .consecutivoTiqueteInterno(t.getConsecutivoTiqueteInterno())
            .consecutivoFacturaInterna(t.getConsecutivoFacturaInterna())
            .consecutivoProforma(t.getConsecutivoProforma())
            .tipoImpresion(t.getTipoImpresion() != null ? t.getTipoImpresion().name() : null)
            .imprimirAutomatico(t.getImprimirAutomatico())
            .updatedAt(t.getUpdatedAt())
            .build();
    }
    
    private UsuarioSync mapUsuario(Usuario u) {
        if (u == null) return null;
        return UsuarioSync.builder()
            .id(u.getId())
            .nombre(u.getNombre())
            .apellidos(u.getApellidos())
            .email(u.getEmail())
            .username(u.getUsername())
            .pin(u.getPin())
            .pinLongitud(u.getPinLongitud())
            .rol(u.getRol() != null ? u.getRol().name() : null)
            .activo(u.getActivo())
            .updatedAt(u.getUpdatedAt())
            .build();
    }
    
    private ProvinciaSync mapProvincia(Provincia p) {
        return ProvinciaSync.builder()
            .id(p.getId().longValue())
            .codigo(String.valueOf(p.getCodigo()))
            .nombre(p.getProvincia())
            .build();
    }
    
    private CantonSync mapCanton(Canton c) {
        return CantonSync.builder()
            .id(c.getId().longValue())
            .provinciaId(Long.valueOf(c.getCodigoProvincia()))
            .codigo(String.valueOf(c.getCodigo()))
            .nombre(c.getCanton())
            .build();
    }
    
    private DistritoSync mapDistrito(Distrito d) {
        return DistritoSync.builder()
            .id(d.getId().longValue())
            .cantonId(Long.valueOf(d.getCodigoCanton()))
            .codigo(String.valueOf(d.getCodigo()))
            .nombre(d.getDistrito())
            .build();
    }
    
    private BarrioSync mapBarrio(Barrio b) {
        return BarrioSync.builder()
            .id(b.getId().longValue())
            .distritoId(Long.valueOf(b.getCodigoDistrito()))
            .codigo(String.valueOf(b.getCodigo()))
            .nombre(b.getBarrio())
            .build();
    }
    
    private ActividadEconomicaSync mapActividadEconomica(ActividadEconomica a) {
        return ActividadEconomicaSync.builder()
            .id(a.getId())
            .codigo(a.getCodigo())
            .descripcion(a.getDescripcion())
            .build();
    }
    
    private CategoriaSync mapCategoria(CategoriaProducto c) {
        return CategoriaSync.builder()
            .id(c.getId())
            .nombre(c.getNombre())
            .descripcion(c.getDescripcion())
            .color(c.getColor())
            .icono(c.getIcono())
            .orden(c.getOrden())
            .activo(c.getActivo())
            .updatedAt(c.getUpdatedAt())
            .build();
    }
    
    private FamiliaProductoSync mapFamilia(FamiliaProducto f) {
        return FamiliaProductoSync.builder()
            .id(f.getId())
            .nombre(f.getNombre())
            .descripcion(f.getDescripcion())
            .codigo(f.getCodigo())
            .color(f.getColor())
            .icono(f.getIcono())
            .activa(f.getActiva())
            .orden(f.getOrden())
            .updatedAt(f.getUpdatedAt())
            .build();
    }
    
    private ProductoSync mapProducto(Producto p) {
        ProductoSync.ProductoSyncBuilder builder = ProductoSync.builder()
            .id(p.getId())
            .familiaId(p.getFamilia() != null ? p.getFamilia().getId() : null)
            .codigoInterno(p.getCodigoInterno())
            .codigoBarras(p.getCodigoBarras())
            .nombre(p.getNombre())
            .descripcion(p.getDescripcion())
            .tipo(p.getTipo() != null ? p.getTipo().name() : null)
            .unidadMedida(p.getUnidadMedida() != null ? p.getUnidadMedida().name() : null)
            .zonaPreparacion(p.getZonaPreparacion() != null ? p.getZonaPreparacion().name() : null)
            .moneda(p.getMoneda() != null ? p.getMoneda().name() : null)
            .precioVenta(p.getPrecioVenta())
            .precioBase(p.getPrecioBase())
            .esServicio(p.getEsServicio())
            .activo(p.getActivo())
            .incluyeIva(p.getIncluyeIVA())
            .requierePersonalizacion(p.getRequierePersonalizacion())
            .cabysCode(p.getEmpresaCabys() != null && p.getEmpresaCabys().getCodigoCabys() != null
                ? p.getEmpresaCabys().getCodigoCabys().getCodigo() : null)
            .cabysDescripcion(p.getEmpresaCabys() != null && p.getEmpresaCabys().getCodigoCabys() != null
                ? p.getEmpresaCabys().getCodigoCabys().getDescripcion() : null)
            .imagenUrl(p.getImagenUrl())
            .updatedAt(p.getUpdatedAt());
        
        // Categorías
        if (p.getCategorias() != null) {
            builder.categoriaIds(p.getCategorias().stream()
                .map(CategoriaProducto::getId)
                .collect(Collectors.toList()));
        }
        
        // Impuestos
        if (p.getImpuestos() != null) {
            builder.impuestos(p.getImpuestos().stream()
                .map(i -> ProductoImpuestoSync.builder()
                    .id(i.getId())
                    .tipoImpuesto(i.getTipoImpuesto() != null ? i.getTipoImpuesto().name() : null)
                    .codigoTarifa(i.getCodigoTarifaIVA() != null ? i.getCodigoTarifaIVA().name() : null)
                    .porcentaje(i.getPorcentaje())
                    .activo(i.getActivo())
                    .build())
                .collect(Collectors.toList()));
        }

// Producto compuesto
        if (p.getProductoCompuesto() != null) {
            ProductoCompuesto pc = p.getProductoCompuesto();

            // Mapear slots
            List<SlotSync> slotsSync = new ArrayList<>();
            if (pc.getSlots() != null) {
                for (ProductoCompuestoSlot slot : pc.getSlots()) {
                    // Mapear opciones del slot
                    List<OpcionSync> opcionesSync = new ArrayList<>();
                    if (slot.getOpciones() != null) {
                        opcionesSync = slot.getOpciones().stream()
                            .map(op -> OpcionSync.builder()
                                .id(op.getId())
                                .productoId(op.getProducto() != null ? op.getProducto().getId() : null)
                                .nombre(op.getNombre())
                                .precioAdicional(op.getPrecioAdicional())
                                .esDefault(op.getEsDefault())
                                .disponible(op.getDisponible())
                                .orden(op.getOrden())
                                .build())
                            .collect(Collectors.toList());
                    }

                    slotsSync.add(SlotSync.builder()
                        .id(slot.getId())
                        .nombre(slot.getNombre())
                        .descripcion(slot.getDescripcion())
                        .cantidadMinima(slot.getCantidadMinima())
                        .cantidadMaxima(slot.getCantidadMaxima())
                        .esRequerido(slot.getEsRequerido())
                        .orden(slot.getOrden())
                        .usaFamilia(slot.getUsaFamilia())
                        .familiaId(slot.getFamilia() != null ? slot.getFamilia().getId() : null)
                        .precioAdicionalPorOpcion(slot.getPrecioAdicionalPorOpcion())
                        .opciones(opcionesSync)
                        .build());
                }
            }

            // Mapear configuraciones
            List<ConfiguracionSync> configsSync = new ArrayList<>();
            if (pc.getConfiguraciones() != null) {
                for (ProductoCompuestoConfiguracion config : pc.getConfiguraciones()) {
                    // Mapear slots de la configuración
                    List<SlotConfiguracionSync> slotsConfigSync = new ArrayList<>();
                    if (config.getSlots() != null) {
                        slotsConfigSync = config.getSlots().stream()
                            .map(sc -> SlotConfiguracionSync.builder()
                                .id(sc.getId())
                                .slotId(sc.getSlot().getId())
                                .orden(sc.getOrden())
                                .cantidadMinimaOverride(sc.getCantidadMinimaOverride())
                                .cantidadMaximaOverride(sc.getCantidadMaximaOverride())
                                .esRequeridoOverride(sc.getEsRequeridoOverride())
                                .precioAdicionalOverride(sc.getPrecioAdicionalOverride())
                                .build())
                            .collect(Collectors.toList());
                    }

                    configsSync.add(ConfiguracionSync.builder()
                        .id(config.getId())
                        .nombre(config.getNombre())
                        .descripcion(config.getDescripcion())
                        .opcionTriggerId(config.getOpcionTrigger() != null ? config.getOpcionTrigger().getId() : null)
                        .orden(config.getOrden())
                        .activa(config.getActiva())
                        .esDefault(config.getEsDefault())
                        .slotsConfig(slotsConfigSync)
                        .build());
                }
            }

            builder.compuesto(ProductoCompuestoSync.builder()
                .id(pc.getId())
                .instruccionesPersonalizacion(pc.getInstruccionesPersonalizacion())
                .tiempoPreparacionExtra(pc.getTiempoPreparacionExtra())
                .slotPreguntaInicialId(pc.getSlotPreguntaInicial() != null ? pc.getSlotPreguntaInicial().getId() : null)
                .maxNivelSubpaso(pc.getMaxNivelSubpaso())
                .slots(slotsSync)
                .configuraciones(configsSync)
                .build());
        }

        return builder.build();
    }
    
    private ClienteSync mapCliente(Cliente c) {
        ClienteSync.ClienteSyncBuilder builder = ClienteSync.builder()
            .id(c.getId())
            .tipoIdentificacion(c.getTipoIdentificacion() != null ? c.getTipoIdentificacion().name() : null)
            .numeroIdentificacion(c.getNumeroIdentificacion())
            .razonSocial(c.getRazonSocial())
            .telefonoCodigoPais(c.getTelefonoCodigoPais())
            .telefonoNumero(c.getTelefonoNumero())
            .inscritoHacienda(c.getInscritoHacienda())
            .permiteCredito(c.getPermiteCredito())
            .limiteCredito(c.getLimiteCredito())
            .tieneExoneracion(c.getTieneExoneracion())
            .activo(c.getActivo())
            .updatedAt(c.getUpdatedAt());
        
        // Emails
        if (c.getClienteEmails() != null) {
            builder.emails(c.getClienteEmails().stream()
                .map(e -> ClienteEmailSync.builder()
                    .id(e.getId())
                    .email(e.getEmail())
                    .esPrincipal(e.getEsPrincipal())
                    .build())
                .collect(Collectors.toList()));
        }
        
        // Ubicación
        if (c.getUbicacion() != null) {
            ClienteUbicacion u = c.getUbicacion();
            builder.ubicacion(ClienteUbicacionSync.builder()
                .id(u.getId())
                .provinciaId(u.getProvincia() != null ? u.getProvincia().getId().longValue() : null)
                .cantonId(u.getCanton() != null ? u.getCanton().getId().longValue() : null)
                .distritoId(u.getDistrito() != null ? u.getDistrito().getId().longValue() : null)
                .barrioId(u.getBarrio() != null ? u.getBarrio().getId().longValue() : null)
                .otrasSenas(u.getOtrasSenas())
                .build());
        }
        
        // Actividades
        if (c.getActividades() != null) {
            builder.actividades(c.getActividades().stream()
                .map(a -> ClienteActividadSync.builder()
                    .id(a.getId())
                    .codigoActividad(a.getCodigoActividad())
                    .descripcion(a.getDescripcion())
                    .build())
                .collect(Collectors.toList()));
        }
        
        // Exoneraciones
        if (c.getExoneraciones() != null) {
            builder.exoneraciones(c.getExoneraciones().stream()
                .map(e -> ClienteExoneracionSync.builder()
                    .id(e.getId())
                    .tipoDocumento(e.getTipoDocumento() != null ? e.getTipoDocumento().name() : null)
                    .numeroDocumento(e.getNumeroDocumento())
                    .nombreInstitucion(e.getNombreInstitucion())
                    .fechaEmision(e.getFechaEmision())
                    .fechaVencimiento(e.getFechaVencimiento())
                    .porcentajeExoneracion(e.getPorcentajeExoneracion())
                    .codigoAutorizacion(e.getCodigoAutorizacion())
                    .numeroAutorizacion(String.valueOf(e.getNumeroAutorizacion()))
                    .categoriaCompra(e.getCategoriaCompra())
                    .montoMaximo(e.getMontoMaximo())
                    .poseeCabys(e.getPoseeCabys())
                    .activo(e.getActivo())
                    .cabysAutorizados(e.getCabysAutorizados() != null 
                        ? e.getCabysAutorizados().stream()
                            .map(cab -> cab.getCabys().getCodigo())
                            .collect(Collectors.toList())
                        : null)
                    .build())
                .collect(Collectors.toList()));
        }
        
        return builder.build();
    }
}