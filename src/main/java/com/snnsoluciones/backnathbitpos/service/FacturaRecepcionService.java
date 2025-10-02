//package com.snnsoluciones.backnathbitpos.service;
//
//import com.snnsoluciones.backnathbitpos.dto.mailreceptor.BuscarSucursalResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.web.multipart.MultipartFile;
//
//public interface FacturaRecepcionService {
//
//    /**
//     * Busca una sucursal por la cédula de la empresa y el email receptor configurado
//     */
//    BuscarSucursalResponse buscarSucursalPorCedulaEmail(String cedula, String email);
//
//    /**
//     * Procesa una factura recibida por email
//     */
//    ProcesarFacturaResponse procesarFactura(Long sucursalId, String xmlContent,
//                                           MultipartFile xmlFile, MultipartFile pdfFile,
//                                           String datosFacturaJson);
//
//    /**
//     * Obtiene las facturas pendientes de conversión a compra
//     */
//    Page<?> obtenerFacturasPendientes(Long empresaId, Long sucursalId, int page, int size);
//}