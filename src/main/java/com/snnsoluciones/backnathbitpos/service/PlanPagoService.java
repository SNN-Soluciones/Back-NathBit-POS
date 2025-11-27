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
     */
    public EstadoPagoDTO verificarEstadoPago(Long sucursalId) {
        PlanPago plan = planPagoRepository.findBySucursalId(sucursalId).orElse(null);
        
        // Si no tiene plan, está activo (sucursales sin cobro)
        if (plan == null) {
            return EstadoPagoDTO.builder()
                .activo(true)
                .suspendido(false)
                .tipoAlerta(EstadoPagoDTO.TipoAlerta.SIN_ALERTA)
                .build();
        }
        
        LocalDate hoy = LocalDate.now();
        LocalDate fechaVencimiento = plan.getFechaProximoVencimiento();
        
        long diasRestantes = ChronoUnit.DAYS.between(hoy, fechaVencimiento);
        
        EstadoPagoDTO estado = EstadoPagoDTO.builder()
            .fechaVencimiento(fechaVencimiento)
            .montoAdeudado(plan.getCuotaMensual())
            .diasRestantes((int) diasRestantes)
            .build();
        
        // Si está suspendido
        if (plan.getEstado() == EstadoPlan.SUSPENDIDO) {
            estado.setActivo(false);
            estado.setSuspendido(true);
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.SUSPENDIDO);
            estado.setMensaje("⚠️ SERVICIO SUSPENDIDO ⚠️\n\n" +
                "El servicio de esta sucursal ha sido suspendido por falta de pago.\n" +
                "Por favor contacte con administración para reactivar el servicio.\n\n");
            return estado;
        }
        
        // Determinar tipo de alerta
        estado.setActivo(true);
        estado.setSuspendido(false);
        
        if (diasRestantes > 3) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.SIN_ALERTA);
            
        } else if (diasRestantes == 3) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.AVISO_3_DIAS);
            estado.setMensaje("📅 Recordatorio de Pago\n\n" +
                "Faltan 3 días para el vencimiento de la cuota de esta sucursal.\n" +
                "Fecha de vencimiento: " + formatearFecha(fechaVencimiento) + "\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else if (diasRestantes == 2) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.AVISO_2_DIAS);
            estado.setMensaje("⏰ Recordatorio de Pago\n\n" +
                "Faltan 2 días para el vencimiento de la cuota de esta sucursal.\n" +
                "Fecha de vencimiento: " + formatearFecha(fechaVencimiento) + "\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else if (diasRestantes == 1) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.AVISO_1_DIA);
            estado.setMensaje("⚠️ Recordatorio Urgente de Pago\n\n" +
                "Mañana vence la cuota de esta sucursal.\n" +
                "Fecha de vencimiento: " + formatearFecha(fechaVencimiento) + "\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else if (diasRestantes == 0) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.VENCIDO_HOY);
            estado.setMensaje("🔴 Pago Vencido HOY\n\n" +
                "La cuota de esta sucursal vence hoy.\n" +
                "Por favor realice el pago para evitar la suspensión del servicio.\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else if (diasRestantes == -1) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.VENCIDO_1_DIA);
            estado.setMensaje("🔴 Pago Vencido - 1 día de mora\n\n" +
                "La cuota de esta sucursal se venció ayer.\n" +
                "Tiene " + plan.getDiasGracia() + " días de gracia antes de la suspensión.\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else if (diasRestantes == -2) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.VENCIDO_2_DIAS);
            estado.setMensaje("🔴 Pago Vencido - 2 días de mora\n\n" +
                "La cuota de esta sucursal está vencida.\n" +
                "Le queda 1 día antes de la suspensión del servicio.\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else if (diasRestantes == -3) {
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.VENCIDO_3_DIAS);
            estado.setMensaje("🔴🔴 ÚLTIMO AVISO - 3 días de mora\n\n" +
                "El servicio de esta sucursal será SUSPENDIDO MAÑANA si no realiza el pago.\n" +
                "Por favor realice el pago de inmediato.\n" +
                "Monto: $" + plan.getCuotaMensual());
            
        } else { // diasRestantes < -3
            // Suspender automáticamente
            plan.setEstado(EstadoPlan.SUSPENDIDO);
            planPagoRepository.save(plan);
            
            estado.setActivo(false);
            estado.setSuspendido(true);
            estado.setTipoAlerta(EstadoPagoDTO.TipoAlerta.SUSPENDIDO);
            estado.setMensaje("⚠️ SERVICIO SUSPENDIDO ⚠️\n\n" +
                "El servicio de esta sucursal ha sido suspendido por falta de pago.\n" +
                "Días de mora: " + Math.abs(diasRestantes) + "\n" +
                "Monto adeudado: $" + plan.getCuotaMensual() + "\n\n" +
                "Por favor contacte con administración para reactivar el servicio.");
        }
        
        return estado;
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
}