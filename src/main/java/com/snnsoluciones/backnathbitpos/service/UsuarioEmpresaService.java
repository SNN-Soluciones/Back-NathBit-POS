package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresa;
import java.util.List;

public interface UsuarioEmpresaService {
    
    UsuarioEmpresa asignar(Long usuarioId, Long empresaId, Long sucursalId);
    
    void desasignar(Long id);
    
    List<UsuarioEmpresa> listarPorUsuario(Long usuarioId);
    
    List<UsuarioEmpresa> listarPorEmpresa(Long empresaId);
    
    List<UsuarioEmpresa> listarPorSucursal(Long sucursalId);
    
    boolean tieneAcceso(Long usuarioId, Long empresaId, Long sucursalId);
}