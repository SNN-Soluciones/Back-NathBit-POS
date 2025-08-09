package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import java.util.List;
import java.util.Optional;

public interface EmpresaService {
    
    Empresa crear(Empresa empresa);
    
    Empresa actualizar(Long id, Empresa empresa);
    
    Optional<Empresa> buscarPorId(Long id);
    
    Optional<Empresa> buscarPorCodigo(String codigo);
    
    List<Empresa> listarTodas();
    
    void eliminar(Long id);
    
    boolean existeCodigo(String codigo);
    
    boolean existeIdentificacion(String identificacion);

    // En EmpresaService.java
    List<Empresa> listarPorUsuario(Long usuarioId);
}