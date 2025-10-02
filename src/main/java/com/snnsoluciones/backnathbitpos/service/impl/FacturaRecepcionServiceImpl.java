//package com.snnsoluciones.backnathbitpos.service.impl;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.snnsoluciones.backnathbitpos.dto.mailreceptor.BuscarSucursalResponse;
//import com.snnsoluciones.backnathbitpos.entity.Empresa;
//import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcionAutomatica;
//import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcionDetalle;
//import com.snnsoluciones.backnathbitpos.entity.Sucursal;
//import com.snnsoluciones.backnathbitpos.entity.SucursalReceptorSmtp;
//import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaRecepcionAutomaticaRepository;
//import com.snnsoluciones.backnathbitpos.repository.FacturaRecepcionDetalleRepository;
//import com.snnsoluciones.backnathbitpos.repository.SucursalReceptorSmtpRepository;
//import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
//import com.snnsoluciones.backnathbitpos.service.FacturaRecepcionService;
//import com.snnsoluciones.backnathbitpos.service.StorageService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class FacturaRecepcionServiceImpl implements FacturaRecepcionService {
//
//    private final EmpresaRepository empresaRepository;
//    private final SucursalRepository sucursalRepository;
//    private final SucursalReceptorSmtpRepository sucursalReceptorSmtpRepository;
//    private final FacturaRecepcionAutomaticaRepository facturaRecepcionRepository;
//    private final FacturaRecepcionDetalleRepository detalleRepository;
//    private final StorageService storageService;
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public BuscarSucursalResponse buscarSucursalPorCedulaEmail(String cedula, String email) {
//        try {
//            // Primero buscar la empresa por cédula
//            Optional<Empresa> empresaOpt = empresaRepository.findByIdentificacion(cedula);
//
//            if (empresaOpt.isEmpty()) {
//                log.warn("No se encontró empresa con cédula: {}", cedula);
//                return BuscarSucursalResponse.notFound();
//            }
//
//            Empresa empresa = empresaOpt.get();
//
//            // Buscar sucursal que tenga configurado ese email receptor
//            Optional<SucursalReceptorSmtp> receptorOpt =
//                sucursalReceptorSmtpRepository.findByEmpresaIdAndEmailReceptor(empresa.getId(), email);
//
//            if (receptorOpt.isEmpty()) {
//                log.warn("No se encontró sucursal con email receptor: {} para empresa: {}",
//                    email, empresa.getNombreRazonSocial());
//                return BuscarSucursalResponse.notFound();
//            }
//
//            SucursalReceptorSmtp receptor = receptorOpt.get();
//            Sucursal sucursal = receptor.getSucursal();
//
//            return BuscarSucursalResponse.found(
//                    sucursal.getId(),
//                    empresa.getId(),
//                    sucursal.getNombre(),
//                    empresa.getNombreComercial(),
//                    empresa.getNombreRazonSocial()
//            );
//
//        } catch (Exception e) {
//            log.error("Error buscando sucursal: ", e);
//            return BuscarSucursalResponse.notFound();
//        }
//    }
//
//    @Override
//    @Transactional
//    public ProcesarFacturaResponse procesarFactura(Long sucursalId, String xmlContent,
//                                                  MultipartFile xmlFile, MultipartFile pdfFile,
//                                                  String datosFacturaJson) {
//        try {
//            // 1. Parsear el JSON con todos los datos de la factura
//            JsonNode datosFactura = objectMapper.readTree(datosFacturaJson);
//
//            // 2. Obtener información clave
//            String clave = datosFactura.get("clave").asText();
//            String tipoDocumento = obtenerTipoDocumento(clave);
//
//            // 3. Verificar que no existe ya
//            if (facturaRecepcionRepository.existsByClave(clave)) {
//                log.warn("Factura ya existe con clave: {}", clave);
//                return ProcesarFacturaResponse.error("La factura ya fue procesada anteriormente");
//            }
//
//            // 4. Obtener sucursal y empresa para S3
//            Sucursal sucursal = sucursalRepository.findById(sucursalId)
//                    .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
//            Empresa empresa = sucursal.getEmpresa();
//
//            // 5. Subir archivos a S3
//            String s3Path = generarPathS3(empresa.getNombreRazonSocial(), tipoDocumento, clave);
//            String s3XmlUrl = storageService.uploadFile(xmlFile, s3Path + ".xml");
//            String s3PdfUrl = storageService.uploadFile(pdfFile, s3Path + ".pdf");
//
//            // 6. Crear entidad principal
//            FacturaRecepcionAutomatica factura = new FacturaRecepcionAutomatica();
//            mapearDatosFactura(factura, datosFactura, sucursal);
//            factura.setRutaXmlS3(s3XmlUrl);
//            factura.setRutaPdfS3(s3PdfUrl);
//
//            // 7. Guardar factura
//            factura = facturaRecepcionRepository.save(factura);
//
//            // 8. Procesar y guardar detalles
//            procesarDetalles(factura, datosFactura.get("detalles"));
//
//            // 9. Procesar impuestos totales si existen
//            procesarImpuestosTotales(factura, datosFactura.get("resumen"));
//
//            // 10. TODO: Validar con Hacienda (cuando esté implementado)
//            // Por ahora marcamos como pendiente de validación
//            factura.setEstadoHacienda("PENDIENTE_VALIDACION");
//            facturaRecepcionRepository.save(factura);
//
//            log.info("Factura procesada exitosamente - ID: {}, Clave: {}", factura.getId(), clave);
//
//            return ProcesarFacturaResponse.success(factura.getId(), clave, s3XmlUrl, s3PdfUrl);
//
//        } catch (Exception e) {
//            log.error("Error procesando factura: ", e);
//            return ProcesarFacturaResponse.error("Error procesando factura: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public Page<?> obtenerFacturasPendientes(Long empresaId, Long sucursalId, int page, int size) {
//        PageRequest pageRequest = PageRequest.of(page, size);
//
//        if (sucursalId != null) {
//            return facturaRecepcionRepository.findBySucursalIdAndConvertidaACompra(sucursalId, false, pageRequest);
//        } else if (empresaId != null) {
//            return facturaRecepcionRepository.findByEmpresaIdAndConvertidaACompra(empresaId, false, pageRequest);
//        } else {
//            return facturaRecepcionRepository.findByConvertidaACompra(false, pageRequest);
//        }
//    }
//
//    // Métodos auxiliares privados
//
//    private String obtenerTipoDocumento(String clave) {
//        // Los dígitos 10-11 de la clave indican el tipo de documento
//        if (clave.length() >= 11) {
//            String tipo = clave.substring(9, 11);
//            switch (tipo) {
//                case "01": return "FE";  // Factura Electrónica
//                case "02": return "ND";  // Nota de Débito
//                case "03": return "NC";  // Nota de Crédito
//                case "04": return "TE";  // Tiquete Electrónico
//                case "08": return "FEC"; // Factura Electrónica de Compra
//                case "09": return "FEE"; // Factura Electrónica de Exportación
//                default: return "OTRO";
//            }
//        }
//        return "DESCONOCIDO";
//    }
//
//    private String generarPathS3(String razonSocial, String tipoDocumento, String clave) {
//        // Formato: /nathbit-facturas/{razon_social}/compras/{tipo}/{año}/{mes}/{clave}
//        LocalDateTime ahora = LocalDateTime.now();
//        String razonSocialLimpia = razonSocial.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
//
//        return String.format("nathbit-facturas/%s/compras/%s/%d/%02d/%s",
//                razonSocialLimpia,
//                tipoDocumento,
//                ahora.getYear(),
//                ahora.getMonthValue(),
//                clave
//        );
//    }
//
//    private void mapearDatosFactura(FacturaRecepcionAutomatica factura, JsonNode datos, Sucursal sucursal) {
//        // Mapear información básica
//        factura.setSucursal(sucursal);
//        factura.setEmpresa(sucursal.getEmpresa());
//        factura.setClaveHacienda(datos.get("clave").asText());
//        factura.setCodigoActividadEmisor(datos.get("codigoActividad").asText());
//        factura.setNumeroConsecutivo(datos.get("numeroConsecutivo").asText());
//
//        // Fecha emisión
//        String fechaEmisionStr = datos.get("fechaEmision").asText();
//        factura.setFechaEmision(LocalDateTime.parse(fechaEmisionStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
//
//        // Emisor
//        JsonNode emisor = datos.get("emisor");
//        factura.setEmisorNombre(emisor.get("nombre").asText());
//        factura.setEmisorTipoIdentificacion(emisor.get("tipoIdentificacion").asText());
//        factura.setEmisorNumeroIdentificacion(emisor.get("numeroIdentificacion").asText());
//        if (emisor.has("nombreComercial")) {
//            factura.setEmisorNombreComercial(emisor.get("nombreComercial").asText());
//        }
//        factura.setEmisorProvincia(emisor.get("provincia").asText());
//        factura.setEmisorCanton(emisor.get("canton").asText());
//        factura.setEmisorDistrito(emisor.get("distrito").asText());
//        if (emisor.has("otrasSenas")) {
//            factura.setEmisorOtrasSenas(emisor.get("otrasSenas").asText());
//        }
//        if (emisor.has("telefono")) {
//            factura.setEmisorTelefonoNumero(emisor.get("telefono").asText());
//        }
//        if (emisor.has("correoElectronico")) {
//            factura.setEmisorCorreo(emisor.get("correoElectronico").asText());
//        }
//
//        // Receptor
//        JsonNode receptor = datos.get("receptor");
//        factura.setReceptorNombre(receptor.get("nombre").asText());
//        factura.setReceptorTipoIdentificacion(receptor.get("tipoIdentificacion").asText());
//        factura.setReceptorNumeroIdentificacion(receptor.get("numeroIdentificacion").asText());
//        if (receptor.has("telefono")) {
//            factura.setReceptorTelefonoNumero(receptor.get("telefono").asText());
//        }
//        if (receptor.has("correoElectronico")) {
//            factura.setReceptorCorreo(receptor.get("correoElectronico").asText());
//        }
//
//        // Condiciones comerciales
//        factura.setCondicionVenta(datos.get("condicionVenta").asText());
//        if (datos.has("plazoCredito")) {
//            factura.setPlazoCredito(Integer.parseInt(datos.get("plazoCredito").asText()));
//        }
//
//        // Resumen de montos
//        JsonNode resumen = datos.get("resumen");
//        factura.setCodigoMoneda(resumen.get("codigoTipoMoneda").asText());
//        factura.setTipoCambio(BigDecimal.valueOf(resumen.get("tipoCambio").asDouble()));
//
//        // Totales por tipo
//        factura.setTotalServGravados(getBigDecimalFromNode(resumen, "totalServGravados"));
//        factura.setTotalServExentos(getBigDecimalFromNode(resumen, "totalServExentos"));
//        factura.setTotalServExonerado(getBigDecimalFromNode(resumen, "totalServExonerado"));
//        factura.setTotalMercanciasGravadas(getBigDecimalFromNode(resumen, "totalMercanciasGravadas"));
//        factura.setTotalMercanciasExentas(getBigDecimalFromNode(resumen, "totalMercanciasExentas"));
////        factura.setTotalMercanciasExoneradas(getBigDecimalFromNode(resumen, "totalMercanciasExoneradas"));
//
//        // Totales generales
//        factura.setTotalGravado(getBigDecimalFromNode(resumen, "totalGravado"));
//        factura.setTotalExento(getBigDecimalFromNode(resumen, "totalExento"));
//        factura.setTotalExonerado(getBigDecimalFromNode(resumen, "totalExonerado"));
//        factura.setTotalVenta(getBigDecimalFromNode(resumen, "totalVenta"));
//        factura.setTotalDescuentos(getBigDecimalFromNode(resumen, "totalDescuentos"));
//        factura.setTotalVentaNeta(getBigDecimalFromNode(resumen, "totalVentaNeta"));
//        factura.setTotalImpuesto(getBigDecimalFromNode(resumen, "totalImpuesto"));
//        factura.setTotalComprobante(getBigDecimalFromNode(resumen, "totalComprobante"));
//
//        // Valores por defecto
//        factura.setFechaValidacion(LocalDateTime.now());
//    }
//
//    private BigDecimal getBigDecimalFromNode(JsonNode node, String field) {
//        if (node.has(field) && !node.get(field).isNull()) {
//            return BigDecimal.valueOf(node.get(field).asDouble());
//        }
//        return BigDecimal.ZERO;
//    }
//
//    private void procesarDetalles(FacturaRecepcionAutomatica factura, JsonNode detalles) {
//        if (detalles == null || !detalles.isArray()) {
//            return;
//        }
//
//        List<FacturaRecepcionDetalle> listaDetalles = new ArrayList<>();
//
//        for (JsonNode detalleNode : detalles) {
//            FacturaRecepcionDetalle detalle = new FacturaRecepcionDetalle();
//            detalle.setFacturaRecepcion(factura);
//            detalle.setNumeroLinea(detalleNode.get("numeroLinea").asInt());
//
//            if (detalleNode.has("codigo")) {
//                detalle.setCodigoComercialCodigo(detalleNode.get("codigo").asText());
//            }
//
//            // Código comercial
//            if (detalleNode.has("codigoComercial")) {
//                JsonNode codigoComercial = detalleNode.get("codigoComercial");
//                detalle.setCodigoComercialTipo(codigoComercial.get("tipo").asText());
//                detalle.setCodigoComercialCodigo(codigoComercial.get("codigo").asText());
//            }
//
//            detalle.setCantidad(getBigDecimalFromNode(detalleNode, "cantidad"));
//            detalle.setUnidadMedida(detalleNode.get("unidadMedida").asText());
//            detalle.setDetalle(detalleNode.get("detalle").asText());
//            detalle.setPrecioUnitario(getBigDecimalFromNode(detalleNode, "precioUnitario"));
//            detalle.setMontoTotal(getBigDecimalFromNode(detalleNode, "montoTotal"));
//            detalle.setSubTotal(getBigDecimalFromNode(detalleNode, "subTotal"));
//            detalle.setBaseImponible(getBigDecimalFromNode(detalleNode, "baseImponible"));
//            detalle.setMontoTotalLinea(getBigDecimalFromNode(detalleNode, "montoTotalLinea"));
//
//            // Procesar impuestos del detalle
//            if (detalleNode.has("impuesto")) {
//                procesarImpuestosDetalle(detalle, detalleNode.get("impuesto"));
//            }
//
//            listaDetalles.add(detalle);
//        }
//
//        // Guardar todos los detalles
//        detalleRepository.saveAll(listaDetalles);
//    }
//
//    private void procesarImpuestosDetalle(FacturaRecepcionDetalle detalle, JsonNode impuestos) {
//        // TODO: Implementar cuando se cree la entidad FacturaRecepcionDetalleImpuesto
//        // Por ahora solo calculamos el total
//        BigDecimal totalImpuestos = BigDecimal.ZERO;
//
//        if (impuestos.isArray()) {
//            for (JsonNode impuesto : impuestos) {
//                totalImpuestos = totalImpuestos.add(getBigDecimalFromNode(impuesto, "monto"));
//            }
//        }
//
//        // Guardamos temporalmente en algún campo o lo dejamos para cuando se implemente
//    }
//
//    private void procesarImpuestosTotales(FacturaRecepcionAutomatica factura, JsonNode resumen) {
//        // TODO: Implementar cuando se cree la entidad FacturaRecepcionImpuestoTotal
//        // Por ahora los totales ya están guardados en la factura principal
//    }
//}