package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.ActividadEconomica;
import com.snnsoluciones.backnathbitpos.repository.ActividadEconomicaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actividades-economicas")
@RequiredArgsConstructor
@Tag(name = "Actividades Económicas", description = "Catálogo de actividades económicas")
public class ActividadEconomicaController {

    private final ActividadEconomicaRepository actividadRepository;

    @Operation(summary = "Buscar actividades económicas")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROOT', 'SOPORTE', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ActividadEconomica>>> buscar(
        @RequestParam(required = false) String search) {
        
        List<ActividadEconomica> actividades;
        
        if (search != null && !search.trim().isEmpty()) {
            // Buscar por código o descripción
            actividades = actividadRepository.findByDescripcionContaining(search);
        } else {
            // Retornar solo las activas
            actividades = actividadRepository.findByActivaTrue();
        }
        
        return ResponseEntity.ok(ApiResponse.ok(
            "Actividades encontradas: " + actividades.size(), 
            actividades
        ));
    }
}