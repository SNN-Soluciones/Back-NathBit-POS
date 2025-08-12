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
    public String uploadFile(InputStream inputStream, String key, String contentType, long size) {
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

            return url;

        } catch (Exception e) {
            log.error("Error subiendo archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String key) {
        try {
            return uploadFile(
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
    public InputStream downloadFile(String key) {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key);
            return s3Object.getObjectContent();
        } catch (Exception e) {
            log.error("Error descargando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al descargar archivo: " + e.getMessage(), e);
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

    @Override
    public void deleteFile(String key) {
        try {
            s3Client.deleteObject(bucketName, key);
            log.info("Archivo eliminado: {}", key);
        } catch (Exception e) {
            log.error("Error eliminando archivo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            return s3Client.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            log.error("Error verificando existencia de archivo: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String subirArchivo(MultipartFile file, String key, String contentType, boolean isPrivate) {
        try {
            return subirArchivo(file.getBytes(), key, contentType, isPrivate);
        } catch (Exception e) {
            log.error("Error al subir archivo: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivo", e);
        }
    }

    @Override
    public String subirArchivo(byte[] data, String key, String contentType, boolean isPrivate) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(data.length);

            PutObjectRequest putRequest = new PutObjectRequest(
                bucketName,
                key,
                new ByteArrayInputStream(data),
                metadata
            );

            // Configurar ACL según si es privado o público
            if (!isPrivate) {
                putRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            }

            s3Client.putObject(putRequest);

            // Retornar URL completa
            if (isPrivate) {
                // Para archivos privados, retornamos solo la key
                // La URL pre-firmada se generará cuando se necesite
                return key;
            } else {
                // Para archivos públicos, retornamos la URL completa
                return String.format("%s/%s/%s", endpoint, bucketName, key);
            }

        } catch (Exception e) {
            log.error("Error al subir archivo a S3: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivo a S3", e);
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

    @Override
    public boolean eliminarArchivo(String key) {
        try {
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, key));
            log.info("Archivo eliminado correctamente: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar archivo: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existeArchivo(String key) {
        try {
            return s3Client.doesObjectExist(bucketName, key);
        } catch (Exception e) {
            log.error("Error al verificar existencia del archivo: {}", e.getMessage());
            return false;
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