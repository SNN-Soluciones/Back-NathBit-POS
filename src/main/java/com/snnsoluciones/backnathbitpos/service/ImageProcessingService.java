
package com.snnsoluciones.backnathbitpos.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface ImageProcessingService {
    
    /**
     * Genera un thumbnail de una imagen
     * @param imagen Imagen original
     * @param width Ancho del thumbnail
     * @param height Alto del thumbnail
     * @return Bytes del thumbnail generado
     * @throws IOException Si hay error al procesar la imagen
     */
    byte[] generarThumbnail(MultipartFile imagen, int width, int height) throws IOException;
    
    /**
     * Genera un thumbnail cuadrado (150x150) por defecto
     * @param imagen Imagen original
     * @return Bytes del thumbnail generado
     * @throws IOException Si hay error al procesar la imagen
     */
    byte[] generarThumbnail(MultipartFile imagen) throws IOException;
}