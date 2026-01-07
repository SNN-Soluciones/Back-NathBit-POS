package com.snnsoluciones.backnathbitpos.dto.inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para carga inicial de inventarios en lote
 * Permite cargar múltiples productos a la vez en una sucursal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargaInicialInventarioDTO {

    @NotNull(message = "La sucursal es requerida")
    private Long sucursalId;

    @NotEmpty(message = "Debe incluir al menos un producto")
    @Valid
    private List<ProductoInventarioInicialDTO> productos;

    private String observaciones;

    /**
     * Item individual de carga inicial
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoInventarioInicialDTO {

        @NotNull(message = "El producto es requerido")
        private Long productoId;

        @NotNull(message = "La cantidad inicial es requerida")
        private BigDecimal cantidadInicial;

        private BigDecimal cantidadMinima; // Stock mínimo (para alertas)

        private BigDecimal precioCompra; // Opcional, para costo

        private String observaciones; // Opcional, por producto
    }
}