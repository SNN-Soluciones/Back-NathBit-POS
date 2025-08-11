package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_impuesto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoImpuesto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "codigo", nullable = false, unique = true, length = 2)
    private String codigo;
    
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}