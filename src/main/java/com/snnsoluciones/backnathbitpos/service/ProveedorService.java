package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorRequest;
import java.util.List;

public interface ProveedorService {
    List<ProveedorDto> listarPorEmpresa(Long empresaId, String busqueda);
    ProveedorDto obtenerPorId(Long id);
    ProveedorDto buscarPorIdentificacion(Long empresaId, String numeroIdentificacion);
    ProveedorDto crear(ProveedorRequest request);
    ProveedorDto actualizar(Long id, ProveedorRequest request);
    ProveedorDto toggleActivo(Long id);
    void eliminar(Long id);
    List<ProveedorDto> listarPorEmpresaConContexto(Long empresaId, String busqueda);
}