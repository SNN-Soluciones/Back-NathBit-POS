package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Handler encargado de gestionar las imágenes de productos.
 * Maneja subida, eliminación y validación de imágenes en DigitalOcean Spaces.
 * 
 * ESTRUCTURA DE CARPETAS:
 * - Productos GLOBALES: NathBit-POS/{empresaId}/productos/globales/{codigoInterno}.jpg
 * - Productos LOCALES:  NathBit-POS/{empresaId}/productos/sucursal_{sucursalId}/{codigoInterno}.jpg
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoImagenHandler {

    private final StorageService storageService;
    private final ProductoRepository productoRepository;

    @Value("${app.storage.max-file-size:5242880}") // 5MB por defecto
    private long maxFileSize;

    @Value("${storage.spaces.base-folder:NathBit-POS}")
    private String baseFolder;

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".webp"
    );

    private static final List<String> CONTENT_TYPES_PERMITIDOS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    // ==================== MÉTODOS PÚBLICOS ====================

    /**
     * Sube una imagen para un producto nuevo
     */
    public void subirImagen(Producto producto, MultipartFile imagen) {
        log.debug("Subiendo imagen para producto ID: {}, código: {}", 
            producto.getId(), producto.getCodigoInterno());

        // Validar imagen
        validarImagen(imagen);

        try {
            // Construir la carpeta según si es global o local
            String carpeta = construirCarpeta(producto);
            
            // Construir nombre del archivo (código interno + extensión)
            String extension = obtenerExtension(imagen.getOriginalFilename());
            String nombreArchivo = producto.getCodigoInterno() + extension;
            
            // Construir key completa (ruta en S3)
            String key = carpeta + "/" + nombreArchivo;
            
            log.debug("Subiendo imagen a: {}", key);

            // Subir a DigitalOcean Spaces (PÚBLICO)
            String urlImagen = storageService.subirArchivo(
                imagen, 
                carpeta, 
                producto.getCodigoInterno(), 
                false  // isPrivate = false → imagen PÚBLICA
            );

            // Actualizar producto con URL y key
            producto.setImagenUrl(urlImagen);
            producto.setImagenKey(key);
            productoRepository.save(producto);

            log.info("Imagen subida exitosamente para producto ID: {} - URL: {}", 
                producto.getId(), urlImagen);

        } catch (Exception e) {
            log.error("Error al subir imagen para producto ID: {}", producto.getId(), e);
            throw new BusinessException("Error al subir la imagen: " + e.getMessage());
        }
    }

    /**
     * Actualiza la imagen de un producto (elimina la anterior y sube la nueva)
     */
    public void actualizarImagen(Producto producto, MultipartFile nuevaImagen) {
        log.debug("Actualizando imagen para producto ID: {}", producto.getId());

        // Validar nueva imagen
        validarImagen(nuevaImagen);

        // Si tiene imagen anterior, eliminarla de S3
        if (producto.getImagenKey() != null && !producto.getImagenKey().isEmpty()) {
            try {
                log.debug("Eliminando imagen anterior: {}", producto.getImagenKey());
                storageService.eliminarArchivo(producto.getImagenKey());
            } catch (Exception e) {
                log.warn("No se pudo eliminar imagen anterior: {} - Continuando...", e.getMessage());
                // No fallar la actualización si no se pudo borrar la anterior
            }
        }

        // Subir nueva imagen (reutiliza el método subirImagen)
        subirImagen(producto, nuevaImagen);
    }

    /**
     * Elimina la imagen de un producto
     */
    public void eliminarImagen(Producto producto) {
        log.debug("Eliminando imagen de producto ID: {}", producto.getId());

        if (producto.getImagenKey() == null || producto.getImagenKey().isEmpty()) {
            log.warn("El producto no tiene imagen asociada");
            return;
        }

        try {
            // Eliminar de DigitalOcean Spaces
            storageService.eliminarArchivo(producto.getImagenKey());

            // Limpiar campos en producto
            producto.setImagenUrl(null);
            producto.setImagenKey(null);
            producto.setThumbnailUrl(null); // Por si en el futuro tenemos thumbnails
            productoRepository.save(producto);

            log.info("Imagen eliminada exitosamente para producto ID: {}", producto.getId());

        } catch (Exception e) {
            log.error("Error al eliminar imagen de producto ID: {}", producto.getId(), e);
            throw new BusinessException("Error al eliminar la imagen: " + e.getMessage());
        }
    }

    // ==================== MÉTODOS PRIVADOS DE VALIDACIÓN ====================

    /**
     * Valida que la imagen cumple con los requisitos
     */
    private void validarImagen(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new BadRequestException("La imagen está vacía o es nula");
        }

        // Validar tamaño
        if (imagen.getSize() > maxFileSize) {
            long maxSizeMB = maxFileSize / (1024 * 1024);
            throw new BadRequestException(
                String.format("La imagen es muy grande. Tamaño máximo: %d MB", maxSizeMB)
            );
        }

        // Validar content type
        String contentType = imagen.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                "Tipo de archivo no permitido. Solo se permiten: JPG, PNG, WEBP"
            );
        }

        // Validar extensión
        String nombreArchivo = imagen.getOriginalFilename();
        if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
            throw new BadRequestException("El archivo debe tener un nombre");
        }

        String extension = obtenerExtension(nombreArchivo).toLowerCase();
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BadRequestException(
                "Extensión de archivo no permitida. Solo se permiten: " +
                String.join(", ", EXTENSIONES_PERMITIDAS)
            );
        }

        log.debug("Imagen validada correctamente: {} - {} bytes", 
            nombreArchivo, imagen.getSize());
    }

    // ==================== MÉTODOS PRIVADOS DE CONSTRUCCIÓN ====================

    /**
     * Construye la carpeta en S3 según si el producto es global o local
     * 
     * EJEMPLOS:
     * - Producto GLOBAL (sucursalId = null):
     *   NathBit-POS/5/productos/globales
     * 
     * - Producto LOCAL (sucursalId = 12):
     *   NathBit-POS/5/productos/sucursal_12
     */
    private String construirCarpeta(Producto producto) {
        Long empresaId = producto.getEmpresa().getId();
        
        // Construir base: NathBit-POS/{empresaId}/productos
        String carpetaBase = String.format("%s/%d/productos", baseFolder, empresaId);

        // Agregar subcarpeta según si es global o local
        if (producto.getSucursal() == null) {
            // Producto GLOBAL
            return carpetaBase + "/globales";
        } else {
            // Producto LOCAL de una sucursal
            Long sucursalId = producto.getSucursal().getId();
            return carpetaBase + "/sucursal_" + sucursalId;
        }
    }

    /**
     * Extrae la extensión del nombre de archivo
     * 
     * @param nombreArchivo Nombre del archivo (ej: "imagen.jpg")
     * @return Extensión con punto (ej: ".jpg")
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return ".jpg"; // Extensión por defecto
        }
        
        int lastDot = nombreArchivo.lastIndexOf(".");
        return nombreArchivo.substring(lastDot).toLowerCase();
    }

    /**
     * Limpia un nombre para usarlo en rutas (remueve caracteres especiales)
     * 
     * @param nombre Nombre a limpiar
     * @return Nombre limpio (solo letras, números, guiones y guiones bajos)
     */
    private String limpiarNombreParaRuta(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "sin_nombre";
        }

        return nombre.trim()
            .replaceAll("\\s+", "_")           // Espacios → guion bajo
            .replaceAll("[^a-zA-Z0-9_-]", "")  // Solo alfanuméricos, _ y -
            .toLowerCase();                     // Minúsculas
    }
}