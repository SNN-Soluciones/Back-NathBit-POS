package com.snnsoluciones.backnathbitpos.enums.facturacion;

/**
 * Pasos del proceso de facturación electrónica
 */
public enum PasoFacturacion {
    GENERAR_XML(1, "Generando XML del documento"),
    FIRMAR_DOCUMENTO(2, "Firmando documento digitalmente"),
    ENVIAR_HACIENDA(3, "Enviando a Hacienda"),
    PROCESAR_RESPUESTA(4, "Procesando respuesta de Hacienda"),
    GENERAR_PDF(5, "Generando PDF"),
    SUBIR_DOCUMENTOS(6, "Subiendo documentos a almacenamiento"),
    ENVIAR_EMAIL(7, "Enviando email al cliente"),
    COMPLETADO(8, "Proceso completado");

    private final int orden;
    private final String descripcion;

    PasoFacturacion(int orden, String descripcion) {
        this.orden = orden;
        this.descripcion = descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Obtiene el siguiente paso en el proceso
     */
    public PasoFacturacion siguiente() {
        PasoFacturacion[] pasos = values();
        for (int i = 0; i < pasos.length - 1; i++) {
            if (pasos[i] == this) {
                return pasos[i + 1];
            }
        }
        return COMPLETADO;
    }

    /**
     * Indica si es un paso crítico que requiere reintentos
     */
    public boolean esCritico() {
        return this == FIRMAR_DOCUMENTO || this == ENVIAR_HACIENDA;
    }
}