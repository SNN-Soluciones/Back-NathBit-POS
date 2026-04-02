package com.snnsoluciones.backnathbitpos.dto.sesiones;

import com.snnsoluciones.backnathbitpos.entity.MovimientoCaja;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCajaDetalladoDTO {
    // Información básica
    private Long sesionId;
    private String terminal;
    private String cajero;
    private String autorizadoPor;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    // Montos base
    private BigDecimal montoInicial;
    private BigDecimal montoCierre;
    private BigDecimal montoEsperado;

    // 🆕 NUEVOS CAMPOS PARA ANDROID
    private BigDecimal montoRetirado;
    private BigDecimal fondoCaja;
    private BigDecimal totalDevoluciones;

    // Ventas por tipo de pago
    private BigDecimal ventasEfectivo;
    private BigDecimal ventasTarjeta;
    private BigDecimal ventasTransferencia;
    private BigDecimal ventasOtros;

    private BigDecimal ventasSinpe;           // SINPE separado
    private List<TurnoResumenDTO> turnos;

    @Builder.Default
    private List<VentaPlataformaDTO> ventasPlataformas = new ArrayList<>();

    // Movimientos de caja
    private BigDecimal entradasAdicionales;
    private BigDecimal vales;
    private BigDecimal depositos;

    // Contadores de documentos
    private Integer cantidadFacturas;
    private Integer cantidadTiquetes;
    private Integer cantidadNotasCredito;
    private Integer cantidadVentasInternas;

    // Totales por tipo de documento
    private BigDecimal totalFacturas;
    private BigDecimal totalTiquetes;
    private BigDecimal totalNotasCredito;
    private BigDecimal totalVentasInternas;

    // 🆕 NUEVO: Lista de datafonos para Android
    @Builder.Default
    private List<DatafonoResumenDTO> datafonos = new ArrayList<>();

    // NUEVO: Lista detallada de documentos
    @Builder.Default
    private List<DocumentoResumenDTO> documentos = new ArrayList<>();

    // NUEVO: Lista de vales detallados
    @Builder.Default
    private List<ValeResumenDTO> valesDetalle = new ArrayList<>();

    // Lista de movimientos
    private List<MovimientoCajaDTO> movimientos;

    // NUEVO: DTO interno para documentos
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentoResumenDTO {
        private Long id;
        private String consecutivo;
        private String tipoDocumento;
        private String clienteNombre;
        private BigDecimal total;
        private String estado;
        private String fechaEmision;
        private String metodoPago;
    }

    // NUEVO: DTO interno para vales
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValeResumenDTO {
        private Long id;
        private BigDecimal monto;
        private String concepto;
        private String autorizadoPor;
        private LocalDateTime fecha;
        private String tipo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VentaPlataformaDTO {
        private Long plataformaId;
        private String plataformaNombre;
        private String plataformaCodigo;
        private BigDecimal totalVentas;
        private Integer cantidadTransacciones;
    }

    // 🆕 NUEVO: DTO interno para datafonos
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatafonoResumenDTO {
        private String datafono;
        private BigDecimal monto;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurnoResumenDTO {
        private Long turnoId;
        private String cajeroNombre;
        private String estado;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private BigDecimal fondoInicioTurno;
        private BigDecimal ventasEfectivo;
        private BigDecimal ventasTarjeta;
        private BigDecimal ventasTransferencia;
        private BigDecimal ventasSinpe;
        private BigDecimal totalVentas;
    }
}