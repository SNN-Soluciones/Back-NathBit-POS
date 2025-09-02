package com.snnsoluciones.backnathbitpos.dto.factura;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaPausadaListDTO {
    private Long id;
    private String descripcion;
    
    // Datos extraídos del JSON para mostrar en la lista
    private String clienteNombre;
    private Integer cantidadItems;
    private Double montoTotal;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaExpiracion;
    
    private String tiempoTranscurrido; // "Hace 5 minutos"
    private String tiempoRestante; // "Expira en 23h"
}