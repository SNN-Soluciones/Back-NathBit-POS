package com.snnsoluciones.backnathbitpos.dto.cliente;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUbicacionDTO {
    private Long id;
    private String provinciaId;
    private String provinciaNombre;
    private String cantonId;
    private String cantonNombre;
    private String distritoId;
    private String distritoNombre;
    private Long barrioId;
    private String barrioNombre;
    private String otrasSenas;
    private String direccionCompleta; // Formato: Provincia, Cantón, Distrito, Barrio - Otras señas
}