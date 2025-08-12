package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.FacturaJob;
import java.io.IOException;

/**
 * Procesador asíncrono de facturas electrónicas
 * Ejecuta los pasos: XML, firma, envío, PDF, email
 */
public interface FacturaAsyncProcessor {
    
    /**
     * Procesa todos los jobs pendientes
     * Se ejecuta periódicamente vía @Scheduled
     */
    void procesarJobsPendientes();
    
    /**
     * Procesa un job específico ejecutando todos los pasos
     * 
     * @param job Job a procesar
     */
    void procesarFactura(FacturaJob job);
    
    /**
     * Genera el XML de la factura
     * 
     * @param facturaId ID de la factura
     * @return Path del XML generado
     */
    String generarXML(Long facturaId);
    
    /**
     * Firma digitalmente el XML
     * 
     * @param xmlPath Path del XML sin firmar
     * @return Path del XML firmado
     */
    String firmarXML(String xmlPath) throws IOException;
    
    /**
     * Envía el XML firmado a Hacienda
     * 
     * @param xmlFirmadoPath Path del XML firmado
     * @param clave Clave del documento
     * @return Respuesta de Hacienda
     */
    String enviarHacienda(String xmlFirmadoPath, String clave);
    
    /**
     * Genera el PDF de la factura
     * 
     * @param facturaId ID de la factura
     * @return Path del PDF generado
     */
    String generarPDF(Long facturaId);
    
    /**
     * Envía email al cliente con la factura
     * 
     * @param facturaId ID de la factura
     * @param pdfPath Path del PDF
     * @param xmlPath Path del XML
     */
    void enviarEmail(Long facturaId, String pdfPath, String xmlPath);
}