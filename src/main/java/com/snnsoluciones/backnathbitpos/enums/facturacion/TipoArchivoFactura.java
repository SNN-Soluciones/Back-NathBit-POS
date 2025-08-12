package com.snnsoluciones.backnathbitpos.enums.facturacion;

/**
 * Tipos de archivos generados en el proceso de facturación
 */
public enum TipoArchivoFactura {
    XML_UNSIGNED("xml", "XML sin firmar", false),
    XML_SIGNED("xml", "XML firmado digitalmente", true),
    XML_RESPUESTA("xml", "Respuesta de Hacienda", true),
    PDF_FACTURA("pdf", "PDF de factura electrónica", true),
    PDF_TIQUETE("pdf", "PDF de tiquete electrónico", true),
    PDF_NOTA_CREDITO("pdf", "PDF de nota de crédito", true),
    PDF_NOTA_DEBITO("pdf", "PDF de nota de débito", true);

    private final String extension;
    private final String descripcion;
    private final boolean publico;

    TipoArchivoFactura(String extension, String descripcion, boolean publico) {
        this.extension = extension;
        this.descripcion = descripcion;
        this.publico = publico;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isPublico() {
        return publico;
    }

    /**
     * Genera el nombre del archivo con el formato: clave_TIPO.extension
     */
    public String generarNombreArchivo(String clave) {
        return String.format("%s_%s.%s", clave, this.name(), extension);
    }

    /**
     * Obtiene el content-type para el archivo
     */
    public String getContentType() {
        switch (this.extension) {
            case "xml":
                return "application/xml";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }
}