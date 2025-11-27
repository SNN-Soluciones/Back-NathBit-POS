// src/main/java/com/snnsoluciones/backnathbitpos/controller/PlanPagoController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.pagos.EstadoPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.HistorialPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.PlanPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.RegistrarPagoDTO;
import com.snnsoluciones.backnathbitpos.dto.pagos.ResumenPagosEmpresaDTO;
import com.snnsoluciones.backnathbitpos.service.PlanPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planes-pago")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PlanPagoController {
    
    private final PlanPagoService planPagoService;
    
    /**
     * Verificar estado de pago de una sucursal
     */
    @GetMapping("/estado/sucursal/{sucursalId}")
    public ResponseEntity<EstadoPagoDTO> verificarEstadoSucursal(@PathVariable Long sucursalId) {
        log.info("Verificando estado de pago - Sucursal: {}", sucursalId);
        EstadoPagoDTO estado = planPagoService.verificarEstadoPago(sucursalId);
        return ResponseEntity.ok(estado);
    }
    
    /**
     * Obtener plan de una sucursal
     */
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<PlanPagoDTO> obtenerPlanSucursal(@PathVariable Long sucursalId) {
        PlanPagoDTO plan = planPagoService.obtenerPlanSucursal(sucursalId);
        return ResponseEntity.ok(plan);
    }
    
    /**
     * Obtener todos los planes de una empresa
     */
    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<List<PlanPagoDTO>> obtenerPlanesPorEmpresa(@PathVariable Long empresaId) {
        List<PlanPagoDTO> planes = planPagoService.obtenerPlanesPorEmpresa(empresaId);
        return ResponseEntity.ok(planes);
    }
    
    /**
     * Obtener resumen de pagos de una empresa
     */
    @GetMapping("/empresa/{empresaId}/resumen")
    public ResponseEntity<ResumenPagosEmpresaDTO> obtenerResumenEmpresa(@PathVariable Long empresaId) {
        ResumenPagosEmpresaDTO resumen = planPagoService.obtenerResumenEmpresa(empresaId);
        return ResponseEntity.ok(resumen);
    }
    
    /**
     * Listar todos los planes
     */
    @GetMapping("/todos")
    public ResponseEntity<List<PlanPagoDTO>> listarTodos() {
        List<PlanPagoDTO> planes = planPagoService.listarTodos();
        return ResponseEntity.ok(planes);
    }
    
    /**
     * Crear plan de pago
     */
    @PostMapping("/crear")
    public ResponseEntity<PlanPagoDTO> crearPlan(@RequestBody PlanPagoDTO dto) {
        log.info("Creando plan - Sucursal: {}, Cuota: {}", dto.getSucursalId(), dto.getCuotaMensual());
        PlanPagoDTO plan = planPagoService.crearPlan(dto);
        return ResponseEntity.ok(plan);
    }
    
    /**
     * Actualizar plan de pago
     */
    @PutMapping("/{id}")
    public ResponseEntity<PlanPagoDTO> actualizarPlan(
            @PathVariable Long id,
            @RequestBody PlanPagoDTO dto) {
        log.info("Actualizando plan: {}", id);
        PlanPagoDTO plan = planPagoService.actualizarPlan(id, dto);
        return ResponseEntity.ok(plan);
    }
    
    /**
     * Registrar un pago
     */
    @PostMapping("/registrar-pago")
    public ResponseEntity<Map<String, String>> registrarPago(
            @RequestBody RegistrarPagoDTO dto,
            @RequestHeader("X-Usuario-Id") Long usuarioId) {
        log.info("Registrando pago - Sucursal: {}, Monto: {}", dto.getSucursalId(), dto.getMonto());
        planPagoService.registrarPago(dto, usuarioId);
        return ResponseEntity.ok(Map.of("mensaje", "Pago registrado exitosamente"));
    }
    
    /**
     * Obtener historial de pagos de una sucursal
     */
    @GetMapping("/historial/sucursal/{sucursalId}")
    public ResponseEntity<List<HistorialPagoDTO>> obtenerHistorialSucursal(@PathVariable Long sucursalId) {
        List<HistorialPagoDTO> historial = planPagoService.obtenerHistorial(sucursalId);
        return ResponseEntity.ok(historial);
    }
    
    /**
     * Suspender plan
     */
    @PostMapping("/suspender/{sucursalId}")
    public ResponseEntity<Map<String, String>> suspenderPlan(@PathVariable Long sucursalId) {
        log.warn("Suspendiendo plan - Sucursal: {}", sucursalId);
        planPagoService.suspenderPlan(sucursalId);
        return ResponseEntity.ok(Map.of("mensaje", "Plan suspendido"));
    }
    
    /**
     * Reactivar plan
     */
    @PostMapping("/reactivar/{sucursalId}")
    public ResponseEntity<Map<String, String>> reactivarPlan(@PathVariable Long sucursalId) {
        log.info("Reactivando plan - Sucursal: {}", sucursalId);
        planPagoService.reactivarPlan(sucursalId);
        return ResponseEntity.ok(Map.of("mensaje", "Plan reactivado"));
    }
}