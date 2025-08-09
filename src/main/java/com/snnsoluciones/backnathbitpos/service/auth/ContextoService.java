package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoDTO;
import com.snnsoluciones.backnathbitpos.entity.UsuarioEmpresaRol;

/**
 * Servicio para gestionar el contexto de trabajo del usuario (empresa/sucursal).
 */
public interface ContextoService {
    
    /**
     * Establece el contexto de trabajo para un usuario.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa seleccionada
     * @param sucursalId ID de la sucursal (opcional)
     * @return ContextoDTO con la información del contexto establecido
     */
    ContextoDTO establecerContexto(Long usuarioId, Long empresaId, Long sucursalId);
    
    /**
     * Obtiene el contexto actual de un usuario.
     * 
     * @param usuarioId ID del usuario
     * @return ContextoDTO actual o null si no hay contexto
     */
    ContextoDTO obtenerContextoActual(Long usuarioId);
    
    /**
     * Limpia el contexto de un usuario.
     * 
     * @param usuarioId ID del usuario
     */
    void limpiarContexto(Long usuarioId);
    
    /**
     * Valida que el usuario tenga acceso al contexto especificado.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (opcional)
     * @return true si tiene acceso
     */
    boolean validarAccesoContexto(Long usuarioId, Long empresaId, Long sucursalId);
    
    /**
     * Construye un ContextoDTO a partir de un UsuarioEmpresaRol.
     * 
     * @param usuarioEmpresaRol relación usuario-empresa-rol
     * @return ContextoDTO con toda la información
     */
    ContextoDTO construirContexto(UsuarioEmpresaRol usuarioEmpresaRol);

    public void actualizarActividad(Long usuarioId);
}