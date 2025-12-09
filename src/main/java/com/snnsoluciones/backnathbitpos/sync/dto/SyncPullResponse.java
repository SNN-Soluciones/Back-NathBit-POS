package com.snnsoluciones.backnathbitpos.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response del pull - Todos los datos actualizados desde lastSync
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncPullResponse {
    
    private LocalDateTime syncTimestamp;
    private Integer totalRegistros;
    
    // ===== CONFIG =====
    private EmpresaSync empresa;
    private SucursalSync sucursal;
    private TerminalSync terminal;
    private List<UsuarioSync> usuarios;
    private List<MedioPagoSync> mediosPago;
    private List<ImpuestoSync> impuestos;
    
    // ===== CATÁLOGOS =====
    private List<ProvinciaSync> provincias;
    private List<CantonSync> cantones;
    private List<DistritoSync> distritos;
    private List<BarrioSync> barrios;
    private List<ActividadEconomicaSync> actividadesEconomicas;
    
    // ===== PRODUCTOS =====
    private List<CategoriaSync> categorias;
    private List<FamiliaProductoSync> familias;
    private List<ProductoSync> productos;
    
    // ===== CLIENTES =====
    private List<ClienteSync> clientes;
    
    // ===== INNER CLASSES =====
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaSync {
        private Long id;
        private String tipoIdentificacion;
        private String identificacion;
        private String nombreComercial;
        private String nombreRazonSocial;
        private String telefono;
        private String email;
        private Long provinciaId;
        private Long cantonId;
        private Long distritoId;
        private Long barrioId;
        private String otrasSenas;
        private Boolean requiereHacienda;
        private String regimenTributario;
        private String logoUrl;
        private LocalDateTime updatedAt;
        private List<ActividadEmpresaSync> actividades;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActividadEmpresaSync {
        private Long id;
        private String codigoActividad;
        private String descripcion;
        private Boolean esPrincipal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SucursalSync {
        private Long id;
        private String nombre;
        private String numeroSucursal;
        private String modoFacturacion;
        private Long provinciaId;
        private Long cantonId;
        private Long distritoId;
        private Long barrioId;
        private String otrasSenas;
        private Boolean manejaInventario;
        private Boolean aplicaRecetas;
        private Boolean permiteNegativos;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TerminalSync {
        private Long id;
        private String numeroTerminal;
        private String nombre;
        private String descripcion;
        private Long consecutivoFacturaElectronica;
        private Long consecutivoTiqueteElectronico;
        private Long consecutivoNotaCredito;
        private Long consecutivoNotaDebito;
        private Long consecutivoTiqueteInterno;
        private Long consecutivoFacturaInterna;
        private Long consecutivoProforma;
        private String tipoImpresion;
        private Boolean imprimirAutomatico;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioSync {
        private Long id;
        private String nombre;
        private String apellidos;
        private String email;
        private String username;
        private String pin;
        private Integer pinLongitud;
        private String rol;
        private Boolean activo;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedioPagoSync {
        private Long id;
        private String codigo;
        private String nombre;
        private String descripcion;
        private Boolean requiereReferencia;
        private Boolean activo;
        private Integer orden;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpuestoSync {
        private Long id;
        private String codigo;
        private String nombre;
        private java.math.BigDecimal porcentaje;
        private String tipo;
        private Boolean activo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProvinciaSync {
        private Long id;
        private String codigo;
        private String nombre;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CantonSync {
        private Long id;
        private Long provinciaId;
        private String codigo;
        private String nombre;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistritoSync {
        private Long id;
        private Long cantonId;
        private String codigo;
        private String nombre;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BarrioSync {
        private Long id;
        private Long distritoId;
        private String codigo;
        private String nombre;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActividadEconomicaSync {
        private Long id;
        private String codigo;
        private String descripcion;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaSync {
        private Long id;
        private String nombre;
        private String descripcion;
        private String color;
        private String icono;
        private Integer orden;
        private Boolean activo;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamiliaProductoSync {
        private Long id;
        private String nombre;
        private String descripcion;
        private String codigo;
        private String color;
        private String icono;
        private Boolean activa;
        private Integer orden;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoSync {
        private Long id;
        private Long familiaId;
        private String codigoInterno;
        private String codigoBarras;
        private String nombre;
        private String descripcion;
        private String tipo;
        private String unidadMedida;
        private String zonaPreparacion;
        private String moneda;
        private java.math.BigDecimal precioVenta;
        private java.math.BigDecimal precioBase;
        private Boolean esServicio;
        private Boolean activo;
        private Boolean incluyeIva;
        private Boolean requierePersonalizacion;
        private String cabysCode;
        private String cabysDescripcion;
        private String imagenUrl;
        private LocalDateTime updatedAt;
        
        // Relaciones
        private List<Long> categoriaIds;
        private List<ProductoImpuestoSync> impuestos;
        private ProductoCompuestoSync compuesto;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoImpuestoSync {
        private Long id;
        private String tipoImpuesto;
        private String codigoTarifa;
        private java.math.BigDecimal porcentaje;
        private Boolean activo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoCompuestoSync {
        private Long id;
        private String instruccionesPersonalizacion;
        private Integer tiempoPreparacionExtra;
        private Long slotPreguntaInicialId;
        private Integer maxNivelSubpaso;
        private List<SlotSync> slots;
        private List<ConfiguracionSync> configuraciones;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotSync {
        private Long id;
        private String nombre;
        private String descripcion;
        private Integer cantidadMinima;
        private Integer cantidadMaxima;
        private Boolean esRequerido;
        private Integer orden;
        private Boolean usaFamilia;
        private Long familiaId;
        private java.math.BigDecimal precioAdicionalPorOpcion;
        private List<OpcionSync> opciones;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpcionSync {
        private Long id;
        private Long productoId;
        private String nombre;
        private java.math.BigDecimal precioAdicional;
        private Boolean esDefault;
        private Boolean disponible;
        private Integer orden;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfiguracionSync {
        private Long id;
        private String nombre;
        private String descripcion;
        private Long opcionTriggerId;
        private Integer orden;
        private Boolean activa;
        private Boolean esDefault;
        private List<SlotConfiguracionSync> slotsConfig;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotConfiguracionSync {
        private Long id;
        private Long slotId;
        private Integer orden;
        private Integer cantidadMinimaOverride;
        private Integer cantidadMaximaOverride;
        private Boolean esRequeridoOverride;
        private java.math.BigDecimal precioAdicionalOverride;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteSync {
        private Long id;
        private String tipoIdentificacion;
        private String numeroIdentificacion;
        private String razonSocial;
        private String telefonoCodigoPais;
        private String telefonoNumero;
        private Boolean inscritoHacienda;
        private Boolean permiteCredito;
        private java.math.BigDecimal limiteCredito;
        private Boolean tieneExoneracion;
        private Boolean activo;
        private LocalDateTime updatedAt;
        
        // Relaciones
        private List<ClienteEmailSync> emails;
        private ClienteUbicacionSync ubicacion;
        private List<ClienteActividadSync> actividades;
        private List<ClienteExoneracionSync> exoneraciones;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteEmailSync {
        private Long id;
        private String email;
        private Boolean esPrincipal;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteUbicacionSync {
        private Long id;
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
    public static class ClienteActividadSync {
        private Long id;
        private String codigoActividad;
        private String descripcion;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteExoneracionSync {
        private Long id;
        private String tipoDocumento;
        private String numeroDocumento;
        private String nombreInstitucion;
        private java.time.LocalDate fechaEmision;
        private java.time.LocalDate fechaVencimiento;
        private java.math.BigDecimal porcentajeExoneracion;
        private String codigoAutorizacion;
        private String numeroAutorizacion;
        private String categoriaCompra;
        private java.math.BigDecimal montoMaximo;
        private Boolean poseeCabys;
        private Boolean activo;
        private List<String> cabysAutorizados;
    }
}