package com.snnsoluciones.backnathbitpos.dto.confighacienda;

import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoAutenticacionHacienda;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigHaciendaRequest {

    @NotNull(message = "El ambiente es requerido")
    private AmbienteHacienda ambiente;

    @NotNull(message = "El tipo de autenticación es requerido")
    private TipoAutenticacionHacienda tipoAutenticacion = TipoAutenticacionHacienda.LLAVE_CRIPTOGRAFICA;

    @NotBlank(message = "El usuario de Hacienda es requerido")
    @Email(message = "El usuario debe ser un email válido")
    private String usuarioHacienda;

    @NotBlank(message = "La clave de Hacienda es requerida")
    private String claveHacienda;

    // Código de actividad económica principal
    @Pattern(regexp = "^[0-9]{6}$", message = "Código de actividad debe tener 6 dígitos")
    private String actividadEconomicaPrincipal;

    @NotNull(message = "La empresa es requerida")
    private Long empresaId;

    // URL/Key del certificado en S3 (viene del endpoint de subir certificado)
    private String urlCertificadoKey;

    // Fecha de vencimiento del certificado (viene del endpoint de subir certificado)
    private LocalDate fechaVencimientoCertificado;

    // PIN del certificado - UNIFICADO (se usa este único campo)
    private String pinCertificado;

    // === CAMPOS OPCIONALES ===

    // Mensajes personalizados para facturas
    private String notaFactura;
    private String notaValidezProforma;
    private String detalleFactura1;
    private String detalleFactura2;
}