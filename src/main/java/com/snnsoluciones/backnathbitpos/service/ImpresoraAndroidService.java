package com.snnsoluciones.backnathbitpos.service;


import com.snnsoluciones.backnathbitpos.dto.impresion.ImpresoraAndroidDTO;
import com.snnsoluciones.backnathbitpos.entity.ImpresoraAndroid.TipoUsoImpresora;
import java.util.List;

public interface ImpresoraAndroidService {

    /**
     * Crear una nueva impresora Android
     * @param dto Datos de la impresora
     * @param usuarioId ID del usuario que crea
     * @return Impresora creada
     */
    ImpresoraAndroidDTO crear(ImpresoraAndroidDTO dto, Long usuarioId);

    /**
     * Actualizar una impresora existente
     * @param id ID de la impresora
     * @param dto Datos actualizados
     * @param usuarioId ID del usuario que actualiza
     * @return Impresora actualizada
     */
    ImpresoraAndroidDTO actualizar(Long id, ImpresoraAndroidDTO dto, Long usuarioId);

    /**
     * Eliminar una impresora
     * @param id ID de la impresora
     */
    void eliminar(Long id);

    /**
     * Obtener impresora por ID
     * @param id ID de la impresora
     * @return Impresora encontrada
     */
    ImpresoraAndroidDTO obtenerPorId(Long id);

    /**
     * Listar todas las impresoras de una sucursal
     * @param sucursalId ID de la sucursal
     * @return Lista de impresoras
     */
    List<ImpresoraAndroidDTO> listarPorSucursal(Long sucursalId);

    /**
     * Listar solo impresoras activas de una sucursal
     * @param sucursalId ID de la sucursal
     * @return Lista de impresoras activas
     */
    List<ImpresoraAndroidDTO> listarActivasPorSucursal(Long sucursalId);

    /**
     * Listar impresoras por tipo de uso
     * @param sucursalId ID de la sucursal
     * @param tipoUso Tipo de uso (FACTURAS, COCINA, BARRA, GENERAL)
     * @return Lista de impresoras del tipo especificado
     */
    List<ImpresoraAndroidDTO> listarPorTipoUso(Long sucursalId, TipoUsoImpresora tipoUso);

    /**
     * Obtener impresora predeterminada por tipo de uso
     * @param sucursalId ID de la sucursal
     * @param tipoUso Tipo de uso
     * @return Impresora predeterminada o null
     */
    ImpresoraAndroidDTO obtenerPredeterminada(Long sucursalId, TipoUsoImpresora tipoUso);

    /**
     * Establecer una impresora como predeterminada
     * (Quita el flag de otras impresoras del mismo tipo de uso)
     * @param id ID de la impresora
     * @param usuarioId ID del usuario que actualiza
     * @return Impresora actualizada
     */
    ImpresoraAndroidDTO establecerComoPredeterminada(Long id, Long usuarioId);

    /**
     * Activar/Desactivar una impresora
     * @param id ID de la impresora
     * @param activa true para activar, false para desactivar
     * @param usuarioId ID del usuario que actualiza
     * @return Impresora actualizada
     */
    ImpresoraAndroidDTO cambiarEstado(Long id, Boolean activa, Long usuarioId);
}