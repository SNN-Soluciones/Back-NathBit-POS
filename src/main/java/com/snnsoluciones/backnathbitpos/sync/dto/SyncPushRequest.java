package com.snnsoluciones.backnathbitpos.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Request para push - Datos creados/modificados en el POS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPushRequest {
    
    private Long terminalId;
    private Long sucursalId;
    
    // ===== CLIENTES NUEVOS/MODIFICADOS =====
    private List<ClientePush> clientes;
    
    // ===== FACTURAS INTERNAS =====
    private List<FacturaInternaPush> facturasInternas;
    
    // ===== SESIONES DE CAJA =====
    private List<SesionCajaPush> sesionesCaja;
    
    // ===== CONSECUTIVOS ACTUALIZADOS =====
    private TerminalConsecutivosPush consecutivos;
    
    // ===== INNER CLASSES =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientePush {
        private String uuid;                     // UUID local (T001-CLI-123)
        private Long serverId;                   // NULL si es nuevo
        
        private String tipoIdentificacion;
        private String numeroIdentificacion;
        private String razonSocial;
        private String telefonoCodigoPais;
        private String telefonoNumero;
        private Boolean inscritoHacienda;
        private Boolean permiteCredito;
        private BigDecimal limiteCredito;
        private Boolean activo;
        
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // Relaciones
        private List<ClienteEmailPush> emails;
        private ClienteUbicacionPush ubicacion;
        private List<ClienteActividadPush> actividades;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteEmailPush {
        private String email;
        private Boolean esPrincipal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteUbicacionPush {
        private Long provinciaId;
        private Long cantonId;
        private Long distritoId;
        private Long barrioId;
        private String otrasSenas;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteActividadPush {
        private String codigoActividad;
        private String descripcion;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaInternaPush {
        private String uuid;
        private Long serverId;
        
        private String numero;
        private LocalDateTime fecha;
        
        // Referencias (pueden ser UUID o serverId)
        private String clienteUuid;
        private Long clienteServerId;
        private String nombreCliente;
        
        private Long cajeroId;
        private Long meseroId;
        private Long sesionCajaServerId;
        private String sesionCajaUuid;
        
        // Montos
        private BigDecimal subtotal;
        private BigDecimal descuentoPorcentaje;
        private BigDecimal descuento;
        private BigDecimal porcentajeServicio;
        private BigDecimal impuestoServicio;
        private BigDecimal total;
        private BigDecimal pagoRecibido;
        private BigDecimal vuelto;
        
        // Estado
        private String estado;
        private Long anuladaPorId;
        private LocalDateTime fechaAnulacion;
        private String motivoAnulacion;
        
        private String numeroViper;
        private String notas;
        
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // Detalles
        private List<FacturaInternaDetallePush> detalles;
        private List<FacturaInternaMedioPagoPush> mediosPago;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaInternaDetallePush {
        private Long productoId;
        private String codigoProducto;
        private String nombreProducto;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        private BigDecimal descuento;
        private BigDecimal total;
        private String notas;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturaInternaMedioPagoPush {
        private String tipo;
        private BigDecimal monto;
        private String referencia;
        private String banco;
        private String notas;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SesionCajaPush {
        private String uuid;
        private Long serverId;
        
        private Long terminalId;
        private Long usuarioId;
        
        private LocalDateTime fechaHoraApertura;
        private LocalDateTime fechaHoraCierre;
        
        private BigDecimal montoInicial;
        private BigDecimal montoCierre;
        private BigDecimal montoRetirado;
        private BigDecimal fondoCaja;
        
        private BigDecimal totalVentas;
        private BigDecimal totalDevoluciones;
        private BigDecimal totalEfectivo;
        private BigDecimal totalTarjeta;
        private BigDecimal totalTransferencia;
        private BigDecimal totalOtros;
        
        private Integer cantidadFacturas;
        private Integer cantidadTiquetes;
        private Integer cantidadNotasCredito;
        
        private String estado;
        private String observacionesApertura;
        private String observacionesCierre;
        
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminalConsecutivosPush {
        private Long terminalId;
        private Long consecutivoFacturaElectronica;
        private Long consecutivoTiqueteElectronico;
        private Long consecutivoNotaCredito;
        private Long consecutivoNotaDebito;
        private Long consecutivoTiqueteInterno;
        private Long consecutivoFacturaInterna;
        private Long consecutivoProforma;
    }
}