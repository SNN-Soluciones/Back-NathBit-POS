package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.cabys.CABySDto;
import com.snnsoluciones.backnathbitpos.dto.cabys.EmpresaCABySDto;
import java.util.List;

public interface EmpresaCABySService {
    List<CABySDto> buscarEnCatalogo(String impuesto, String busqueda);
    EmpresaCABySDto asignar(Long empresaId, Long codigoCabysId);
    List<EmpresaCABySDto> listarPorEmpresa(Long empresaId);
    void quitar(Long id);
}