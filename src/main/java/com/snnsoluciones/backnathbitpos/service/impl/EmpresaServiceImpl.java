package com.snnsoluciones.backnathbitpos.service.impl;

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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
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