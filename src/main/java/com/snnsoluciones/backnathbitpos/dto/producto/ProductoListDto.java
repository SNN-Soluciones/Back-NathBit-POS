package com.snnsoluciones.backnathbitpos.dto.producto;

import java.util.List;
import lombok.*;
import java.math.BigDecimal;

// DTO simplificado para listados
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoListDto {
    private Long id;
    private String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private List<String> categoriasNombres;
    private BigDecimal precioVenta;
    private String monedaSimbolo;
    private Long empresaId;
    private Boolean activo;
}
