package com.snnsoluciones.backnathbitpos.dto.facturarecepcion;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class SubirXmlRequest {
    private Long empresaId;
    private Long sucursalId;
    private MultipartFile xmlFile;
    private MultipartFile pdfFile;

    // NUEVO: Siempre crear proveedor si no existe
    @Builder.Default
    private boolean crearProveedorSiNoExiste = true;

    // NUEVO: Siempre aceptar automáticamente
    @Builder.Default
    private boolean aceptarAutomaticamente = true;
}