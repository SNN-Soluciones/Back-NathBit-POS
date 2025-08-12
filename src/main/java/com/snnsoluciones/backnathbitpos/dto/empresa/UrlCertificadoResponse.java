package com.snnsoluciones.backnathbitpos.dto.empresa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para URL pre-firmada
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlCertificadoResponse {
    private String url;
    private Integer minutosValidez;
    private String fechaExpiracion;
}