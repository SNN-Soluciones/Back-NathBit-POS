package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tarifas_iva")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TarifaIVA {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "codigo_hacienda", nullable = false, unique = true, length = 2)
    private String codigoHacienda; // 01, 02, 04, etc
    
    @Column(nullable = false, length = 100)
    private String descripcion;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}