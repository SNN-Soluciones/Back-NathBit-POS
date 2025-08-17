package com.snnsoluciones.backnathbitpos.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProductoImagenService {
    
    /**
     * Sube una imagen de producto a S3
     * @param empresaId ID de la empresa
     * @param nombreComercial Nombre comercial de la empresa para la carpeta
     * @param codigoInterno Código interno del producto
     * @param imagen Archivo de imagen
     * @return URL de la imagen subida
     */
    String subirImagen(Long empresaId, String nombreComercial, String codigoInterno, MultipartFile imagen);
    
    /**
     * Elimina la imagen de un producto
     * @param empresaId ID de la empresa
     * @param productoId ID del producto
     */
    void eliminarImagen(Long empresaId, Long productoId);
    
    /**
     * Valida que el archivo sea una imagen válida
     * @param imagen Archivo a validar
     * @throws IllegalArgumentException si no es válida
     */
    void validarImagen(MultipartFile imagen);
}