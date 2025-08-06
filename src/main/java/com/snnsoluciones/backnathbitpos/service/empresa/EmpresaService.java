package com.snnsoluciones.backnathbitpos.service.empresa;

import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.EmpresaDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.ConfiguracionEmpresaDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.EstadisticasEmpresaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Servicio para gestión de empresas.
 * Maneja operaciones CRUD y lógica de negocio relacionada con empresas.
 */
public interface EmpresaService {
    
    /**
     * Crea una nueva empresa en el sistema.
     * 
     * @param request datos de la nueva empresa
     * @return EmpresaDTO creada
     */
    EmpresaDTO crearEmpresa(CrearEmpresaRequest request);
    
    /**
     * Obtiene una empresa por su ID.
     * 
     * @param id ID de la empresa
     * @return EmpresaDTO encontrada
     */
    EmpresaDTO obtenerPorId(Long id);
    
    /**
     * Obtiene una empresa por su código único.
     * 
     * @param codigo código de la empresa
     * @return EmpresaDTO encontrada
     */
    EmpresaDTO obtenerPorCodigo(String codigo);
    
    /**
     * Actualiza la información de una empresa.
     * 
     * @param id ID de la empresa
     * @param empresaDTO datos actualizados
     * @return EmpresaDTO actualizada
     */
    EmpresaDTO actualizarEmpresa(Long id, EmpresaDTO empresaDTO);
    
    /**
     * Lista todas las empresas accesibles por un usuario.
     * 
     * @param usuarioId ID del usuario
     * @return Lista de empresas donde el usuario tiene acceso
     */
    List<EmpresaDTO> listarEmpresasPorUsuario(Long usuarioId);
    
    /**
     * Lista todas las empresas con paginación (solo SUPER_ADMIN).
     * 
     * @param activas filtrar solo activas (opcional)
     * @param search término de búsqueda (opcional)
     * @param pageable configuración de paginación
     * @return Página de empresas
     */
    Page<EmpresaDTO> listarEmpresas(Boolean activas, String search, Pageable pageable);
    
    /**
     * Activa o desactiva una empresa.
     * 
     * @param id ID de la empresa
     * @param activa nuevo estado
     * @return EmpresaDTO actualizada
     */
    EmpresaDTO cambiarEstadoEmpresa(Long id, boolean activa);
    
    /**
     * Obtiene la configuración de una empresa.
     * 
     * @param empresaId ID de la empresa
     * @return ConfiguracionEmpresaDTO con toda la configuración
     */
    ConfiguracionEmpresaDTO obtenerConfiguracion(Long empresaId);
    
    /**
     * Actualiza la configuración de una empresa.
     * 
     * @param empresaId ID de la empresa
     * @param configuracion nueva configuración
     * @return ConfiguracionEmpresaDTO actualizada
     */
    ConfiguracionEmpresaDTO actualizarConfiguracion(Long empresaId, 
                                                   ConfiguracionEmpresaDTO configuracion);
    
    /**
     * Verifica si existe una empresa con el código dado.
     * 
     * @param codigo código a verificar
     * @return true si existe
     */
    boolean existePorCodigo(String codigo);
    
    /**
     * Verifica si existe una empresa con la cédula jurídica dada.
     * 
     * @param cedulaJuridica cédula a verificar
     * @return true si existe
     */
    boolean existePorCedulaJuridica(String cedulaJuridica);
    
    /**
     * Obtiene estadísticas básicas de una empresa.
     * 
     * @param empresaId ID de la empresa
     * @return EstadisticasEmpresaDTO con métricas básicas
     */
    EstadisticasEmpresaDTO obtenerEstadisticas(Long empresaId);
    
    /**
     * Valida si un usuario tiene acceso a una empresa.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa
     * @return true si tiene acceso
     */
    boolean usuarioTieneAcceso(Long usuarioId, Long empresaId);
}