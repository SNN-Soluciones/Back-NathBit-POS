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
    @Transactional
    public EmpresaConfigHacienda crearOActualizar(ConfigHaciendaRequest request) {
        Empresa empresa = empresaService.buscarPorId(request.getEmpresaId())
            .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Buscar o crear configuración
        EmpresaConfigHacienda config = configRepository.findByEmpresaId(request.getEmpresaId())
            .orElseGet(() -> {
                EmpresaConfigHacienda nueva = new EmpresaConfigHacienda();
                nueva.setEmpresa(empresa);
                return nueva;
            });

        // Actualizar campos básicos
        config.setAmbiente(request.getAmbiente());
        config.setTipoAutenticacion(request.getTipoAutenticacion());
        config.setUsuarioHacienda(request.getUsuarioHacienda());

        // Solo actualizar clave si no es el placeholder
        if (request.getClaveHacienda() != null && !request.getClaveHacienda().equals("********")) {
            config.setClaveHacienda(passwordEncoder.encode(request.getClaveHacienda()));
        }

        // Si viene URL del certificado, guardarlo
        if (request.getUrlCertificadoKey() != null && !request.getUrlCertificadoKey().isEmpty()) {
            config.setUrlCertificadoKey(request.getUrlCertificadoKey());
            config.setCertificadoEncriptado(true);

            // Si viene fecha de vencimiento del certificado
            if (request.getFechaVencimientoCertificado() != null) {
                config.setFechaVencimientoCertificado(request.getFechaVencimientoCertificado());
            }
        }

        // PIN del certificado (si se proporciona)
        if (request.getPinCertificado() != null && !request.getPinCertificado().isEmpty()) {
            config.setPinCertificado(passwordEncoder.encode(request.getPinCertificado()));
        }

        config.setProveedorSistemas(request.getProveedorSistemas());

        // Mensajes personalizados
        config.setNotaFactura(request.getNotaFactura());
        config.setNotaValidezProforma(request.getNotaValidezProforma());
        config.setDetalleFactura1(request.getDetalleFactura1());
        config.setDetalleFactura2(request.getDetalleFactura2());

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