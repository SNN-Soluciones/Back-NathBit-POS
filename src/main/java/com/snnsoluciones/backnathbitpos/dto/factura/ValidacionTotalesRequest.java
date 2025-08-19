package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request para validar totales antes de crear factura
 * Extiende CrearFacturaRequest para reutilizar toda la estructura
 */
public class ValidacionTotalesRequest extends CrearFacturaRequest {
    // Hereda todos los campos de CrearFacturaRequest
    // No necesita campos adicionales ya que la validación
    // usa exactamente la misma estructura que la creación
}