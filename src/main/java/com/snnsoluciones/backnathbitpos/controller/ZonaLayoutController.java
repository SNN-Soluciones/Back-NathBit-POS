// controller/ZonaLayoutController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.mesas.GuardarLayoutRequest;
import com.snnsoluciones.backnathbitpos.dto.mesas.MesaLayoutDTO;
import com.snnsoluciones.backnathbitpos.service.mesas.ZonaLayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ZonaLayoutController {

    private final ZonaLayoutService layoutService;

    /**
     * POST /api/zonas/{zonaId}/layout
     * Guardar layout completo (2D o 3D)
     */
    @PostMapping("/{zonaId}/layout")
    public ResponseEntity<Void> guardarLayout(
            @PathVariable Long zonaId,
            @RequestBody GuardarLayoutRequest request) {
        
        log.info("📥 POST /api/zonas/{}/layout - {} elementos", 
            zonaId, request.getMesas().size());
        
        layoutService.guardarLayout(zonaId, request);
        
        log.info("✅ Layout guardado exitosamente para zona {}", zonaId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/zonas/{zonaId}/layout
     * Obtener layout guardado con información de tipos enriquecida
     */
    @GetMapping("/{zonaId}/layout")
    public ResponseEntity<List<MesaLayoutDTO>> obtenerLayout(
            @PathVariable Long zonaId) {
        
        log.info("📤 GET /api/zonas/{}/layout", zonaId);
        
        List<MesaLayoutDTO> layout = layoutService.obtenerLayout(zonaId);
        
        log.info("✅ Layout obtenido: {} elementos", layout.size());
        
        return ResponseEntity.ok(layout);
    }

    /**
     * DELETE /api/zonas/{zonaId}/layout
     * Eliminar layout
     */
    @DeleteMapping("/{zonaId}/layout")
    public ResponseEntity<Void> eliminarLayout(@PathVariable Long zonaId) {
        
        log.info("🗑️ DELETE /api/zonas/{}/layout", zonaId);
        
        layoutService.eliminarLayout(zonaId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * HEAD /api/zonas/{zonaId}/layout
     * Verificar si existe layout
     */
    @RequestMapping(value = "/{zonaId}/layout", method = RequestMethod.HEAD)
    public ResponseEntity<Void> existeLayout(@PathVariable Long zonaId) {
        
        boolean existe = layoutService.existeLayout(zonaId);
        
        return existe 
            ? ResponseEntity.ok().build() 
            : ResponseEntity.notFound().build();
    }
}