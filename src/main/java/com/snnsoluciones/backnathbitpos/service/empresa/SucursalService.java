package com.snnsoluciones.backnathbitpos.service.empresa;

import com.snnsoluciones.backnathbitpos.dto.empresa.ConfiguracionSucursalDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearSucursalRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.EstadisticasSucursalDTO;
import com.snnsoluciones.backnathbitpos.dto.empresa.SucursalDTO;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Servicio para gestión de sucursales.
 * Maneja operaciones CRUD y lógica de negocio relacionada con sucursales.
 */
public interface SucursalService {
    
    /**
     * Crea una nueva sucursal para una empresa.
     * 
     * @param empresaId ID de la empresa
     * @param request datos de la nueva sucursal
     * @return SucursalDTO creada
     */
    SucursalDTO crearSucursal(Long empresaId, CrearSucursalRequest request)
        throws BadRequestException;
    
    /**
     * Obtiene una sucursal por su ID.
     * 
     * @param id ID de la sucursal
     * @return SucursalDTO encontrada
     */
    SucursalDTO obtenerPorId(Long id);
    
    /**
     * Actualiza la información de una sucursal.
     * 
     * @param id ID de la sucursal
     * @param sucursalDTO datos actualizados
     * @return SucursalDTO actualizada
     */
    SucursalDTO actualizarSucursal(Long id, SucursalDTO sucursalDTO);
    
    /**
     * Lista todas las sucursales de una empresa.
     * 
     * @param empresaId ID de la empresa
     * @param activas filtrar solo activas (opcional)
     * @return Lista de sucursales
     */
    List<SucursalDTO> listarSucursalesPorEmpresa(Long empresaId, Boolean activas);
    
    /**
     * Lista sucursales accesibles por un usuario.
     * 
     * @param usuarioId ID del usuario
     * @param empresaId ID de la empresa (opcional)
     * @return Lista de sucursales donde el usuario tiene acceso
     */
    List<SucursalDTO> listarSucursalesPorUsuario(Long usuarioId, Long empresaId);
    
    /**
     * Lista sucursales con paginación.
     * 
     * @param empresaId ID de la empresa
     * @param search término de búsqueda (opcional)
     * @param pageable configuración de paginación
     * @return Página de sucursales
     */
    Page<SucursalDTO> listarSucursales(Long empresaId, String search, Pageable pageable);
    
    /**
     * Activa o desactiva una sucursal.
     * 
     * @param id ID de la sucursal
     * @param activa nuevo estado
     * @return SucursalDTO actualizada
     */
    SucursalDTO cambiarEstadoSucursal(Long id, boolean activa) throws BadRequestException;
    
    /**
     * Obtiene la configuración de una sucursal.
     * 
     * @param sucursalId ID de la sucursal
     * @return ConfiguracionSucursalDTO con toda la configuración
     */
    ConfiguracionSucursalDTO obtenerConfiguracion(Long sucursalId);
    
    /**
     * Actualiza la configuración de una sucursal.
     * 
     * @param sucursalId ID de la sucursal
     * @param configuracion nueva configuración
     * @return ConfiguracionSucursalDTO actualizada
     */
    ConfiguracionSucursalDTO actualizarConfiguracion(Long sucursalId, 
                                                    ConfiguracionSucursalDTO configuracion);
    
    /**
     * Establece una sucursal como principal de la empresa.
     * 
     * @param sucursalId ID de la sucursal
     */
    void establecerComoPrincipal(Long sucursalId);
    
    /**
     * Verifica si existe una sucursal con el código dado en la empresa.
     * 
     * @param codigo código a verificar
     * @param empresaId ID de la empresa
     * @return true si existe
     */
    boolean existePorCodigoYEmpresa(String codigo, Long empresaId);
    
    /**
     * Obtiene estadísticas básicas de una sucursal.
     * 
     * @param sucursalId ID de la sucursal
     * @return EstadisticasSucursalDTO con métricas básicas
     */
    EstadisticasSucursalDTO obtenerEstadisticas(Long sucursalId);
    
    /**
     * Valida si un usuario tiene acceso a una sucursal.
     * 
     * @param usuarioId ID del usuario
     * @param sucursalId ID de la sucursal
     * @return true si tiene acceso
     */
    boolean usuarioTieneAcceso(Long usuarioId, Long sucursalId);
    
    /**
     * Copia la configuración de una sucursal a otra.
     * Útil para crear sucursales con configuración similar.
     * 
     * @param sucursalOrigenId ID de la sucursal origen
     * @param sucursalDestinoId ID de la sucursal destino
     */
    void copiarConfiguracion(Long sucursalOrigenId, Long sucursalDestinoId)
        throws BadRequestException;
}