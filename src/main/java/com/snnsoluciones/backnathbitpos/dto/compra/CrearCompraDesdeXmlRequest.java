package com.snnsoluciones.backnathbitpos.dto.compra;

import lombok.Data;

// DTO para crear compra desde XML
@Data
public class CrearCompraDesdeXmlRequest {
    private Long proveedorId;
    private String xmlContent; // Base64 o XML plano
    private String observaciones;
    private Boolean procesarInventario = true; // Si actualiza inventario automáticamente
    private Boolean crearProductosSiNoExisten = false; // Si crea productos nuevos
}
