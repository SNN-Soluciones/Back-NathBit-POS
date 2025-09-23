package com.snnsoluciones.backnathbitpos.dto.producto;

import lombok.Data;
import lombok.Builder;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ValidacionSeleccionResponse {
    private boolean esValida;
    private boolean todasDisponiblesEnSucursal;
    private List<ErrorValidacion> errores;
    private List<SlotValidacion> validacionPorSlot;
    
    @Data
    @Builder
    public static class ErrorValidacion {
        private String campo;
        private String mensaje;
        private String tipoError; // FALTA_REQUERIDO, EXCEDE_MAXIMO, NO_DISPONIBLE, SIN_STOCK
    }
    
    @Data
    @Builder
    public static class SlotValidacion {
        private Long slotId;
        private String slotNombre;
        private boolean esRequerido;
        private int cantidadMinima;
        private int cantidadMaxima;
        private int cantidadSeleccionada;
        private boolean cumpleRequisitos;
        private List<OpcionValidada> opcionesSeleccionadas;
    }
    
    @Data
    @Builder
    public static class OpcionValidada {
        private Long opcionId;
        private String productoNombre;
        private boolean tieneStockSuficiente;
        private String mensajeStock;
    }
}