package com.snnsoluciones.backnathbitpos.dto.producto;

import com.snnsoluciones.backnathbitpos.enums.TipoInventario;
import com.snnsoluciones.backnathbitpos.enums.TipoProducto;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.UnidadMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoUpdateDto {
    
    @NotBlank(message = "Código interno es requerido")
    @Size(max = 20, message = "Código interno máximo 20 caracteres")
    private String codigoInterno;
    
    @Size(max = 30, message = "Código de barras máximo 30 caracteres")
    private String codigoBarras;
    
    @NotBlank(message = "Nombre es requerido")
    @Size(max = 200, message = "Nombre máximo 200 caracteres")
    private String nombre;
    
    private String descripcion;
    
    @NotNull(message = "Código CAByS es requerido")
    private Long empresaCabysId;
    
    private Long categoriaId;

    private Long familiaId;
    
    @NotNull(message = "Unidad de medida es requerida")
    private UnidadMedida unidadMedida;  // ENUM directamente
    
    @NotNull(message = "Moneda es requerida")
    private Moneda moneda;  // ENUM directamente
    
    @NotNull(message = "Precio de venta es requerido")
    @DecimalMin(value = "0.00", message = "Precio debe ser mayor o igual a 0")
    @Digits(integer = 13, fraction = 5, message = "Precio formato inválido")
    private BigDecimal precioVenta;

    @Builder.Default
    private Boolean esServicio = false;
    
    @Builder.Default
    private Boolean activo = true;

    private TipoProducto tipo;

    private BigDecimal precioCompra;

    @Schema(description = "Indica si se debe actualizar la configuración tributaria (CABYS, impuestos, unidad medida)")
    @Builder.Default
    private Boolean actualizarConfigTributaria = false;

    // Campos tributarios (solo se usan si actualizarConfigTributaria = true)
    private List<ProductoImpuestoCreateDto> impuestos;

    private TipoInventario tipoInventario;

    private BigDecimal factorConversionReceta;
}