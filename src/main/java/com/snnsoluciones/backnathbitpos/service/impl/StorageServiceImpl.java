package com.snnsoluciones.backnathbitpos.service.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    @Value("${storage.spaces.access-key:DO00C3NT8QYQ9R7HHJRW}")
    private String accessKey;

    @Value("${storage.spaces.secret-key:HyOasERhBvz4pLciZTf0W8utirzlvYPR41bteNSeYLA}")
    private String secretKey;

    @Value("${storage.spaces.endpoint:https://nyc3.digitaloceanspaces.com}")
    private String endpoint;

    @Value("${storage.spaces.region:nyc3}")
    private String region;

    @Value("${storage.spaces.bucket:snn-soluciones}")
    private String bucketName;

    @Value("${storage.spaces.base-folder:NathBit-POS}")
    private String baseFolder;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        s3Client = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();

        log.info("Storage service inicializado con endpoint: {}", endpoint);
    }

    @Override
    public void uploadFile(InputStream inputStream, String key, String contentType, long size) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(size);

            // Agregar metadatos adicionales
            metadata.addUserMetadata("uploaded-by", "NathBit-POS");
            metadata.addUserMetadata("upload-date", Instant.now().toString());

            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            request.setCannedAcl(CannedAccessControlList.Private); // Archivos privados

            s3Client.putObject(request);

            String url = String.format("https://%s.%s/%s", bucketName, endpoint.replace("https://", ""), key);
            log.info("Archivo subido exitosamente: {}", url);

        } catch (Exception e) {
            log.error("Error subiendo archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void uploadFile(MultipartFile file, String key) {
        try {
            uploadFile(
                file.getInputStream(),
                key,
                file.getContentType(),
                file.getSize()
            );
        } catch (Exception e) {
            log.error("Error subiendo MultipartFile: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateSignedUrl(String key, int expirationMinutes) {
        try {
            Date expiration = Date.from(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key)
                .withMethod(com.amazonaws.HttpMethod.GET)
                .withExpiration(expiration);

            URL url = s3Client.generatePresignedUrl(request);
            log.debug("URL firmada generada para {}: {}", key, url.toString());

            return url.toString();

        } catch (Exception e) {
            log.error("Error generando URL firmada: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar URL firmada: " + e.getMessage(), e);
        }
    }


    /**
     * Sube un archivo a S3 con configuración específica
     *
     * @param archivo Archivo a subir
     * @param carpeta Carpeta destino (sin slash al final)
     * @param nombreArchivo Nombre del archivo SIN extensión
     * @param privado Si el archivo debe ser privado o público
     * @return URL completa del archivo (si es público) o key (si es privado)
     */
    @Override
    public String subirArchivo(MultipartFile archivo, String carpeta, String nombreArchivo, boolean privado) {
        try {
            log.debug("Subiendo archivo: carpeta={}, nombre={}, privado={}",
                carpeta, nombreArchivo, privado);

            // Obtener extensión del archivo original
            String originalFilename = archivo.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Construir key completa: carpeta/nombreArchivo.ext
            String key = carpeta + "/" + nombreArchivo + extension;

            // Configurar metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(archivo.getContentType());
            metadata.setContentLength(archivo.getSize());
            metadata.addUserMetadata("uploaded-by", "NathBit-POS-V3");
            metadata.addUserMetadata("upload-date", Instant.now().toString());

            // Crear request
            PutObjectRequest putRequest = new PutObjectRequest(
                bucketName,
                key,
                archivo.getInputStream(),
                metadata
            );

            // ✅ CONFIGURAR ACL SEGÚN SI ES PÚBLICO O PRIVADO
            if (privado) {
                putRequest.setCannedAcl(CannedAccessControlList.Private);
                log.debug("Archivo configurado como PRIVADO");
            } else {
                putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
                log.debug("Archivo configurado como PÚBLICO");
            }

            // Subir archivo
            s3Client.putObject(putRequest);

            // Retornar URL o key según si es público o privado
            if (privado) {
                // Para archivos privados, retornar solo la key
                log.info("Archivo PRIVADO subido exitosamente: {}", key);
                return key;
            } else {
                // Para archivos públicos, retornar URL completa
                String url = s3Client.getUrl(bucketName, key).toString();
                log.info("Archivo PÚBLICO subido exitosamente: {}", url);
                return url;
            }

        } catch (Exception e) {
            log.error("Error al subir archivo a S3: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String subirArchivo(byte[] bytes, String key, String contentType, boolean privado) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(bytes.length);

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            PutObjectRequest putRequest = new PutObjectRequest(
                bucketName,
                key,
                bais,
                metadata
            );

            if (!privado) {
                putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            }

            s3Client.putObject(putRequest);

            return privado ? key : s3Client.getUrl(bucketName, key).toString();

        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String generarUrlPreFirmada(String key, Duration duracion) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + duracion.toMillis());

            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, key)
                    .withMethod(com.amazonaws.HttpMethod.GET)
                    .withExpiration(expiration);

            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            return url.toString();

        } catch (Exception e) {
            log.error("Error al generar URL pre-firmada: {}", e.getMessage());
            throw new RuntimeException("Error al generar URL pre-firmada", e);
        }
    }

    /**
     * Elimina un archivo de S3
     *
     * @param key Ruta completa del archivo en S3
     */
    @Override
    public void eliminarArchivo(String key) {
        try {
            log.debug("Eliminando archivo: {}", key);

            DeleteObjectRequest deleteRequest = new DeleteObjectRequest(bucketName, key);
            s3Client.deleteObject(deleteRequest);

            log.info("Archivo eliminado exitosamente: {}", key);

        } catch (Exception e) {
            log.error("Error al eliminar archivo de S3: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] obtenerArchivo(String key) {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key);
            S3ObjectInputStream inputStream = s3Object.getObjectContent();
            return IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            log.error("Error al obtener archivo de S3: {}", e.getMessage());
            throw new RuntimeException("Error al obtener archivo de S3", e);
        }
    }

    /**
     * Implementación requerida por los workers para convertir el SIGNED a Base64.
     */
    @Override
    public byte[] downloadFileAsBytes(String key) {
        // Reutiliza la lógica ya existente
        return obtenerArchivo(key);
    }

    /**
     * Genera la ruta completa para un archivo de factura
     * Formato: NathBit-POS/{EMPRESA_NOMBRE}/documentos/2025/agosto/09/clave_TIPO.ext
     */
    public String generarRutaFactura(String empresaNombre, String clave, String tipoArchivo, String extension) {
        // Limpiar nombre de empresa (reemplazar espacios por _)
        String empresaLimpia = empresaNombre.replaceAll("\\s+", "_")
            .replaceAll("[^a-zA-Z0-9_-]", "");

        LocalDateTime ahora = LocalDateTime.now();
        String mesNombre = obtenerNombreMes(ahora.getMonthValue());

        return String.format("%s/%s/documentos/%d/%s/%02d/%s_%s.%s",
            baseFolder,
            empresaLimpia,
            ahora.getYear(),
            mesNombre,
            ahora.getDayOfMonth(),
            clave,
            tipoArchivo,
            extension
        );
    }

    private String obtenerNombreMes(int mes) {
        return switch (mes) {
            case 1 -> "enero";
            case 2 -> "febrero";
            case 3 -> "marzo";
            case 4 -> "abril";
            case 5 -> "mayo";
            case 6 -> "junio";
            case 7 -> "julio";
            case 8 -> "agosto";
            case 9 -> "septiembre";
            case 10 -> "octubre";
            case 11 -> "noviembre";
            case 12 -> "diciembre";
            default -> "mes" + mes;
        };
    }
}