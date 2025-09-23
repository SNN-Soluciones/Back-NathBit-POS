package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import java.util.Set;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

// DTO completo para respuesta
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDto {
    private Long id;
    private Long empresaId;
    private Long sucursalId;
    private String sucursalNombre;
    private String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private EmpresaCABySSelectDto empresaCabys;
    private Set<CategoriaProductoDto> categoriaProductoDtos; // Info de categorías
    private UnidadMedida unidadMedida;
    private Moneda moneda;
    private BigDecimal precioVenta;
    private List<ProductoImpuestoDto> impuestos;
    private TipoProducto tipo;
    private BigDecimal totalImpuestos;
    private BigDecimal precioFinal;
    private Boolean esServicio = false;
    private Boolean incluyeIVA = true;
    private String imagenUrl;
    private Boolean activo;
    private String createdAt;
    private String updatedAt;
}
