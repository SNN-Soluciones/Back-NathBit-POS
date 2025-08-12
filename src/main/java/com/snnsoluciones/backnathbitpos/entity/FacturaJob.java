package com.snnsoluciones.backnathbitpos.entity;

import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoProcesoJob;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "factura_jobs", indexes = {
    @Index(name = "idx_factura_job_clave", columnList = "clave"),
    @Index(name = "idx_factura_job_estado", columnList = "estado_proceso"),
    @Index(name = "idx_factura_job_proxima", columnList = "proxima_ejecucion")
})
public class FacturaJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 50, nullable = false)
    private String clave;
    
    @Column(name = "factura_id", nullable = false)
    private Long facturaId;
    
    @Column(name = "estado_proceso", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoProcesoJob estadoProceso = EstadoProcesoJob.PENDIENTE;
    
    @Column(name = "paso_actual", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private PasoFacturacion pasoActual = PasoFacturacion.GENERAR_XML;
    
    @Column(nullable = false)
    private Integer intentos = 0;
    
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;
    
    @Column(name = "proxima_ejecucion", nullable = false)
    private LocalDateTime proximaEjecucion = LocalDateTime.now();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helpers
    public void incrementarIntentos() {
        this.intentos++;
        // Backoff exponencial: 1min, 2min, 4min, 8min...
        int minutosEspera = (int) Math.pow(2, intentos - 1);
        this.proximaEjecucion = LocalDateTime.now().plusMinutes(minutosEspera);
    }
    
    public boolean puedeReintentarse() {
        return intentos < 5 && estadoProceso.puedeReintentar();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> oEffectiveClass = o instanceof HibernateProxy
            ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) {
            return false;
        }
        FacturaJob that = (FacturaJob) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}