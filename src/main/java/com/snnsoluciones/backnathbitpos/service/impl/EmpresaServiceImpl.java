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
        existente.setRegimenTributario(empresa.getRegimenTributario());

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