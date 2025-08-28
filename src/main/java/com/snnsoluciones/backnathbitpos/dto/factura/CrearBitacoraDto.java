package com.snnsoluciones.backnathbitpos.dto.factura;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear entrada en bitácora
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearBitacoraDto {
    private Long facturaId;
    private String clave;
}