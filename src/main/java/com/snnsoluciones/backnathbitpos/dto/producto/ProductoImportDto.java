package com.snnsoluciones.backnathbitpos.dto.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoImportDto {
    // Mapeo directo desde Excel
    private Long id; // ID original del sistema anterior (opcional)

    @NotBlank(message = "Código es requerido")
    private String codigo;

    @NotBlank(message = "Nombre es requerido")
    private String nombreProducto;

    @NotNull(message = "Precio es requerido")
    private BigDecimal precio;

    private String codigoBarras;
    private BigDecimal precioCompra;
    private Integer existenciaMinima;
    private Boolean productoExento;          // 1 = exento, 0 = gravado
    private Boolean afectaInventario;       // "1" o "2" en Excel
    private String estadoProducto;       // "A" = Activo, "D" = Desactivado

    // Campos calculados o por defecto
    private Long empresaId;
    private Long categoriaId;
    private String cabysId;     // ID del código CABYS

    // Para control de importación
    private Boolean importado = false;
    private String mensajeError;
}