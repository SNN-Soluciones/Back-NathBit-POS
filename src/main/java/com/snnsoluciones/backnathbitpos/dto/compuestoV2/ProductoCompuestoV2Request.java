package com.snnsoluciones.backnathbitpos.dto.compuestoV2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ProductoCompuestoV2Request {
    private String instruccionesPersonalizacion;

    @NotNull
    @Valid
    private List<SlotV2Request> slots;

    @Data
    public static class SlotV2Request {
        @NotBlank
        private String nombre;
        private String descripcion;
        private Boolean esRequerido = true;
        private Integer cantidadMinima = 1;
        private Integer cantidadMaxima = 1;
        private Integer orden;
        private Boolean usaFamilia = false;
        private Long familiaId;
        private BigDecimal precioAdicionalPorOpcion;
        private List<OpcionV2Request> opciones;
    }

    @Data
    public static class OpcionV2Request {
        private String nombre;
        private Long productoId;
        private BigDecimal precioAdicional = BigDecimal.ZERO;
        private Boolean esDefault = false;
        private Boolean disponible = true;
        private Integer orden;
        private List<SlotV2Request> subSlots; // ← recursivo
    }
}