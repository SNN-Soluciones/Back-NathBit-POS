package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import java.util.List;
import java.util.Set;
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
    private Set<CategoriaProductoDto> categorias;
    private List<ProductoImpuestoDto> impuestos;
    private BigDecimal precioVenta;
    private EmpresaCABySSelectDto empresaCabys;
    private UnidadMedida unidadMedida;
    private TipoProducto tipo;
    private Moneda moneda;
    private Long empresaId;
    private Boolean activo;
    private Boolean aplicaServicio = false;
    private Boolean esServicio = false;
    private String imagenUrl;
    private String thumbnailUrl;
}
