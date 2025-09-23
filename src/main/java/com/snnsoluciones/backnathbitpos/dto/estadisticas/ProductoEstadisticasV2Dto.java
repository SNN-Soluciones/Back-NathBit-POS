package com.snnsoluciones.backnathbitpos.dto.estadisticas;

import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import lombok.*;
import java.util.Map;
import java.util.List;

/**
 * DTO para estadísticas de productos V2
 * Compatible con el frontend y maneja empresa/sucursal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoEstadisticasV2Dto {
    
    // Totales generales
    private Long total;
    private Long activos;
    private Long inactivos;
    
    // Por tipo de producto
    private Map<TipoProducto, Long> porTipo;
    
    // Por categorías (top 10)
    private List<CategoriaEstadistica> topCategorias;
    
    // Productos más vendidos
    private List<ProductoTopVenta> topProductosVendidos;
    
    // Adicionales
    private Long sinCategoria;
    private Long conBajoStock; // Para futuro uso con inventarios
    
    // Metadata
    private Long empresaId;
    private Long sucursalId; // null si es a nivel empresa
    private String periodo; // "HOY", "SEMANA", "MES", "TOTAL"
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaEstadistica {
        private Long categoriaId;
        private String categoriaNombre;
        private Long cantidadProductos;
        private Double porcentajeDelTotal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoTopVenta {
        private Long productoId;
        private String codigoInterno;
        private String nombre;
        private String tipo;
        private Long cantidadVendida;
        private Double montoTotalVendido;
        private String ultimaVenta; // ISO date string
        private Integer ranking;
    }
}