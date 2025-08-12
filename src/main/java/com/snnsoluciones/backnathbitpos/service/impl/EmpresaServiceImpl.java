package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.empresa.CertificadoResponse;
import com.snnsoluciones.backnathbitpos.dto.empresa.UrlCertificadoResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaActividad;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.EmpresaActividadRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.service.CertificadoService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaConfigHaciendaRepository configHaciendaRepository;
    private final EmpresaActividadRepository empresaActividadRepository;
    private final UsuarioServiceImpl usuarioService;

    @Autowired
    private CertificadoService certificadoService;

    @Autowired
    private StorageService storageService;

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
    public Empresa crear(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    @Override
    public Empresa actualizar(Long id, Empresa empresa) {
        Empresa existente = empresaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Campos existentes
        existente.setNombre(empresa.getNombre());
        existente.setCodigo(empresa.getCodigo());
        existente.setTipoIdentificacion(empresa.getTipoIdentificacion());
        existente.setIdentificacion(empresa.getIdentificacion());
        existente.setDireccion(empresa.getDireccion());
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
    public Optional<Empresa> buscarPorId(Long id) {
        return empresaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Empresa> buscarPorCodigo(String codigo) {
        return empresaRepository.findByCodigo(codigo);
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
    public boolean existeCodigo(String codigo) {
        return empresaRepository.existsByCodigo(codigo);
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

    // === NUEVOS MÉTODOS ===

    @Override
    public EmpresaConfigHacienda crearConfiguracionHacienda(Long empresaId, EmpresaConfigHacienda config) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Verificar si ya existe configuración
        if (configHaciendaRepository.findByEmpresaId(empresaId).isPresent()) {
            throw new RuntimeException("La empresa ya tiene configuración de Hacienda");
        }

        config.setEmpresa(empresa);
        return configHaciendaRepository.save(config);
    }

    @Override
    public EmpresaConfigHacienda actualizarConfiguracionHacienda(Long empresaId, EmpresaConfigHacienda config) {
        EmpresaConfigHacienda existente = configHaciendaRepository.findByEmpresaId(empresaId)
            .orElseThrow(() -> new RuntimeException("Configuración de Hacienda no encontrada"));

        existente.setAmbiente(config.getAmbiente());
        existente.setTipoAutenticacion(config.getTipoAutenticacion());
        existente.setUsuarioHacienda(config.getUsuarioHacienda());
        if (config.getClaveHacienda() != null && !config.getClaveHacienda().isEmpty()) {
            // Aquí deberías encriptar la clave antes de guardarla
            existente.setClaveHacienda(config.getClaveHacienda());
        }
        existente.setProveedorSistemas(config.getProveedorSistemas());
        existente.setNotaFactura(config.getNotaFactura());
        existente.setNotaValidezProforma(config.getNotaValidezProforma());

        return configHaciendaRepository.save(existente);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmpresaConfigHacienda> buscarConfiguracionHacienda(Long empresaId) {
        return configHaciendaRepository.findByEmpresaId(empresaId);
    }

    @Override
    public EmpresaActividad agregarActividad(Long empresaId, String codigoActividad, Boolean esPrincipal) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Si es principal, quitar la marca de principal a las demás
        if (Boolean.TRUE.equals(esPrincipal)) {
            empresaActividadRepository.findByEmpresaIdOrderByOrden(empresaId)
                .forEach(ea -> {
                    ea.setEsPrincipal(false);
                    empresaActividadRepository.save(ea);
                });
        }

        EmpresaActividad nuevaActividad = new EmpresaActividad();
        nuevaActividad.setEmpresa(empresa);
        // Aquí deberías buscar la actividad económica por código
        nuevaActividad.setEsPrincipal(esPrincipal);

        return empresaActividadRepository.save(nuevaActividad);
    }

    @Override
    @Transactional
    public CertificadoResponse subirCertificado(Long empresaId, MultipartFile certificado, String pin) {
        // Buscar empresa
        Empresa empresa = buscarPorId(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

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
                empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombre()
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
                .nombreEmpresa(empresa.getNombre())
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
        Empresa empresa = buscarPorId(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        try {
            // Eliminar logo anterior si existe
            if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().contains("defaults")) {
                // Extraer key del URL anterior
                String oldKey = extraerKeyDeUrl(empresa.getLogoUrl());
                if (oldKey != null) {
                    storageService.eliminarArchivo(oldKey);
                }
            }

            // Construir nueva ruta
            String nombreComercialSanitizado = certificadoService.sanitizarNombreComercial(
                empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombre()
            );
            String extension = obtenerExtension(logo.getOriginalFilename());
            String key = String.format("%s/%s/logo/logo.%s",
                baseFolder, nombreComercialSanitizado, extension);

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
        Empresa empresa = buscarPorId(empresaId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

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

    @Override
    @Transactional(readOnly = true)
    public List<EmpresaActividad> listarActividades(Long empresaId) {
        return empresaActividadRepository.findByEmpresaIdOrderByOrden(empresaId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneFacturacionElectronicaConfigurada(Long empresaId) {
        return empresaRepository.tieneFacturacionElectronicaConfigurada(empresaId);
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
}