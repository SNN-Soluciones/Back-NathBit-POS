// HaciendaProxyController.java
package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.ContribuyenteDTO;
import com.snnsoluciones.backnathbitpos.service.HaciendaProxyService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


// HaciendaProxyController.java
@RestController
@RequestMapping("/api/proxy/hacienda")
public class HaciendaProxyController {

    private final HaciendaProxyService service;

    public HaciendaProxyController(HaciendaProxyService service) {
        this.service = service;
    }

    @GetMapping("/contribuyente/{identificacion}")
    public ResponseEntity<ContribuyenteDTO> getContribuyente(
        @PathVariable String identificacion
    ) {
        return ResponseEntity.ok(service.consultar(identificacion));
    }
}