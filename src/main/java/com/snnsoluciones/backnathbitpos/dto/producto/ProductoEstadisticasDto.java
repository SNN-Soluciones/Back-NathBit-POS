package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.*;
import java.util.Map;
import java.util.List;

// DTO para estadísticas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoEstadisticasDto {
    private Long totalProductos;
    private Long totalServicios;
    private Long totalBienes;
    private Long productosSinCategoria;
    private Map<String, Long> productosPorCategoria;
}
