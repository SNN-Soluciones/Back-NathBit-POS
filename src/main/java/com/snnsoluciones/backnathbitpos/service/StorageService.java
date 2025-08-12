package com.snnsoluciones.backnathbitpos.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Servicio para manejo de archivos en Digital Ocean Spaces (S3 compatible)
 */
public interface StorageService {
    
    /**
     * Sube un archivo desde un InputStream
     * 
     * @param inputStream Stream del archivo
     * @param key Ruta/nombre en S3
     * @param contentType Tipo MIME del archivo
     * @param size Tamaño en bytes
     * @return URL del archivo subido
     */
    String uploadFile(InputStream inputStream, String key, String contentType, long size);
    
    /**
     * Sube un archivo desde MultipartFile
     * 
     * @param file Archivo a subir
     * @param key Ruta/nombre en S3
     * @return URL del archivo subido
     */
    String uploadFile(MultipartFile file, String key);
    
    /**
     * Descarga un archivo
     * 
     * @param key Ruta/nombre en S3
     * @return InputStream del archivo
     */
    InputStream downloadFile(String key);
    
    /**
     * Genera una URL temporal firmada para acceso directo
     * 
     * @param key Ruta/nombre en S3
     * @param expirationMinutes Minutos de validez
     * @return URL firmada temporal
     */
    String generateSignedUrl(String key, int expirationMinutes);
    
    /**
     * Elimina un archivo
     * 
     * @param key Ruta/nombre en S3
     */
    void deleteFile(String key);
    
    /**
     * Verifica si un archivo existe
     * 
     * @param key Ruta/nombre en S3
     * @return true si existe
     */
    boolean fileExists(String key);
}