package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;

import java.util.List;
import java.util.Optional;

public interface SucursalService {

    // Métodos existentes
    Optional<Sucursal> finById(Long id);
    Sucursal crear(Sucursal sucursal);
    Sucursal actualizar(Long id, Sucursal sucursal);
    Optional<Sucursal> buscarPorId(Long id);
    List<Sucursal> listarPorEmpresa(Long empresaId);
    List<Sucursal> listarTodas();
    void eliminar(Long id);
    boolean existeCodigo(Long codigo);
    List<Sucursal> listarPorUsuario(Long usuarioId);
    List<Sucursal> listarPorUsuarioYEmpresa(Long usuarioId, Long empresaId);
    Boolean existsNumeroSucursalYEmpresaId(String numeroSucursal, Long empresaId);

    // Gestión de terminales
    Terminal crearTerminal(Long sucursalId, Terminal terminal);
    List<Terminal> listarTerminales(Long sucursalId);
    List<Terminal> listarTerminalesActivas(Long sucursalId);

}