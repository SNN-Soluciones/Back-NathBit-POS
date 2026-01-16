// CategoriaMenuItemResponse.java
package com.snnsoluciones.backnathbitpos.dto.producto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaMenuItemResponse {

    private Long id;
    private Long categoriaMenuId;
    private ProductoHijoDTO productoHijo;
    private Integer orden;
    private Boolean destacado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductoHijoDTO {
        private Long id;
        private String nombre;
        private String codigoInterno;
        private String tipo;
        private BigDecimal precioVenta;
        private String imagenUrl;
        private String descripcion;
        private Boolean activo;
    }
}
