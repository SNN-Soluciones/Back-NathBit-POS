package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.familia.ActualizarFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.CrearFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.FamiliaProductoDTO;

import java.util.List;

/**
 * Servicio para gestión de Familias de Productos
 */
public interface FamiliaProductoService {
    
    /**
     * Listar todas las familias de una empresa
     * @param empresaId ID de la empresa
     * @return Lista de familias ordenadas por 'orden'
     */
    List<FamiliaProductoDTO> listarPorEmpresa(Long empresaId);
    
    /**
     * Listar solo familias activas de una empresa
     * @param empresaId ID de la empresa
     * @return Lista de familias activas ordenadas por 'orden'
     */
    List<FamiliaProductoDTO> listarActivasPorEmpresa(Long empresaId);
    
    /**
     * Buscar familias por nombre o código
     * @param empresaId ID de la empresa
     * @param busqueda Término de búsqueda
     * @return Lista de familias que coinciden con la búsqueda
     */
    List<FamiliaProductoDTO> buscarPorEmpresa(Long empresaId, String busqueda);
    
    /**
     * Obtener una familia por ID
     * @param id ID de la familia
     * @param empresaId ID de la empresa (validación de seguridad)
     * @return DTO de la familia
     */
    FamiliaProductoDTO obtenerPorId(Long id, Long empresaId);
    
    /**
     * Crear una nueva familia
     * @param request Datos de la familia a crear
     * @param empresaId ID de la empresa
     * @return DTO de la familia creada
     */
    FamiliaProductoDTO crear(CrearFamiliaProductoRequest request, Long empresaId);
    
    /**
     * Actualizar una familia existente
     * @param id ID de la familia a actualizar
     * @param request Datos actualizados
     * @param empresaId ID de la empresa (validación de seguridad)
     * @return DTO de la familia actualizada
     */
    FamiliaProductoDTO actualizar(Long id, ActualizarFamiliaProductoRequest request, Long empresaId);
    
    /**
     * Eliminar una familia (eliminación lógica)
     * @param id ID de la familia a eliminar
     * @param empresaId ID de la empresa (validación de seguridad)
     */
    void eliminar(Long id, Long empresaId);
    
    /**
     * Cambiar el estado activo/inactivo de una familia
     * @param id ID de la familia
     * @param activa true para activar, false para desactivar
     * @param empresaId ID de la empresa (validación de seguridad)
     * @return DTO de la familia actualizada
     */
    FamiliaProductoDTO cambiarEstado(Long id, Boolean activa, Long empresaId);
}