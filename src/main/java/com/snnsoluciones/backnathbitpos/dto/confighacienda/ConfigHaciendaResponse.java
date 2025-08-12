package com.snnsoluciones.backnathbitpos.dto.confighacienda;

import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoAutenticacionHacienda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigHaciendaResponse {

    private Long id;
    private AmbienteHacienda ambiente;
    private TipoAutenticacionHacienda tipoAutenticacion;
    private String usuarioHacienda;

    // Nunca devolvemos la clave por seguridad
    private boolean tieneClaveConfigurada;
    private boolean tieneCertificadoConfigurado;

    private String proveedorSistemas;

    // Certificado info (si existe)
    private LocalDate fechaEmisionCertificado;
    private LocalDate fechaVencimientoCertificado;

    // Info de la empresa
    private Long empresaId;
    private String empresaNombre;
    private String empresaIdentificacion;

    // Estado de la configuración
    private boolean configuracionCompleta;
    private String mensajeEstado;

    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper para verificar si está lista para producción
    public boolean listaParaProduccion() {
        return configuracionCompleta &&
            ambiente == AmbienteHacienda.PRODUCCION &&
            tieneClaveConfigurada;
    }
}