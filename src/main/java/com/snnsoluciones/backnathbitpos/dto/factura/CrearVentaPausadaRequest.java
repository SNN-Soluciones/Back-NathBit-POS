package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

// DTO para crear una venta pausada
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearVentaPausadaRequest {
    private Map<String, Object> datosFactura; // El CrearFacturaRequest completo como Map
    private String descripcion; // Descripción opcional
}