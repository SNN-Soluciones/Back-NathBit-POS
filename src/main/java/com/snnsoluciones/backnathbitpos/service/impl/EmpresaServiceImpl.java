package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.empresa.CertificadoResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.UrlCertificadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.service.CertificadoService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.UsuarioEmpresaService;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmpresaServiceImpl implements EmpresaService {

    private final ModelMapper modelMapper;

    private final EmpresaRepository empresaRepository;
    private final EmpresaConfigHaciendaRepository configHaciendaRepository;
    private final UsuarioServiceImpl usuarioService;
    private final UsuarioEmpresaService usuarioEmpresaRepository;
    private final S3PathBuilder s3PathBuilder;
    private final CertificadoService certificadoService;
    private final StorageService storageService;

    @Value("${app.certificados.presigned-url-duration-minutes:15}")
    private Integer presignedUrlDurationMinutes;

    @Value("${app.logos.default-logo-path:defaults/logo-default.png}")
    private String defaultLogoPath;

    @Value("${storage.spaces.base-folder}")
    private String baseFolder;

    @Value("${storage.spaces.endpoint}")
    private String endpoint;

    @Value("${storage.spaces.bucket}")
    private String bucketName;

    @Override
    public Empresa actualizar(Long id, Empresa empresa) {
        Empresa existente = empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Campos existentes
        existente.setNombreRazonSocial(empresa.getNombreRazonSocial());
        existente.setTipoIdentificacion(empresa.getTipoIdentificacion());
        existente.setIdentificacion(empresa.getIdentificacion());
        existente.setTelefono(empresa.getTelefono());
        existente.setEmail(empresa.getEmail());
        existente.setActiva(empresa.getActiva());

        // === NUEVOS CAMPOS ===
        existente.setNombreComercial(empresa.getNombreComercial());
        existente.setNombreRazonSocial(empresa.getNombreRazonSocial());
        existente.setFax(empresa.getFax());
        existente.setEmailNotificacion(empresa.getEmailNotificacion());
        existente.setProvincia(empresa.getProvincia());
        existente.setCanton(empresa.getCanton());
        existente.setDistrito(empresa.getDistrito());
        existente.setBarrio(empresa.getBarrio());
        existente.setOtrasSenas(empresa.getOtrasSenas());
        existente.setRequiereHacienda(empresa.getRequiereHacienda());
        existente.setRegimenTributario(empresa.getRegimenTributario());
        existente.setLimiteAnualSimplificado(empresa.getLimiteAnualSimplificado());
        existente.setLogoUrl(empresa.getLogoUrl());

        return empresaRepository.save(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public Empresa buscarPorId(Long id) {
        return empresaRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Empresa> listarTodas() {
        return empresaRepository.findAll();
    }

    @Override
    public void eliminar(Long id) {
        empresaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeIdentificacion(String identificacion) {
        return empresaRepository.existsByIdentificacion(identificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Empresa> listarPorUsuario(Long usuarioId, Pageable pageable) {
        return empresaRepository.findByUsuarioId(usuarioId, pageable);
    }

    @Override
    @Transactional
    public CertificadoResponse subirCertificado(Long empresaId, MultipartFile certificado, String pin) {
        // Buscar empresa
        Empresa empresa = buscarPorId(empresaId);
        if (empresa == null) {
            throw  new ResourceNotFoundException("Empresa no encontrada");
        }
        // Validar que requiere facturación electrónica
        if (!Boolean.TRUE.equals(empresa.getRequiereHacienda())) {
            throw new RuntimeException("Esta empresa no requiere facturación electrónica");
        }

        // Validar certificado con PIN
        if (!certificadoService.validarCertificado(certificado, pin)) {
            throw new RuntimeException("Certificado inválido o PIN incorrecto");
        }

        // Extraer información del certificado
        LocalDate fechaVencimiento = certificadoService.extraerFechaVencimiento(certificado, pin);
        if (fechaVencimiento == null || fechaVencimiento.isBefore(LocalDate.now())) {
            throw new RuntimeException("El certificado está vencido o próximo a vencer");
        }

        try {
            // Obtener configuración existente si hay
            EmpresaConfigHacienda config = configHaciendaRepository
                .findByEmpresaId(empresaId)
                .orElse(null);

            // Si existe config previa, eliminar certificado anterior
            if (config != null && config.getUrlCertificadoKey() != null) {
                storageService.eliminarArchivo(config.getUrlCertificadoKey());
            }

            // Encriptar certificado
            byte[] certificadoEncriptado = certificadoService.encriptar(certificado.getBytes());

            // Construir ruta en S3
            String nombreComercialSanitizado = certificadoService.sanitizarNombreComercial(
                empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreRazonSocial()
            );
            String key = String.format("%s/%s/certificado/certificado.p12",
                baseFolder, nombreComercialSanitizado);

            // Subir a S3 (privado)
            String urlKey = storageService.subirArchivo(
                certificadoEncriptado,
                key,
                "application/x-pkcs12",
                true
            );

            // NO guardamos la config aquí, solo retornamos la info
            return CertificadoResponse.builder()
                .exitoso(true)
                .mensaje("Certificado validado y procesado correctamente")
                .fechaVencimiento(fechaVencimiento.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .nombreEmpresa(empresa.getNombreRazonSocial())
                .identificacion(empresa.getIdentificacion())
                .urlCertificadoKey(urlKey) // Agregamos este campo al response
                .build();

        } catch (Exception e) {
            log.error("Error al procesar certificado: {}", e.getMessage());
            throw new RuntimeException("Error al procesar certificado: " + e.getMessage());
        }
    }

    @Override
    public UrlCertificadoResponse generarUrlCertificado(Long empresaId) {
        // Buscar configuración
        EmpresaConfigHacienda config = configHaciendaRepository
            .findByEmpresaId(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("No hay configuración de Hacienda"));

        if (config.getUrlCertificadoKey() == null) {
            throw new RuntimeException("No hay certificado registrado para esta empresa");
        }

        // Generar URL pre-firmada
        Duration duracion = Duration.ofMinutes(presignedUrlDurationMinutes);
        String urlPreFirmada = storageService.generarUrlPreFirmada(
            config.getUrlCertificadoKey(),
            duracion
        );

        return UrlCertificadoResponse.builder()
            .url(urlPreFirmada)
            .minutosValidez(presignedUrlDurationMinutes)
            .fechaExpiracion(LocalDateTime.now().plusMinutes(presignedUrlDurationMinutes)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .build();
    }

    @Override
    @Transactional
    public void eliminarCertificado(Long empresaId) {
        EmpresaConfigHacienda config = configHaciendaRepository
            .findByEmpresaId(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("No hay configuración de Hacienda"));

        if (config.getUrlCertificadoKey() != null) {
            // Eliminar de S3
            storageService.eliminarArchivo(config.getUrlCertificadoKey());

            // Limpiar datos en BD
            config.setUrlCertificadoKey(null);
            config.setPinCertificado(null);
            config.setFechaVencimientoCertificado(null);
            config.setCertificadoEncriptado(null);

            configHaciendaRepository.save(config);
        }
    }

    @Override
    @Transactional
    public String subirLogo(Long empresaId, MultipartFile logo) {
        try {
            // Validaciones
            Empresa empresa = buscarPorId(empresaId);
            if (empresa == null) {
                throw new ResourceNotFoundException("Empresa no encontrada");
            }

            if (logo.isEmpty()) {
                throw new IllegalArgumentException("Archivo de logo vacío");
            }

            String contentType = logo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("El archivo debe ser una imagen");
            }

            // Tamaño máximo 5MB
            if (logo.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("El logo no puede superar 5MB");
            }

            // Eliminar logo anterior si existe
            if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().contains("defaults")) {
                String keyAnterior = extraerKeyDeUrl(empresa.getLogoUrl());
                if (keyAnterior != null) {
                    storageService.eliminarArchivo(keyAnterior);
                }
            }

            // Obtener extensión
            String extension = obtenerExtension(logo.getOriginalFilename());

            // Usar S3PathBuilder para generar la ruta
            String nombreEmpresa = empresa.getNombreComercial() != null ?
                empresa.getNombreComercial() : empresa.getNombreRazonSocial();

            String key = s3PathBuilder.buildLogoPath(nombreEmpresa, extension);

            // Subir a S3 (público)
            String logoUrl = storageService.subirArchivo(
                logo,
                key,
                logo.getContentType(),
                false
            );

            // Actualizar empresa
            empresa.setLogoUrl(logoUrl);
            empresaRepository.save(empresa);

            return logoUrl;

        } catch (Exception e) {
            log.error("Error al subir logo: {}", e.getMessage());
            throw new RuntimeException("Error al subir logo: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void eliminarLogo(Long empresaId) {
        Empresa empresa = buscarPorId(empresaId);
        if (empresa == null) {
            throw  new ResourceNotFoundException("Empresa no encontrada");
        }

        if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().contains("defaults")) {
            // Extraer key del URL
            String key = extraerKeyDeUrl(empresa.getLogoUrl());
            if (key != null) {
                storageService.eliminarArchivo(key);
            }

            // Poner logo por defecto
            empresa.setLogoUrl(String.format("%s/%s/%s", endpoint, bucketName, defaultLogoPath));
            empresaRepository.save(empresa);
        }
    }

    // Métodos auxiliares
    private String extraerKeyDeUrl(String url) {
        if (url == null) return null;
        // Extraer la key después del bucket name
        int index = url.lastIndexOf(bucketName + "/");
        if (index != -1) {
            return url.substring(index + bucketName.length() + 1);
        }
        return null;
    }

    private String obtenerExtension(String filename) {
        if (filename == null) return "jpg";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "jpg";
    }

    public Page<Empresa> listarPorUsuarioPaginado(Long usuarioId, Pageable pageable) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Si es ROOT o SOPORTE, puede ver todas
        if (usuario.esRolSistema()) {
            return empresaRepository.findAll(pageable);
        }

        // Si es SUPER_ADMIN, solo sus empresas
        return listarPorUsuario(usuarioId, pageable);
    }

    @Override
    public boolean usuarioTieneAcceso(Long usuarioId, Long empresaId) {
        // Para SUPER_ADMIN, verificar en la tabla usuarios_empresas
        return usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId);
    }

    @Override
    public Page<EmpresaResponse> listar(Pageable pageable) {
        Page<Empresa> empresa = empresaRepository.findAll(pageable);
        return empresa.map(e -> modelMapper.map(e, EmpresaResponse.class));
    }
}