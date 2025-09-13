package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.hibernate.proxy.HibernateProxy;

@Data
@Entity
@Table(name = "productos_recetas",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"empresa_id", "producto_id"})
    }
)
public class ProductoReceta  {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    
    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
    
    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private Boolean estado = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecetaIngrediente> ingredientes = new ArrayList<>();
    
    // Método helper para agregar ingrediente
    public void agregarIngrediente(RecetaIngrediente ingrediente) {
        ingredientes.add(ingrediente);
        ingrediente.setReceta(this);
    }
    
    // Método helper para remover ingrediente
    public void removerIngrediente(RecetaIngrediente ingrediente) {
        ingredientes.remove(ingrediente);
        ingrediente.setReceta(null);
    }
    
    // Método para calcular costo basado en ingredientes
    public void calcularCosto() {
        this.costoEstimado = ingredientes.stream()
            .map(ing -> {
                BigDecimal precioUnitario = ing.getProducto().getPrecioCompra();
                if (precioUnitario == null) return BigDecimal.ZERO;
                
                // Si hay factor de conversión, ajustar el precio
                if (ing.getProducto().getFactorConversion() != null) {
                    precioUnitario = precioUnitario.divide(
                        ing.getProducto().getFactorConversion(), 
                        4, 
                        BigDecimal.ROUND_HALF_UP
                    );
                }
                
                return precioUnitario.multiply(ing.getCantidad());
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        ProductoReceta that = (ProductoReceta) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy
            ? ((HibernateProxy) this).getHibernateLazyInitializer()
            .getPersistentClass().hashCode() : getClass().hashCode();
    }
}