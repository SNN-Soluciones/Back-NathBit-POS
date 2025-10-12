package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.familia.ActualizarFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.CrearFamiliaProductoRequest;
import com.snnsoluciones.backnathbitpos.dto.familia.FamiliaProductoDTO;

import java.util.List;

/**
 * Servicio para gestión de Familias de Productos
 * Soporta familias globales (empresa) y específicas por sucursal
 */
public interface FamiliaProductoService {

    /**
     * Listar familias por empresa y opcionalmente por sucursal
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (0 o null = solo globales de empresa)
     * @return Lista de familias ordenadas por 'orden'
     */
    List<FamiliaProductoDTO> listarPorEmpresaYSucursal(Long empresaId, Long sucursalId);

    /**
     * Listar solo familias activas
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (0 o null = solo globales de empresa)
     * @return Lista de familias activas ordenadas por 'orden'
     */
    List<FamiliaProductoDTO> listarActivasPorEmpresaYSucursal(Long empresaId, Long sucursalId);

    /**
     * Buscar familias por nombre o código
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (0 o null = solo globales de empresa)
     * @param busqueda Término de búsqueda
     * @return Lista de familias que coinciden con la búsqueda
     */
    List<FamiliaProductoDTO> buscarPorEmpresaYSucursal(Long empresaId, Long sucursalId, String busqueda);

    /**
     * Obtener una familia por ID
     * @param id ID de la familia
     * @param empresaId ID de la empresa (validación de seguridad)
     * @param sucursalId ID de la sucursal (validación de seguridad)
     * @return DTO de la familia
     */
    FamiliaProductoDTO obtenerPorId(Long id, Long empresaId, Long sucursalId);

    /**
     * Crear una nueva familia
     * @param request Datos de la familia a crear
     * @param empresaId ID de la empresa
     * @param sucursalId ID de la sucursal (0 o null = familia global)
     * @return DTO de la familia creada
     */
    FamiliaProductoDTO crear(CrearFamiliaProductoRequest request, Long empresaId, Long sucursalId);

    /**
     * Actualizar una familia existente
     * @param id ID de la familia a actualizar
     * @param request Datos actualizados
     * @param empresaId ID de la empresa (validación de seguridad)
     * @param sucursalId ID de la sucursal (validación de seguridad)
     * @return DTO de la familia actualizada
     */
    FamiliaProductoDTO actualizar(Long id, ActualizarFamiliaProductoRequest request, Long empresaId, Long sucursalId);

    /**
     * Eliminar una familia
     * @param id ID de la familia a eliminar
     * @param empresaId ID de la empresa (validación de seguridad)
     * @param sucursalId ID de la sucursal (validación de seguridad)
     */
    void eliminar(Long id, Long empresaId, Long sucursalId);

    /**
     * Cambiar el estado activo/inactivo de una familia
     * @param id ID de la familia
     * @param activa true para activar, false para desactivar
     * @param empresaId ID de la empresa (validación de seguridad)
     * @param sucursalId ID de la sucursal (validación de seguridad)
     * @return DTO de la familia actualizada
     */
    FamiliaProductoDTO cambiarEstado(Long id, Boolean activa, Long empresaId, Long sucursalId);
}