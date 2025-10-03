package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.service.ImageProcessingService;
import com.snnsoluciones.backnathbitpos.service.ProductoImagenService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoImagenServiceImpl implements ProductoImagenService {

    private final StorageService storageService;
    private final ProductoRepository productoRepository;
    private final ImageProcessingService imageProcessingService;

    @Value("${app.storage.max-file-size:5242880}") // 5MB por defecto
    private long maxFileSize;

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".webp"
    );

    private static final List<String> CONTENT_TYPES_PERMITIDOS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    @Override
    @Transactional
    public String subirImagen(Long empresaId, String nombreComercial, String codigoInterno, MultipartFile imagen) {
        try {
            // Validar imagen
            validarImagen(imagen);

            // Limpiar nombre comercial para usar en la ruta
            String nombreComercialLimpio = limpiarNombreParaRuta(nombreComercial);

            // Obtener extensión del archivo
            String nombreOriginal = imagen.getOriginalFilename();
            String extension = obtenerExtension(nombreOriginal);

            // Construir las rutas
            String carpeta = String.format("NathBit-POS/%s/productos", nombreComercialLimpio);
            String nombreArchivoOriginal = codigoInterno;
            String nombreArchivoThumbnail = codigoInterno + "_thumb";

            // Subir imagen original
            String urlImagenOriginal = storageService.subirArchivo(imagen, carpeta, nombreArchivoOriginal, false);

            // Generar y subir thumbnail
            String urlThumbnail = null;
            try {
                byte[] thumbnailBytes = imageProcessingService.generarThumbnail(imagen);
                String keyThumbnail = carpeta + "/" + nombreArchivoThumbnail + ".jpg";
                urlThumbnail = storageService.subirArchivo(
                    thumbnailBytes,
                    keyThumbnail,
                    "image/jpeg",
                    false
                );
                log.info("Thumbnail generado y subido para producto {}: {}", codigoInterno, urlThumbnail);
            } catch (Exception e) {
                log.error("Error generando thumbnail para producto {}: {}", codigoInterno, e.getMessage());
                // No lanzar excepción, continuar sin thumbnail
            }

            // Actualizar producto con las URLs
            actualizarUrlsEnProducto(empresaId, codigoInterno, urlImagenOriginal, urlThumbnail);

            log.info("Imagen subida exitosamente para producto {}: {}", codigoInterno, urlImagenOriginal);

            return urlImagenOriginal;

        } catch (Exception e) {
            log.error("Error al subir imagen del producto {}: {}", codigoInterno, e.getMessage());
            throw new RuntimeException("Error al subir imagen: " + e.getMessage(), e);
        }
    }

    private void actualizarUrlsEnProducto(Long empresaId, String codigoInterno,
        String urlOriginal, String urlThumbnail) {
        // Buscar el producto y actualizar las URLs
        Producto producto = productoRepository.findByEmpresaIdAndCodigoInterno(empresaId, codigoInterno)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + codigoInterno));

        // Extraer keys de las URLs
        String keyOriginal = extraerKeyDeUrl(urlOriginal);
        String keyThumbnail = urlThumbnail != null ? extraerKeyDeUrl(urlThumbnail) : null;

        // IMPORTANTE: Solo actualizar si no hay imagen previa o si estamos actualizando
        producto.setImagenUrl(urlOriginal);
        producto.setImagenKey(keyOriginal);
        producto.setThumbnailUrl(urlThumbnail);
        producto.setThumbnailKey(keyThumbnail);

        productoRepository.save(producto);

        log.info("URLs actualizadas para producto {}: original={}, thumbnail={}",
            codigoInterno, urlOriginal, urlThumbnail);
    }

    @Override
    @Transactional
    public void eliminarImagen(Long empresaId, Long productoId) {
        Producto producto = productoRepository.findById(productoId)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Verificar que el producto pertenece a la empresa
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new BadRequestException("El producto no pertenece a la empresa especificada");
        }

        // Eliminar imagen original
        if (producto.getImagenKey() != null) {
            try {
                storageService.eliminarArchivo(producto.getImagenKey());
                log.info("Imagen eliminada de S3: {}", producto.getImagenKey());
            } catch (Exception e) {
                log.error("Error al eliminar imagen de S3: {}", e.getMessage());
            }
        }

        // Eliminar thumbnail
        if (producto.getThumbnailKey() != null) {
            try {
                storageService.eliminarArchivo(producto.getThumbnailKey());
                log.info("Thumbnail eliminado de S3: {}", producto.getThumbnailKey());
            } catch (Exception e) {
                log.error("Error al eliminar thumbnail de S3: {}", e.getMessage());
            }
        }

        // Limpiar campos en la BD
        producto.setImagenUrl(null);
        producto.setImagenKey(null);
        producto.setThumbnailUrl(null);
        producto.setThumbnailKey(null);
        productoRepository.save(producto);
    }

    @Override
    public void validarImagen(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new BadRequestException("La imagen no puede estar vacía");
        }

        // Validar tamaño
        if (imagen.getSize() > maxFileSize) {
            throw new BadRequestException(String.format(
                "El archivo excede el tamaño máximo permitido de %d MB",
                maxFileSize / 1024 / 1024
            ));
        }

        // Validar tipo de contenido
        String contentType = imagen.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Tipo de archivo no permitido. Solo se permiten: JPG, PNG, WEBP");
        }

        // Validar extensión
        String nombreArchivo = imagen.getOriginalFilename();
        if (nombreArchivo == null) {
            throw new BadRequestException("El archivo debe tener un nombre");
        }

        String extension = obtenerExtension(nombreArchivo).toLowerCase();
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BadRequestException("Extensión de archivo no permitida. Solo se permiten: " +
                String.join(", ", EXTENSIONES_PERMITIDAS));
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf("."));
    }

    private String limpiarNombreParaRuta(String nombre) {
        if (nombre == null) {
            return "sin_nombre";
        }

        return nombre.trim()
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-zA-Z0-9_-]", "")
            .toUpperCase();
    }

    private String extraerKeyDeUrl(String url) {
        if (url == null) return null;

        // La URL tiene formato: https://snn-soluciones.nyc3.digitaloceanspaces.com/key
        String base = "digitaloceanspaces.com/";
        int index = url.indexOf(base);
        if (index != -1) {
            return url.substring(index + base.length());
        }
        return url;
    }
}