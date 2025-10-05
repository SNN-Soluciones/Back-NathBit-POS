package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;

import java.util.List;
import java.util.Optional;

public interface TerminalService {
    
    Terminal crear(Terminal terminal);
    Terminal actualizar(Long id, Terminal terminal);
    Optional<Terminal> buscarPorId(Long id);
    List<Terminal> listarPorSucursal(Long sucursalId);
    void eliminar(Long id);
    
    // Gestión de consecutivos
    Long obtenerSiguienteConsecutivo(Long terminalId, TipoDocumento tipoDocumento);
    String generarNumeroConsecutivo(Long terminalId, TipoDocumento tipoDocumento);
    
    // Validaciones
    boolean estaOcupada(Long terminalId);
}