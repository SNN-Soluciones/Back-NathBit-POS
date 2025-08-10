package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaRequest;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;

import java.util.Optional;

public interface ConfigHaciendaService {
    
    Optional<EmpresaConfigHacienda> buscarPorEmpresa(Long empresaId);
    
    EmpresaConfigHacienda crearOActualizar(ConfigHaciendaRequest request);
    
    void eliminar(Long id);
    
    boolean esConfiguracionCompleta(Long empresaId);
}