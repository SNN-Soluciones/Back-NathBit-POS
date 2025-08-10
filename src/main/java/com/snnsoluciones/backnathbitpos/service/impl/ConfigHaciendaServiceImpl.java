package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaRequest;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.service.ConfigHaciendaService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfigHaciendaServiceImpl implements ConfigHaciendaService {

    private final EmpresaConfigHaciendaRepository configRepository;
    private final EmpresaService empresaService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Optional<EmpresaConfigHacienda> buscarPorEmpresa(Long empresaId) {
        return configRepository.findByEmpresaId(empresaId);
    }

    @Override
    public EmpresaConfigHacienda crearOActualizar(ConfigHaciendaRequest request) {
        Empresa empresa = empresaService.buscarPorId(request.getEmpresaId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Buscar config existente o crear nueva
        EmpresaConfigHacienda config = configRepository.findByEmpresaId(request.getEmpresaId())
            .orElse(new EmpresaConfigHacienda());

        // Actualizar valores básicos
        config.setEmpresa(empresa);
        config.setAmbiente(request.getAmbiente());
        config.setTipoAutenticacion(request.getTipoAutenticacion());
        config.setUsuarioHacienda(request.getUsuarioHacienda());

        // Encriptar clave si cambió (no es el placeholder)
        if (request.getClaveHacienda() != null && !request.getClaveHacienda().equals("********")) {
            config.setClaveHacienda(passwordEncoder.encode(request.getClaveHacienda()));
        }

        // Certificado y PIN (para firma digital) - usando los nombres CORRECTOS del DTO original
        if (request.getCertificadoBase64() != null && !request.getCertificadoBase64().isEmpty()) {
            config.setCertificadoP12(Base64.getDecoder().decode(request.getCertificadoBase64()));
        }

        if (request.getPinLlaveCriptografica() != null && !request.getPinLlaveCriptografica().isEmpty()) {
            config.setPinCertificado(passwordEncoder.encode(request.getPinLlaveCriptografica()));
        }

        config.setProveedorSistemas(request.getProveedorSistemas());

        return configRepository.save(config);
    }

    @Override
    public void eliminar(Long id) {
        configRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esConfiguracionCompleta(Long empresaId) {
        return configRepository.existsConfiguracionCompleta(empresaId);
    }
}