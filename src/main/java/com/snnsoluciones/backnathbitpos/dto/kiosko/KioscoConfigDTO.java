package com.snnsoluciones.backnathbitpos.dto.kiosko;
 
import lombok.*;
 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KioscoConfigDTO {
 
    private Long sucursalId;
    private String templateId;
 
    // 9 Design Tokens — nombres exactos que usa el frontend
    private String colorPrimary;
    private String colorSecondary;
    private String colorBackground;
    private String colorSurface;
    private String colorTextPrimary;
    private String colorTextSecondary;
    private String colorAccent;
    private String colorSuccess;
    private String colorDanger;
 
    // Assets
    private String logoUrl;
    private String imagenBienvenidaUrl;
    private String textoBienvenida;
 
    // Comportamiento
    private Integer tiempoInactividad;
    private Boolean mostrarPrecios;
    private Boolean requierePagoEnCaja;
 
    // Mapper estático
    public static KioscoConfigDTO from(com.snnsoluciones.backnathbitpos.entity.KioscoConfig e) {
        return KioscoConfigDTO.builder()
            .sucursalId(e.getSucursalId())
            .templateId(e.getTemplateId())
            .colorPrimary(e.getColorPrimary())
            .colorSecondary(e.getColorSecondary())
            .colorBackground(e.getColorBackground())
            .colorSurface(e.getColorSurface())
            .colorTextPrimary(e.getColorTextPrimary())
            .colorTextSecondary(e.getColorTextSecondary())
            .colorAccent(e.getColorAccent())
            .colorSuccess(e.getColorSuccess())
            .colorDanger(e.getColorDanger())
            .logoUrl(e.getLogoUrl())
            .imagenBienvenidaUrl(e.getImagenBienvenidaUrl())
            .textoBienvenida(e.getTextoBienvenida())
            .tiempoInactividad(e.getTiempoInactividad())
            .mostrarPrecios(e.getMostrarPrecios())
            .requierePagoEnCaja(e.getRequierePagoEnCaja())
            .build();
    }
}