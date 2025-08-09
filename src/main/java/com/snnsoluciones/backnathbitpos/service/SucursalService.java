package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;

import java.util.List;
import java.util.Optional;

public interface SucursalService {

    // Métodos existentes
    Sucursal crear(Sucursal sucursal);
    Sucursal actualizar(Long id, Sucursal sucursal);
    Optional<Sucursal> buscarPorId(Long id);
    Optional<Sucursal> buscarPorCodigo(String codigo);
    List<Sucursal> listarPorEmpresa(Long empresaId);
    List<Sucursal> listarTodas();
    void eliminar(Long id);
    boolean existeCodigo(String codigo);
    List<Sucursal> listarPorUsuario(Long usuarioId);
    List<Sucursal> listarPorUsuarioYEmpresa(Long usuarioId, Long empresaId);

    // Gestión de terminales
    Terminal crearTerminal(Long sucursalId, Terminal terminal);
    List<Terminal> listarTerminales(Long sucursalId);
    List<Terminal> listarTerminalesActivas(Long sucursalId);

    // Consultas especiales
    Optional<Sucursal> buscarConTerminales(Long id);
    boolean puedeFacturarElectronicamente(Long sucursalId);
}