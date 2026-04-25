package com.snnsoluciones.backnathbitpos.dto.kiosko;
 
import lombok.*;
import java.util.List;
 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KioskoInitResponse {
 
    // Info del dispositivo
    private Long dispositivoId;
    private String dispositivoNombre;
    private Long terminalId;

    // Info de la sucursal
    private Long sucursalId;
    private String sucursalNombre;
 
    // Sesión de caja activa
    private Long sesionId;
 
    // Configuración del kiosko
    private KioskoConfig config;
 
    // Catálogo
    private List<CategoriaKiosko> categorias;

    private KioscoConfigDTO branding;
 
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class KioskoConfig {
        private boolean permitePagoDirecto;
        private List<String> metodosPago;   // TARJETA, SINPE, EFECTIVO
        private boolean aceptaEfectivo;
        private List<String> modosConsumo;  // AQUI, LLEVAR
        private boolean pausado;
        private int maxItemsOrden;
        private boolean mostrarTiempoEspera;
        private boolean ofrecerFacturaElectronica;
    }
 
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoriaKiosko {
        private Long id;
        private String nombre;
        private String imagen;
        private List<ProductoKiosko> productos;
    }
 
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProductoKiosko {
        private Long id;
        private String nombre;
        private String descripcion;
        private String imagen;
        private java.math.BigDecimal precio;
        private boolean disponible;
        private boolean tieneOpciones; // compuesto v2
        private String tipo; // VENTA, COMBO, COMPUESTO
    }
}