package com.snnsoluciones.backnathbitpos.dto.empresa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para subir certificado de empresa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificadoRequest {
    
    @NotNull(message = "El archivo del certificado es requerido")
    private MultipartFile certificado;
    
    @NotBlank(message = "El PIN del certificado es requerido")
    private String pin;
}