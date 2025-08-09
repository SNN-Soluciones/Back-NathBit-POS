package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "monedas")
public class Moneda {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "codigo", length = 3, unique = true, nullable = false)
    private String codigo; // CRC, USD, EUR
    
    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre; // Colón, Dólar, Euro
    
    @Column(name = "simbolo", length = 5)
    private String simbolo; // ₡, $, €
    
    @Column(name = "decimales")
    private Integer decimales = 2;
    
    @Column(nullable = false)
    private Boolean activa = true;
    
    @Column(name = "es_local", nullable = false)
    private Boolean esLocal = false; // true para CRC
    
    @Column(name = "orden")
    private Integer orden = 0;
}