package com.snnsoluciones.backnathbitpos.dto.empresa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de certificado
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificadoResponse {
    private String mensaje;
    private boolean exitoso;
    private String fechaVencimiento;
    private String nombreEmpresa;
    private String identificacion;
    private String urlCertificadoKey; // Nueva propiedad para retornar la URL/key
}
