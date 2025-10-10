package com.snnsoluciones.backnathbitpos.dto.plataforma;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlataformaDigitalDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private BigDecimal porcentajeIncremento;
    private Boolean activo;
    private String colorHex;
    private String icono;
    private Integer orden;
    private String descripcion;
    private Long empresaId; // ⭐ AGREGAR
    private Long sucursalId; // ⭐ AGREGAR
    private String sucursalNombre; // ⭐ AGREGAR (para mostrar en UI)
}