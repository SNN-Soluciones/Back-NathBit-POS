package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductoCompuestoRequest {

    @Size(max = 500, message = "Las instrucciones no pueden exceder 500 caracteres")
    private String instruccionesPersonalizacion;

    @Min(value = 0, message = "El tiempo extra no puede ser negativo")
    private Integer tiempoPreparacionExtra;

    @NotEmpty(message = "Debe incluir al menos un slot de personalización")
    private List<SlotRequest> slots;

    @Data
    public static class SlotRequest {
        @NotBlank(message = "El nombre del slot es requerido")
        @Size(max = 100)
        private String nombre;

        private String descripcion;

        @NotNull
        @Min(0)
        private Integer cantidadMinima;

        @NotNull
        @Min(1)
        private Integer cantidadMaxima;

        @NotNull
        private Boolean esRequerido;

        private Integer orden;

        private Boolean usaFamilia;

        private Long familiaId;

        private BigDecimal precioAdicionalPorOpcion;

        private List<OpcionRequest> opciones;
    }

    @Data
    public static class OpcionRequest {
        @NotNull(message = "El producto es requerido")
        private Long productoId;

        @NotNull(message = "El precio adicional es requerido")
        private BigDecimal precioAdicional; // Puede ser negativo!

        private Boolean esDefault = false;
        private Boolean disponible = true;
        private Integer orden;
    }
}