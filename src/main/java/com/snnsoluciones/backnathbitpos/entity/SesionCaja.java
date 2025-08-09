package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.EstadoSesion;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sesiones_caja")
@ToString(exclude = {"terminal", "usuario"})
public class SesionCaja {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "fecha_hora_apertura", nullable = false)
    private LocalDateTime fechaHoraApertura;
    
    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;
    
    @Column(name = "monto_inicial", precision = 18, scale = 2, nullable = false)
    private BigDecimal montoInicial = BigDecimal.ZERO;
    
    @Column(name = "monto_cierre", precision = 18, scale = 2)
    private BigDecimal montoCierre;
    
    // Montos de operación
    @Column(name = "total_ventas", precision = 18, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;
    
    @Column(name = "total_devoluciones", precision = 18, scale = 2)
    private BigDecimal totalDevoluciones = BigDecimal.ZERO;
    
    @Column(name = "total_efectivo", precision = 18, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;
    
    @Column(name = "total_tarjeta", precision = 18, scale = 2)
    private BigDecimal totalTarjeta = BigDecimal.ZERO;
    
    @Column(name = "total_transferencia", precision = 18, scale = 2)
    private BigDecimal totalTransferencia = BigDecimal.ZERO;
    
    @Column(name = "total_otros", precision = 18, scale = 2)
    private BigDecimal totalOtros = BigDecimal.ZERO;
    
    // Contadores de documentos
    @Column(name = "cantidad_facturas")
    private Integer cantidadFacturas = 0;
    
    @Column(name = "cantidad_tiquetes")
    private Integer cantidadTiquetes = 0;
    
    @Column(name = "cantidad_notas_credito")
    private Integer cantidadNotasCredito = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoSesion estado = EstadoSesion.ABIERTA;
    
    @Column(name = "observaciones_apertura", length = 500)
    private String observacionesApertura;
    
    @Column(name = "observaciones_cierre", length = 500)
    private String observacionesCierre;
    
    // Información de cierre
    @Column(name = "diferencia_cierre", precision = 18, scale = 2)
    private BigDecimal diferenciaCierre;
    
    @Column(name = "ip_apertura", length = 45)
    private String ipApertura;
    
    @Column(name = "ip_cierre", length = 45)
    private String ipCierre;
    
    @PrePersist
    protected void onCreate() {
        fechaHoraApertura = LocalDateTime.now();
        
        // Validar que no haya otra sesión abierta para el usuario
        // Esto se debe hacer en el servicio, no aquí
    }
    
    /**
     * Calcula el monto esperado en caja
     */
    public BigDecimal calcularMontoEsperado() {
        return montoInicial
            .add(totalVentas)
            .subtract(totalDevoluciones);
    }
    
    /**
     * Calcula la diferencia al cerrar
     */
    public BigDecimal calcularDiferencia() {
        if (montoCierre == null) return BigDecimal.ZERO;
        return montoCierre.subtract(calcularMontoEsperado());
    }
    
    /**
     * Valida si la sesión puede cerrarse
     */
    public boolean puedeCerrarse() {
        return estado == EstadoSesion.ABIERTA && fechaHoraCierre == null;
    }
}