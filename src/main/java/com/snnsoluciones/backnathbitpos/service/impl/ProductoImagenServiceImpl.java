package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Producto;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.ProductoRepository;
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
    
    @Value("${app.storage.max-file-size:5242880}") // 5MB por defecto
    private long maxFileSize;
    
    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".webp"
    );
    
    private static final List<String> CONTENT_TYPES_PERMITIDOS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    
    @Override
    public String subirImagen(Long empresaId, String nombreComercial, String codigoInterno, MultipartFile imagen) {
        try {
            // Validar imagen
            validarImagen(imagen);
            
            // Limpiar nombre comercial para usar en la ruta
            String nombreComercialLimpio = limpiarNombreParaRuta(nombreComercial);
            
            // Obtener extensión del archivo
            String nombreOriginal = imagen.getOriginalFilename();
            String extension = obtenerExtension(nombreOriginal);
            
            // Construir la ruta: NathBit-POS/NOMBRE_COMERCIAL/productos/codigo_interno.extension
            String carpeta = String.format("NathBit-POS/%s/productos", nombreComercialLimpio);
            String nombreArchivo = codigoInterno;
            
            // Subir a S3
            String urlImagen = storageService.subirArchivo(imagen, carpeta, nombreArchivo, false);
            
            log.info("Imagen subida exitosamente para producto {}: {}", codigoInterno, urlImagen);
            
            return urlImagen;
            
        } catch (Exception e) {
            log.error("Error al subir imagen del producto {}: {}", codigoInterno, e.getMessage());
            throw new RuntimeException("Error al subir imagen: " + e.getMessage(), e);
        }
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
        
        if (producto.getImagenKey() != null) {
            try {
                // Eliminar de S3
                storageService.eliminarArchivo(producto.getImagenKey());
                log.info("Imagen eliminada de S3: {}", producto.getImagenKey());
            } catch (Exception e) {
                log.error("Error al eliminar imagen de S3: {}", e.getMessage());
                // No lanzar excepción, continuar con la limpieza en BD
            }
        }
        
        // Limpiar campos en la BD
        producto.setImagenUrl(null);
        producto.setImagenKey(null);
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
        
        // Reemplazar espacios por guiones bajos
        // Eliminar caracteres especiales excepto guiones y guiones bajos
        // Convertir a minúsculas
        return nombre.trim()
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-zA-Z0-9_-]", "")
            .toLowerCase();
    }
}