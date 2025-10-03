package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

/**
 * Servicio para generar thumbnails de productos existentes
 * Se ejecuta con el perfil "thumbnail-migration"
 */
@Slf4j
@Component
@Profile("thumbnail-migration")
@RequiredArgsConstructor
public class ThumbnailMigrationService implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final StorageService storageService;
    private final ImageProcessingService imageProcessingService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Iniciando migración de thumbnails ===");

        try {
            // Buscar productos con imagen pero sin thumbnail
            List<Producto> productos = productoRepository.findAllByImagenUrlNotNullAndThumbnailUrlNull();

            log.info("Encontrados {} productos para procesar", productos.size());

            int procesados = 0;
            int errores = 0;

            for (Producto producto : productos) {
                try {
                    log.info("Procesando producto: {} - {}", producto.getCodigoInterno(), producto.getNombre());

                    // Descargar imagen original
                    URL url = new URL(producto.getImagenUrl());
                    BufferedImage imagenOriginal = ImageIO.read(url);

                    if (imagenOriginal != null) {
                        // Generar thumbnail
                        BufferedImage thumbnail = redimensionarImagen(imagenOriginal, 150, 150);

                        // Convertir a bytes
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(thumbnail, "jpg", baos);
                        byte[] thumbnailBytes = baos.toByteArray();

                        // IMPORTANTE: Generar una nueva key para el thumbnail
                        String thumbnailKey = generarKeyThumbnail(producto.getImagenKey());

                        log.debug("Key original: {}", producto.getImagenKey());
                        log.debug("Key thumbnail: {}", thumbnailKey);

                        // Subir thumbnail con su propia key
                        String thumbnailUrl = storageService.subirArchivo(
                            thumbnailBytes,
                            thumbnailKey,
                            "image/jpeg",
                            false
                        );

                        // Actualizar SOLO los campos de thumbnail, NO tocar la imagen original
                        producto.setThumbnailUrl(thumbnailUrl);
                        producto.setThumbnailKey(thumbnailKey);
                        productoRepository.save(producto);

                        procesados++;
                        log.info("✓ Thumbnail generado para: {}", producto.getCodigoInterno());
                        log.info("  - Imagen original: {}", producto.getImagenUrl());
                        log.info("  - Thumbnail nuevo: {}", thumbnailUrl);
                    }

                } catch (Exception e) {
                    errores++;
                    log.error("✗ Error procesando producto {}: {}",
                        producto.getCodigoInterno(), e.getMessage());
                }

                // Pausa pequeña para no saturar S3
                Thread.sleep(100);
            }

            log.info("=== Migración completada ===");
            log.info("Procesados: {}, Errores: {}", procesados, errores);

        } catch (Exception e) {
            log.error("ERROR CRÍTICO en migración: {}", e.getMessage(), e);
        }
    }

    /**
     * Genera la key para el thumbnail basándose en la key original
     */
    private String generarKeyThumbnail(String keyOriginal) {
        if (keyOriginal == null) return null;

        // Si ya tiene _thumb, no hacer nada
        if (keyOriginal.contains("_thumb")) {
            return keyOriginal;
        }

        // Buscar la última extensión
        int lastDot = keyOriginal.lastIndexOf('.');
        if (lastDot > 0) {
            String sinExtension = keyOriginal.substring(0, lastDot);
            // Siempre usar .jpg para thumbnails
            return sinExtension + "_thumb.jpg";
        }

        // Si no tiene extensión, agregar al final
        return keyOriginal + "_thumb.jpg";
    }

    private BufferedImage redimensionarImagen(BufferedImage original, int width, int height) {
        double proporcion = Math.min(
            (double) width / original.getWidth(),
            (double) height / original.getHeight()
        );

        int nuevoAncho = (int) (original.getWidth() * proporcion);
        int nuevoAlto = (int) (original.getHeight() * proporcion);

        BufferedImage thumbnail = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo blanco
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, nuevoAncho, nuevoAlto);
        g2d.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        g2d.dispose();

        return thumbnail;
    }
}