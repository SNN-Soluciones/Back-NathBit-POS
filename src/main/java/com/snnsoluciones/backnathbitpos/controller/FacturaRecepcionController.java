//package com.snnsoluciones.backnathbitpos.controller;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.snnsoluciones.backnathbitpos.dto.mailreceptor.BuscarSucursalResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//@Slf4j
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "*", allowedHeaders = "*")
//public class FacturaRecepcionController {
//
//    private final FacturaRecepcionService facturaRecepcionService;
//
//    /**
//     * Endpoint A: Buscar sucursal por cédula de empresa y email receptor
//     */
//    @GetMapping("/sucursales/buscar-por-cedula-email")
//    public ResponseEntity<BuscarSucursalResponse> buscarSucursalPorCedulaEmail(
//            @RequestParam("cedula") String cedula,
//            @RequestParam("email") String email) {
//
//        log.info("Buscando sucursal para cédula: {} y email: {}", cedula, email);
//
//        try {
//            BuscarSucursalResponse response = facturaRecepcionService.buscarSucursalPorCedulaEmail(cedula, email);
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            log.error("Error buscando sucursal: ", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(BuscarSucursalResponse.notFound());
//        }
//    }
//
//    /**
//     * Endpoint B: Procesar factura recibida por email
//     * Recibe el XML, PDF y todos los datos parseados del XML
//     */
//    @PostMapping(value = "/facturas-recepcion/procesar",
//                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
//                 produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<ProcesarFacturaResponse> procesarFacturaRecepcion(
//            @RequestParam("sucursalId") Long sucursalId,
//            @RequestParam("xmlContent") String xmlContent,
//            @RequestParam("xmlFile") MultipartFile xmlFile,
//            @RequestParam("pdfFile") MultipartFile pdfFile,
//            @RequestParam("datosFactura") String datosFacturaJson) {
//
//        log.info("Procesando factura para sucursal: {}", sucursalId);
//        log.debug("Tamaño XML: {} bytes, PDF: {} bytes", xmlFile.getSize(), pdfFile.getSize());
//
//        try {
//            // Validaciones básicas
//            if (xmlFile.isEmpty() || pdfFile.isEmpty()) {
//                return ResponseEntity.badRequest()
//                        .body(ProcesarFacturaResponse.error("Los archivos XML y PDF son requeridos"));
//            }
//
//            // Procesar la factura
//            ProcesarFacturaResponse response = facturaRecepcionService.procesarFactura(
//                    sucursalId, xmlContent, xmlFile, pdfFile, datosFacturaJson);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("Error procesando factura: ", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ProcesarFacturaResponse.error("Error procesando factura: " + e.getMessage()));
//        }
//    }
//
//    /**
//     * Endpoint C (adicional): Obtener facturas pendientes de conversión
//     */
//    @GetMapping("/facturas-recepcion/pendientes")
//    public ResponseEntity<?> obtenerFacturasPendientes(
//            @RequestParam(required = false) Long empresaId,
//            @RequestParam(required = false) Long sucursalId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//
//        log.info("Obteniendo facturas pendientes - Empresa: {}, Sucursal: {}", empresaId, sucursalId);
//
//        try {
//            return ResponseEntity.ok(facturaRecepcionService.obtenerFacturasPendientes(
//                    empresaId, sucursalId, page, size));
//        } catch (Exception e) {
//            log.error("Error obteniendo facturas pendientes: ", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ProcesarFacturaResponse.error("Error obteniendo facturas: " + e.getMessage()));
//        }
//    }
//}