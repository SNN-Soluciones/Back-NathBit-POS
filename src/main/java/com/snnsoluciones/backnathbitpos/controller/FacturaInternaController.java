package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.*;
import com.snnsoluciones.backnathbitpos.service.FacturaInternaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/factura-interna")
@RequiredArgsConstructor
public class FacturaInternaController {
    
    private final FacturaInternaService facturaInternaService;
    
    @PostMapping
    public ResponseEntity<FacturaInternaResponse> crear(@Valid @RequestBody FacturaInternaRequest request) {
        return new ResponseEntity<>(facturaInternaService.crear(request), HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FacturaInternaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(facturaInternaService.obtenerPorId(id));
    }
    
    @PostMapping("/{id}/anular")
    public ResponseEntity<Void> anular(@PathVariable Long id, 
                                     @RequestParam String motivo) {
        facturaInternaService.anular(id, motivo);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/sucursal/{sucursalId}/hoy")
    public ResponseEntity<List<FacturaInternaResponse>> facturasHoy(@PathVariable Long sucursalId) {
        return ResponseEntity.ok(facturaInternaService.obtenerFacturasHoy(sucursalId));
    }
}