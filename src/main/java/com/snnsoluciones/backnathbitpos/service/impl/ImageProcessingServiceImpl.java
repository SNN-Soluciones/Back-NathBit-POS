package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.service.ImageProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {
    
    private static final int DEFAULT_THUMBNAIL_SIZE = 150;
    private static final String DEFAULT_FORMAT = "jpg";
    
    @Override
    public byte[] generarThumbnail(MultipartFile imagen, int width, int height) throws IOException {
        log.debug("Generando thumbnail de {}x{} para imagen: {}", width, height, imagen.getOriginalFilename());
        
        // Leer la imagen original
        BufferedImage imagenOriginal = ImageIO.read(imagen.getInputStream());
        if (imagenOriginal == null) {
            throw new IOException("No se pudo leer la imagen");
        }
        
        // Calcular las dimensiones manteniendo la proporción
        Dimension dimensionThumbnail = calcularDimensionesProporcionadas(
            imagenOriginal.getWidth(), 
            imagenOriginal.getHeight(), 
            width, 
            height
        );
        
        // Crear el thumbnail
        BufferedImage thumbnail = new BufferedImage(
            dimensionThumbnail.width,
            dimensionThumbnail.height,
            BufferedImage.TYPE_INT_RGB
        );
        
        // Dibujar la imagen redimensionada con mejor calidad
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fondo blanco para imágenes con transparencia
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, dimensionThumbnail.width, dimensionThumbnail.height);
        
        // Dibujar la imagen
        g2d.drawImage(imagenOriginal, 0, 0, dimensionThumbnail.width, dimensionThumbnail.height, null);
        g2d.dispose();
        
        // Convertir a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, DEFAULT_FORMAT, baos);
        
        byte[] thumbnailBytes = baos.toByteArray();
        log.debug("Thumbnail generado exitosamente. Tamaño: {} bytes", thumbnailBytes.length);
        
        return thumbnailBytes;
    }
    
    @Override
    public byte[] generarThumbnail(MultipartFile imagen) throws IOException {
        return generarThumbnail(imagen, DEFAULT_THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE);
    }
    
    private Dimension calcularDimensionesProporcionadas(int anchoOriginal, int altoOriginal, 
                                                        int anchoMaximo, int altoMaximo) {
        double proporcion = Math.min(
            (double) anchoMaximo / anchoOriginal,
            (double) altoMaximo / altoOriginal
        );
        
        int anchoFinal = (int) (anchoOriginal * proporcion);
        int altoFinal = (int) (altoOriginal * proporcion);
        
        return new Dimension(anchoFinal, altoFinal);
    }
}