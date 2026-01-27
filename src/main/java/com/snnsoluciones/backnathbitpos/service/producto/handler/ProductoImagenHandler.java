package com.snnsoluciones.backnathbitpos.service.producto.handler;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import com.snnsoluciones.backnathbitpos.service.ImageProcessingService;
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
 * ESTRUCTURA DE CARPETAS V3 (Soporta GLOBALES y LOCALES):
 *
 * ESTRATEGIA:
 * - Producto GLOBAL (sucursalId = NULL) → Carpeta por empresa
 *   Carpeta: NathBit-POS/{nombreComercialEmpresa}/productos/{codigo}.jpg
 *   Ejemplo: NathBit-POS/TACO_BELL/productos/BURRITO-001.jpg
 *
 * - Producto LOCAL (sucursalId != NULL) → Carpeta por sucursal
 *   Carpeta: NathBit-POS/{nombreSucursal}/productos/{codigo}.jpg
 *   Ejemplo: NathBit-POS/VIAJE_AL_SABOR_ESCAZU/productos/GALLO-PINTO.jpg
 *
 * THUMBNAILS:
 * - Original: {codigo}.jpg
 * - Thumbnail: {codigo}_thumb.jpg (150x150px)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoImagenHandler {

    private final StorageService storageService;
    private final ProductoRepository productoRepository;
    private final ImageProcessingService imageProcessingService;

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
     * Sube una imagen para un producto nuevo (con thumbnail automático)
     */
    public void subirImagen(Producto producto, MultipartFile imagen) {
        log.debug("Subiendo imagen para producto ID: {}, código: {}",
            producto.getId(), producto.getCodigoInterno());

        // Validar imagen
        validarImagen(imagen);

        try {
            // Construir la carpeta según estrategia de negocio
            String carpeta = construirCarpeta(producto);

            // Construir nombres de archivos SIN extensión (storageService la agrega)
            String nombreArchivoOriginal = producto.getCodigoInterno();
            String nombreArchivoThumbnail = producto.getCodigoInterno() + "_thumb";

            // Obtener extensión para construir las keys después
            String extension = obtenerExtension(imagen.getOriginalFilename());

            log.debug("📁 Carpeta destino: {}", carpeta);
            log.debug("🖼️ Nombre archivo: {}", nombreArchivoOriginal);

            // 1️⃣ GENERAR THUMBNAIL PRIMERO (antes de consumir el InputStream)
            byte[] thumbnailBytes = null;
            try {
                thumbnailBytes = imageProcessingService.generarThumbnail(imagen, 150, 150);
                log.debug("✅ Thumbnail generado en memoria ({} bytes)", thumbnailBytes.length);
            } catch (Exception e) {
                log.error("⚠️ Error generando thumbnail: {}", e.getMessage(), e);
                // Continuar sin thumbnail
            }

            // 2️⃣ SUBIR IMAGEN ORIGINAL (PÚBLICA)
            // Firma antigua: subirArchivo(MultipartFile, carpeta, nombreSinExtension, privado)
            String urlOriginal = storageService.subirArchivo(
                imagen,
                carpeta,
                nombreArchivoOriginal,
                false  // isPrivate = false → imagen PÚBLICA
            );

            // Construir key completa para guardar en BD
            String keyOriginal = carpeta + "/" + nombreArchivoOriginal + extension;

            // 3️⃣ SUBIR THUMBNAIL (si se generó exitosamente)
            String urlThumbnail = null;
            String keyThumbnail = null;

            if (thumbnailBytes != null && thumbnailBytes.length > 0) {
                try {
                    // Construir key completa para thumbnail
                    keyThumbnail = carpeta + "/" + nombreArchivoThumbnail + ".jpg";

                    log.debug("🔳 Subiendo thumbnail a: {}", keyThumbnail);

                    // Subir thumbnail usando firma con bytes
                    urlThumbnail = storageService.subirArchivo(
                        thumbnailBytes,
                        keyThumbnail,
                        "image/jpeg",
                        false
                    );
                    log.info("✅ Thumbnail subido: {}", keyThumbnail);
                } catch (Exception e) {
                    log.error("❌ Error subiendo thumbnail a S3: {}", e.getMessage(), e);
                    // No fallar la subida principal
                }
            } else {
                log.warn("⚠️ No se generó thumbnail (continuando sin él)");
            }

            // 4️⃣ ACTUALIZAR PRODUCTO CON URLs Y KEYS
            producto.setImagenUrl(urlOriginal);
            producto.setImagenKey(keyOriginal);
            producto.setThumbnailUrl(urlThumbnail);
            producto.setThumbnailKey(keyThumbnail);
            productoRepository.save(producto);

            log.info("✅ Imagen subida exitosamente para producto ID: {} - URL: {}",
                producto.getId(), urlOriginal);

        } catch (Exception e) {
            log.error("❌ Error al subir imagen para producto ID: {}", producto.getId(), e);
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

        // Eliminar imágenes anteriores (original + thumbnail)
        eliminarImagenesAnteriores(producto);

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

        eliminarImagenesAnteriores(producto);

        // Limpiar campos en producto
        producto.setImagenUrl(null);
        producto.setImagenKey(null);
        producto.setThumbnailUrl(null);
        producto.setThumbnailKey(null);
        productoRepository.save(producto);

        log.info("✅ Imagen eliminada exitosamente para producto ID: {}", producto.getId());
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * ✅ CORREGIDO: Construye la carpeta donde se guardará la imagen.
     *
     * NUEVA ESTRATEGIA (soporta productos GLOBALES y LOCALES):
     *
     * 1. Si producto.sucursal == NULL → Producto GLOBAL
     *    → Usar nombreComercial de EMPRESA
     *
     * 2. Si producto.sucursal != NULL → Producto LOCAL
     *    → Usar nombre de SUCURSAL
     *
     * @param producto Producto con empresa/sucursal cargados
     * @return Ruta de carpeta sin trailing slash (ej: "NathBit-POS/TACO_BELL/productos")
     */
    private String construirCarpeta(Producto producto) {
        Empresa empresa = producto.getEmpresa();
        Sucursal sucursal = producto.getSucursal();

        String nombreCarpeta;

        if (sucursal == null) {
            // ✅ PRODUCTO GLOBAL → Usar nombre comercial de EMPRESA
            nombreCarpeta = limpiarNombreParaRuta(empresa.getNombreComercial());
            log.debug("📦 Producto GLOBAL - Usando nombre comercial empresa: {}", nombreCarpeta);

        } else {
            // ✅ PRODUCTO LOCAL → Usar nombre de SUCURSAL
            nombreCarpeta = limpiarNombreParaRuta(sucursal.getNombre());
            log.debug("📦 Producto LOCAL - Usando nombre sucursal: {}", nombreCarpeta);
        }

        // Construir ruta completa: NathBit-POS/{nombreCarpeta}/productos
        return baseFolder + "/" + nombreCarpeta + "/productos";
    }

    /**
     * Elimina imágenes anteriores (original + thumbnail) de S3
     */
    private void eliminarImagenesAnteriores(Producto producto) {
        // Eliminar imagen original
        if (producto.getImagenKey() != null && !producto.getImagenKey().isEmpty()) {
            try {
                log.debug("🗑️ Eliminando imagen original: {}", producto.getImagenKey());
                storageService.eliminarArchivo(producto.getImagenKey());
            } catch (Exception e) {
                log.warn("⚠️ No se pudo eliminar imagen anterior: {} - Continuando...", e.getMessage());
            }
        }

        // Eliminar thumbnail
        if (producto.getThumbnailKey() != null && !producto.getThumbnailKey().isEmpty()) {
            try {
                log.debug("🗑️ Eliminando thumbnail: {}", producto.getThumbnailKey());
                storageService.eliminarArchivo(producto.getThumbnailKey());
            } catch (Exception e) {
                log.warn("⚠️ No se pudo eliminar thumbnail anterior: {} - Continuando...", e.getMessage());
            }
        }
    }

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
    }

    /**
     * Obtiene la extensión del archivo (incluyendo el punto)
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return ".jpg"; // Extensión por defecto
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf(".")).toLowerCase();
    }

    /**
     * Limpia un nombre para usarlo en rutas de S3.
     *
     * TRANSFORMACIONES:
     * - "Viaje al Sabor - Escazú" → "VIAJE_AL_SABOR_ESCAZU"
     * - "Taco Bell" → "TACO_BELL"
     * - "La Esquina del Café" → "LA_ESQUINA_DEL_CAFE"
     *
     * @param nombre Nombre original
     * @return Nombre limpio para usar en rutas
     */
    private String limpiarNombreParaRuta(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "SIN_NOMBRE";
        }

        return nombre.trim()
            // Reemplazar espacios por guiones bajos
            .replaceAll("\\s+", "_")
            // Eliminar acentos y caracteres especiales
            .replaceAll("[áàäâ]", "a")
            .replaceAll("[éèëê]", "e")
            .replaceAll("[íìïî]", "i")
            .replaceAll("[óòöô]", "o")
            .replaceAll("[úùüû]", "u")
            .replaceAll("[ñ]", "n")
            .replaceAll("[ÁÀÄÂ]", "A")
            .replaceAll("[ÉÈËÊ]", "E")
            .replaceAll("[ÍÌÏÎ]", "I")
            .replaceAll("[ÓÒÖÔ]", "O")
            .replaceAll("[ÚÙÜÛ]", "U")
            .replaceAll("[Ñ]", "N")
            // Eliminar guiones, puntos y caracteres especiales
            .replaceAll("[^a-zA-Z0-9_]", "")
            // Convertir a mayúsculas
            .toUpperCase();
    }
}