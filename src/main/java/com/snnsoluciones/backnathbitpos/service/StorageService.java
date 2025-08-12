package com.snnsoluciones.backnathbitpos.service;

import java.time.Duration;
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

    /**
     * Sube un archivo a S3 con configuración específica
     * @param file archivo a subir
     * @param key ruta completa en S3
     * @param contentType tipo de contenido
     * @param isPrivate si el archivo debe ser privado
     * @return URL del archivo
     */
    String subirArchivo(MultipartFile file, String key, String contentType, boolean isPrivate);

    /**
     * Sube un archivo desde bytes
     * @param data datos del archivo
     * @param key ruta completa en S3
     * @param contentType tipo de contenido
     * @param isPrivate si el archivo debe ser privado
     * @return URL del archivo
     */
    String subirArchivo(byte[] data, String key, String contentType, boolean isPrivate);

    /**
     * Genera una URL pre-firmada para acceso temporal
     * @param key ruta del archivo en S3
     * @param duracion duración de validez de la URL
     * @return URL pre-firmada
     */
    String generarUrlPreFirmada(String key, Duration duracion);

    /**
     * Elimina un archivo de S3
     * @param key ruta del archivo a eliminar
     * @return true si se eliminó correctamente
     */
    boolean eliminarArchivo(String key);

    /**
     * Verifica si un archivo existe en S3
     * @param key ruta del archivo
     * @return true si existe
     */
    boolean existeArchivo(String key);

    /**
     * Obtiene el contenido de un archivo
     * @param key ruta del archivo
     * @return contenido del archivo
     */
    byte[] obtenerArchivo(String key);
}