package com.snnsoluciones.backnathbitpos.dto.empresa;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ActividadEconomicaRequest;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.RegimenTributario;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CrearEmpresaCompletaRequest {
    
    // ===== DATOS BÁSICOS EMPRESA =====
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100)
    private String nombreRazonSocial;
    
    @Size(max = 100)
    private String nombreComercial;
    
    @NotNull(message = "El tipo de identificación es requerido")
    private TipoIdentificacion tipoIdentificacion;
    
    @NotBlank(message = "La identificación es requerida")
    @Pattern(regexp = "^[0-9-]+$", message = "La identificación solo puede contener números y guiones")
    private String identificacion;
    
    @NotBlank(message = "El email es requerido")
    @Email
    private String email;
    
    @Email
    private String emailNotificacion;
    
    @Pattern(regexp = "^\\d{4}-?\\d{4}$", message = "Formato de teléfono inválido")
    private String telefono;
    
    @Pattern(regexp = "^\\d{4}-?\\d{4}$", message = "Formato de fax inválido")
    private String fax;
    
    // ===== UBICACIÓN =====
    private Integer provinciaId;
    private Integer cantonId;
    private Integer distritoId;
    private Integer barrioId;

    @Size(max = 500)
    private String otrasSenas;
    
    // ===== CONFIGURACIÓN =====
    @NotNull
    private Boolean activa = true;
    
    @NotNull(message = "Debe indicar si requiere facturación electrónica")
    private Boolean requiereHacienda;
    
    private RegimenTributario regimenTributario = RegimenTributario.REGIMEN_TRADICIONAL;
    
    // ===== CONFIGURACIÓN HACIENDA (si requiereHacienda = true) =====
    private ConfigHaciendaData configHacienda;
    
    @Data
    public static class ConfigHaciendaData {
        @NotNull
        private AmbienteHacienda ambiente = AmbienteHacienda.SANDBOX;
        
        @Email
        private String usuarioHacienda;
        
        private String claveHacienda;
        
        @Pattern(regexp = "^\\d{4}$", message = "El PIN debe ser de 4 dígitos")
        private String pinCertificado;
        
        private String notaFactura;
        private String notaValidezProforma = "Válida por 15 días";
        private String detalleFactura1;
        private String detalleFactura2;
        
        @NotEmpty(message = "Debe seleccionar al menos una actividad económica")
        private List<ActividadEconomicaRequest> actividades;
    }
}
