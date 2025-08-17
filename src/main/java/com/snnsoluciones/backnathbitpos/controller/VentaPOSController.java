package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.EstadoPOSResponse;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.repository.SesionCajaRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pos/estado")
@RequiredArgsConstructor
@Tag(name = "POS - Estado", description = "Estado del punto de venta")
@PreAuthorize("hasAnyRole('CAJERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN', 'ROOT', 'SOPORTE')")
public class VentaPOSController {
    
    private final TerminalRepository terminalRepository;
    private final SesionCajaRepository sesionCajaRepository;
    
    @Operation(summary = "Verificar estado del POS (terminal y sesión)")
    @GetMapping("/verificar")
    public ResponseEntity<ApiResponse<EstadoPOSResponse>> verificarEstado(
            @RequestParam Long terminalId) {
        
        try {
            Terminal terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
            
            if (!terminal.getActiva()) {
                return ResponseEntity.ok(ApiResponse.error(
                    "Terminal inactiva", 
                    EstadoPOSResponse.builder()
                        .terminalActiva(false)
                        .build()
                ));
            }
            
            // Buscar sesión abierta
            SesionCaja sesion = sesionCajaRepository.findSesionAbiertaByTerminalId(terminalId)
                .orElse(null);
            
            EstadoPOSResponse estado = EstadoPOSResponse.builder()
                .terminalId(terminalId)
                .terminalNombre(terminal.getNombre())
                .terminalActiva(true)
                .sucursalId(terminal.getSucursal().getId())
                .sucursalNombre(terminal.getSucursal().getNombre())
                .sesionAbierta(sesion != null)
                .sesionId(sesion != null ? sesion.getId() : null)
                .cajeroNombre(sesion != null ? 
                    sesion.getUsuario().getNombre() + " " + sesion.getUsuario().getApellidos() : null)
                .montoApertura(sesion != null ? sesion.getMontoInicial() : null)
                .fechaApertura(sesion != null ? sesion.getFechaHoraApertura() : null)
                .build();
            
            String mensaje = sesion != null 
                ? "Terminal lista para facturar" 
                : "No hay sesión de caja abierta";
            
            return ResponseEntity.ok(ApiResponse.ok(mensaje, estado));
            
        } catch (Exception e) {
            log.error("Error verificando estado POS: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al verificar estado: " + e.getMessage()));
        }
    }
}