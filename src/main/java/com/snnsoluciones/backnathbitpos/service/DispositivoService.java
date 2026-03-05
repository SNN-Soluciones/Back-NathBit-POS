package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.dispositivo.*;
import com.snnsoluciones.backnathbitpos.entity.global.Dispositivo;

import java.util.List;
import java.util.Optional;

public interface DispositivoService {

    GenerarTokenResponse generarTokenRegistro(GenerarTokenRequest request);

    RegistrarDispositivoResponse registrarDispositivo(RegistrarDispositivoRequest request, String ipCliente);

    DispositivoUsuariosResponse obtenerUsuariosDispositivo(String deviceToken, Boolean includeRoot);

    Optional<Dispositivo> buscarPorToken(String deviceToken);

    Optional<Dispositivo> buscarActivoPorToken(String deviceToken);

    List<DispositivoDTO> listarDispositivosPorEmpresa(Long empresaId);

    List<SucursalSimpleDTO> listarSucursalesPorEmpresa(Long empresaId);

    void activarDispositivo(Long id);

    void desactivarDispositivo(Long id);

    void registrarUso(Dispositivo dispositivo);
}