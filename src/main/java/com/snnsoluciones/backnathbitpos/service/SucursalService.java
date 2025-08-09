package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import java.util.List;
import java.util.Optional;

public interface SucursalService {
    
    Sucursal crear(Sucursal sucursal);
    
    Sucursal actualizar(Long id, Sucursal sucursal);
    
    Optional<Sucursal> buscarPorId(Long id);
    
    Optional<Sucursal> buscarPorCodigo(String codigo);
    
    List<Sucursal> listarPorEmpresa(Long empresaId);
    
    List<Sucursal> listarTodas();
    
    void eliminar(Long id);
    
    boolean existeCodigo(String codigo);
}