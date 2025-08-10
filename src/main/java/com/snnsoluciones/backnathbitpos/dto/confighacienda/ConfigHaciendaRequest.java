package com.snnsoluciones.backnathbitpos.dto.confighacienda;

import com.snnsoluciones.backnathbitpos.enums.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.TipoAutenticacionHacienda;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    private TipoAutenticacionHacienda tipoAutenticacion;

    @NotBlank(message = "El usuario de Hacienda es requerido")
    @Email(message = "El usuario debe ser un email válido")
    private String usuarioHacienda;

    @NotBlank(message = "La clave de Hacienda es requerida")
    private String claveHacienda;

    // PIN para llave criptográfica (solo si tipoAutenticacion = LLAVE_CRIPTOGRAFICA)
    private String pinLlaveCriptografica;

    // Certificado en Base64 (para futuras versiones)
    private String certificadoBase64;

    @NotBlank(message = "El proveedor de sistemas es requerido")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Identificación del proveedor inválida")
    private String proveedorSistemas;

    // Código de actividad económica principal
    @Pattern(regexp = "^[0-9]{6}$", message = "Código de actividad debe tener 6 dígitos")
    private String actividadEconomicaPrincipal;

    @NotNull(message = "La empresa es requerida")
    private Long empresaId;
}