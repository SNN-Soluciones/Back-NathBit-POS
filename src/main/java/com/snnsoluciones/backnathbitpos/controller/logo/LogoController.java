package com.snnsoluciones.backnathbitpos.controller.logo;

import com.snnsoluciones.backnathbitpos.dto.logo.LogoResponseDTO;
import com.snnsoluciones.backnathbitpos.service.logo.LogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logo")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Logo", description = "Gestión de logos para segunda pantalla")
public class LogoController {

    private final LogoService logoService;

    /**
     * Obtiene el logo para una sucursal
     * Prioridad: Logo de Sucursal → Logo de Empresa → Logo Default
     *
     * GET /api/logo/{sucursalId}
     */
    @GetMapping("/{sucursalId}")
    @Operation(
        summary = "Obtener logo para segunda pantalla",
        description = "Devuelve el logo de la sucursal, empresa o logo default según disponibilidad"
    )
    public ResponseEntity<LogoResponseDTO> obtenerLogo(@PathVariable Long sucursalId) {
        log.info("📥 GET /api/logo/{}", sucursalId);

        LogoResponseDTO response = logoService.obtenerLogo(sucursalId);

        log.info("📤 Logo obtenido: tipo={}, url={}", response.getTipoLogo(),
            response.getLogoUrl() != null ? "✅" : "❌");

        return ResponseEntity.ok(response);
    }
}