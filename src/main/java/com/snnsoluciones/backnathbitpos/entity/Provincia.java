package com.snnsoluciones.backnathbitpos.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "provincia")
public class Provincia {
    
    @Id
    private Integer id;
    
    @Column(name = "codigo")
    private Integer codigo;
    
    @Column(name = "provincia", length = 45)
    private String provincia;
}