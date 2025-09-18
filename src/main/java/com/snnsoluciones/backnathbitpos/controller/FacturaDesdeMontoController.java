package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoRequest;
import com.snnsoluciones.backnathbitpos.dto.pago.FacturaDesdeMontoResponse;
import com.snnsoluciones.backnathbitpos.service.FacturaDesdeMontoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class FacturaDesdeMontoController {

    private final FacturaDesdeMontoService facturaDesdeMontoService;

    @PostMapping("/from-amount")
    public FacturaDesdeMontoResponse generar(@RequestBody FacturaDesdeMontoRequest request) {
        return facturaDesdeMontoService.generar(request);
    }
}