package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.dispositivo.*;

import com.snnsoluciones.backnathbitpos.entity.DispositivoPdv;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de dispositivos PDV
 */
public interface DispositivoService {

    /**
     * Genera un token de registro para un nuevo PDV (Admin Web)
     *
     * @param request Datos del token a generar
     * @return Response con token, QR y link de registro
     */
    GenerarTokenResponse generarTokenRegistro(GenerarTokenRequest request);

    /**
     * Registra un dispositivo usando un token (PDV App)
     *
     * @param request Datos del dispositivo y token
     * @param ipCliente IP desde donde se registra
     * @return Response con deviceToken y datos de empresa/sucursal
     */
    RegistrarDispositivoResponse registrarDispositivo(RegistrarDispositivoRequest request, String ipCliente);

    /**
     * Obtiene lista de usuarios disponibles para login en el PDV
     *
     * @param deviceToken Token del dispositivo
     * @return Response con lista de usuarios
     */
    DispositivoUsuariosResponse obtenerUsuariosDispositivo(String deviceToken, Boolean includeRoot);

    /**
     * Busca un dispositivo por su token
     *
     * @param deviceToken Token del dispositivo
     * @return Optional con el dispositivo
     */
    Optional<DispositivoPdv> buscarPorToken(String deviceToken);

    /**
     * Busca un dispositivo activo por su token
     *
     * @param deviceToken Token del dispositivo
     * @return Optional con el dispositivo si está activo
     */
    Optional<DispositivoPdv> buscarActivoPorToken(String deviceToken);

    /**
     * Lista dispositivos de una empresa
     *
     * @param empresaId ID de la empresa
     * @return Lista de DTOs de dispositivos
     */
    List<DispositivoDTO> listarDispositivosPorEmpresa(Long empresaId);

    /**
     * Lista sucursales de una empresa (simplificado)
     *
     * @param empresaId ID de la empresa
     * @return Lista simple de sucursales
     */
    List<SucursalSimpleDTO> listarSucursalesPorEmpresa(Long empresaId);

    /**
     * Activa un dispositivo
     *
     * @param id ID del dispositivo
     */
    void activarDispositivo(Long id);

    /**
     * Desactiva un dispositivo (bloquea su acceso)
     *
     * @param id ID del dispositivo
     */
    void desactivarDispositivo(Long id);

    /**
     * Actualiza el timestamp de último uso del dispositivo
     *
     * @param dispositivo Dispositivo a actualizar
     */
    void registrarUso(DispositivoPdv dispositivo);
}