package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.entity.Producto;
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
 * Maneja subida, eliminación y validación de imágenes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoImagenHandler {

    private final StorageService storageService;
    private final ProductoRepository productoRepository;

    @Value("${app.storage.max-file-size:5242880}") // 5MB por defecto
    private long maxFileSize;

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".webp"
    );

    private static final List<String> CONTENT_TYPES_PERMITIDOS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /**
     * Sube una imagen para un producto
     */
    public void subirImagen(Producto producto, MultipartFile imagen) {
        log.debug("Subiendo imagen para producto ID: {}", producto.getId());

        // Validar imagen
        validarImagen(imagen);

        try {
            // Construir ruta
            String carpeta = construirRuta(producto);
            String nombreArchivo = producto.getCodigoInterno();

            // Subir a storage
            String urlImagen = storageService.subirArchivo(imagen, carpeta, nombreArchivo, false);

            // Construir key
            String extension = obtenerExtension(imagen.getOriginalFilename());
            String imagenKey = carpeta + "/" + nombreArchivo + extension;

            // Actualizar producto
            producto.setImagenUrl(urlImagen);
            producto.setImagenKey(imagenKey);
            productoRepository.save(producto);

            log.info("Imagen subida exitosamente para producto ID: {}", producto.getId());

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

        // Si tiene imagen anterior, eliminarla
        if (producto.getImagenKey() != null) {
            try {
                eliminarImagenDeStorage(producto.getImagenKey());
            } catch (Exception e) {
                log.warn("No se pudo eliminar imagen anterior: {}", e.getMessage());
                // Continuar con la subida de la nueva imagen
            }
        }

        // Subir nueva imagen
        subirImagen(producto, nuevaImagen);
    }

    /**
     * Elimina la imagen de un producto
     */
    public void eliminarImagen(Producto producto) {
        log.debug("Eliminando imagen de producto ID: {}", producto.getId());

        if (producto.getImagenKey() == null) {
            log.warn("El producto no tiene imagen asociada");
            return;
        }

        try {
            // Eliminar de storage
            eliminarImagenDeStorage(producto.getImagenKey());

            // Limpiar campos en producto
            producto.setImagenUrl(null);
            producto.setImagenKey(null);
            productoRepository.save(producto);

            log.info("Imagen eliminada exitosamente para producto ID: {}", producto.getId());

        } catch (Exception e) {
            log.error("Error al eliminar imagen de producto ID: {}", producto.getId(), e);
            throw new BusinessException("Error al eliminar la imagen: " + e.getMessage());
        }
    }

    /**
     * Valida que la imagen cumple con los requisitos
     */
    private void validarImagen(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new BusinessException("La imagen está vacía");
        }

        // Validar tamaño
        if (imagen.getSize() > maxFileSize) {
            throw new BusinessException(
                String.format("La imagen es muy grande. Tamaño máximo: %d MB", 
                    maxFileSize / (1024 * 1024))
            );
        }

        // Validar content type
        String contentType = imagen.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new BusinessException(
                "Tipo de archivo no permitido. Use: " + String.join(", ", CONTENT_TYPES_PERMITIDOS)
            );
        }

        // Validar extensión
        String nombreArchivo = imagen.getOriginalFilename();
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            throw new BusinessException("El nombre del archivo es inválido");
        }

        String extension = obtenerExtension(nombreArchivo);
        if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase())) {
            throw new BusinessException(
                "Extensión no permitida. Use: " + String.join(", ", EXTENSIONES_PERMITIDAS)
            );
        }

        log.debug("Imagen validada correctamente: {} - {} bytes", nombreArchivo, imagen.getSize());
    }

    /**
     * Construye la ruta de almacenamiento para la imagen
     */
    private String construirRuta(Producto producto) {
        String nombreComercial;

        if (producto.getSucursal() != null) {
            nombreComercial = producto.getSucursal().getNombre();
        } else {
            nombreComercial = producto.getEmpresa().getNombreComercial() != null
                ? producto.getEmpresa().getNombreComercial()
                : producto.getEmpresa().getNombreRazonSocial();
        }

        String nombreLimpio = limpiarNombreParaRuta(nombreComercial);
        return String.format("NathBit-POS/%s/productos", nombreLimpio);
    }

    /**
     * Limpia un nombre para usarlo en rutas de archivos
     */
    private String limpiarNombreParaRuta(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return "sin_nombre";
        }

        return nombre
            .trim()
            .replaceAll("[^a-zA-Z0-9-_]", "_")
            .replaceAll("_{2,}", "_")
            .toLowerCase();
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            return "";
        }

        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot == -1 || lastDot == nombreArchivo.length() - 1) {
            return "";
        }

        return nombreArchivo.substring(lastDot).toLowerCase();
    }

    /**
     * Elimina una imagen del storage usando su key
     */
    private void eliminarImagenDeStorage(String imagenKey) {
        try {
            storageService.eliminarArchivo(imagenKey);
            log.debug("Imagen eliminada del storage: {}", imagenKey);
        } catch (Exception e) {
            log.error("Error al eliminar imagen del storage: {}", imagenKey, e);
            throw new BusinessException("No se pudo eliminar la imagen del almacenamiento");
        }
    }
}