package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.compuestoV2.ProductoCompuestoV2Dto;
import com.snnsoluciones.backnathbitpos.dto.compuestoV2.ProductoCompuestoV2Request;

public interface ProductoCompuestoV2Service {
    ProductoCompuestoV2Dto crear(Long empresaId, Long productoId, ProductoCompuestoV2Request request);
    ProductoCompuestoV2Dto actualizar(Long empresaId, Long productoId, ProductoCompuestoV2Request request);
    ProductoCompuestoV2Dto obtener(Long empresaId, Long productoId);
    void eliminar(Long empresaId, Long productoId);
}