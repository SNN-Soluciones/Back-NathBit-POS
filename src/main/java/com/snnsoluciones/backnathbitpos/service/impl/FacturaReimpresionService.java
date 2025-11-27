package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaReimpresionDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaReimpresionDto.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.repository.FacturaBitacoraRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para obtener datos completos de factura para reimpresión
 * Incluye: emisor, cliente, detalles, otros cargos, exoneraciones, referencias, medios de pago
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaReimpresionService {

    private final FacturaRepository facturaRepository;
    private final FacturaBitacoraRepository bitacoraRepository;

    /**
     * Obtiene todos los datos necesarios para reimprimir una factura electrónica
     * 
     * @param clave Clave de 50 dígitos del documento
     * @return DTO con todos los datos para generar ESC/POS
     */
    @Transactional(readOnly = true)
    public FacturaReimpresionDto obtenerDatosReimpresion(String clave) {
        log.info("🔍 Obteniendo datos de reimpresión para clave: {}", clave);

        // 1. Buscar factura con todas sus relaciones
        Factura factura = facturaRepository.findByClaveWithAllRelations(clave)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada con clave: " + clave));

        // 2. Buscar mensaje de Hacienda en bitácora
        String mensajeHacienda = bitacoraRepository.findByClave(clave)
            .map(FacturaBitacora::getHaciendaMensaje)
            .orElse(null);

        // 3. Construir DTO
        return construirDto(factura, mensajeHacienda);
    }

    /**
     * Construye el DTO completo desde la entidad Factura
     */
    private FacturaReimpresionDto construirDto(Factura factura, String mensajeHacienda) {
        Empresa empresa = factura.getSucursal().getEmpresa();
        Cliente cliente = factura.getCliente();

        return FacturaReimpresionDto.builder()
            // Identificación
            .clave(factura.getClave())
            .consecutivo(factura.getConsecutivo())
            .tipoDocumento(factura.getTipoDocumento().getCodigo())
            .tipoDocumentoNombre(getNombreTipoDocumento(factura.getTipoDocumento()))
            .fechaEmision(factura.getFechaEmision())
            .estado(factura.getEstado().name())
            .condicionVenta(factura.getCondicionVenta().name())
            .plazoCredito(factura.getPlazoCredito())
            .moneda(factura.getMoneda().getCodigo())
            .tipoCambio(factura.getTipoCambio())

            // Emisor
            .empresaNombre(empresa.getNombreRazonSocial())
            .empresaNombreComercial(empresa.getNombreComercial())
            .empresaCedula(empresa.getIdentificacion())
            .empresaTelefono(empresa.getTelefono())
            .empresaCorreo(empresa.getEmail())
            .empresaDireccion(construirDireccionEmpresa(empresa))
            .sucursalNombre(factura.getSucursal().getNombre())

            // Cliente
            .clienteNombre(cliente != null ? cliente.getRazonSocial() : factura.getNombreReceptor())
            .clienteCedula(cliente != null ? cliente.getNumeroIdentificacion() : null)
            .clienteEmail(factura.getEmailReceptor())
            .clienteTelefono(cliente != null ? cliente.getTelefonoNumero() : null)
            .clienteDireccion(cliente != null ? construirDireccionCliente(cliente) : null)
            .clienteActividadEconomica(factura.getActividadReceptor())

            // Detalles
            .detalles(construirDetalles(factura.getDetalles()))

            // Otros cargos
            .otrosCargos(construirOtrosCargos(factura.getOtrosCargos()))

            // Exoneraciones (extraídas de los detalles)
            .exoneraciones(extraerExoneraciones(factura.getDetalles()))

            // Referencias (para NC/ND)
            .informacionReferencia(construirReferencias(factura))

            // Medios de pago
            .mediosPago(construirMediosPago(factura.getMediosPago()))

            // Totales
            .totalServiciosGravados(factura.getTotalServiciosGravados())
            .totalServiciosExentos(factura.getTotalServiciosExentos())
            .totalServiciosExonerados(factura.getTotalServiciosExonerados())
            .totalMercanciasGravadas(factura.getTotalMercanciasGravadas())
            .totalMercanciasExentas(factura.getTotalMercanciasExentas())
            .totalMercanciasExoneradas(factura.getTotalMercanciasExoneradas())
            .totalGravado(factura.getTotalGravado())
            .totalExento(factura.getTotalExento())
            .totalExonerado(factura.getTotalExonerado())
            .totalVenta(factura.getTotalVenta())
            .totalDescuentos(factura.getTotalDescuentos())
            .totalVentaNeta(factura.getTotalVentaNeta())
            .totalImpuesto(factura.getTotalImpuesto())
            .totalOtrosCargos(factura.getTotalOtrosCargos())
            .totalComprobante(factura.getTotalComprobante())
            .vuelto(factura.getVuelto())

            // Hacienda
            .mensajeHacienda(mensajeHacienda)
            .build();
    }

    // ========== MÉTODOS AUXILIARES ==========

    private List<DetalleReimpresionDto> construirDetalles(List<FacturaDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return Collections.emptyList();
        }

        return detalles.stream()
            .map(this::construirDetalle)
            .collect(Collectors.toList());
    }

    private DetalleReimpresionDto construirDetalle(FacturaDetalle detalle) {
        // Buscar si tiene exoneración en alguno de sus impuestos
        BigDecimal montoExoneracion = BigDecimal.ZERO;
        String institucionExoneracion = null;
        boolean tieneExoneracion = false;

        if (detalle.getImpuestos() != null) {
            for (FacturaDetalleImpuesto impuesto : detalle.getImpuestos()) {
                if (Boolean.TRUE.equals(impuesto.getTieneExoneracion())) {
                    tieneExoneracion = true;
                    if (impuesto.getMontoExoneracion() != null) {
                        montoExoneracion = montoExoneracion.add(impuesto.getMontoExoneracion());
                    }
                    if (impuesto.getNombreInstitucion() != null) {
                        institucionExoneracion = impuesto.getNombreInstitucion();
                    }
                }
            }
        }

        return DetalleReimpresionDto.builder()
            .numeroLinea(detalle.getNumeroLinea())
            .codigo(detalle.getProducto() != null ? detalle.getProducto().getCodigoInterno() : null)
            .descripcion(detalle.getDetalle())
            .descripcionPersonalizada(detalle.getDescripcionPersonalizada())
            .cantidad(detalle.getCantidad())
            .unidadMedida(detalle.getUnidadMedida())
            .precioUnitario(detalle.getPrecioUnitario())
            .montoDescuento(detalle.getMontoDescuento())
            .subtotal(detalle.getSubtotal())
            .montoImpuesto(detalle.getMontoImpuesto())
            .montoTotalLinea(detalle.getMontoTotalLinea())
            .tieneExoneracion(tieneExoneracion)
            .montoExoneracion(montoExoneracion)
            .institucionExoneracion(institucionExoneracion)
            .build();
    }

    private List<OtroCargoReimpresionDto> construirOtrosCargos(List<OtroCargo> otrosCargos) {
        if (otrosCargos == null || otrosCargos.isEmpty()) {
            return Collections.emptyList();
        }

        return otrosCargos.stream()
            .map(cargo -> OtroCargoReimpresionDto.builder()
                .tipoDocumento(cargo.getTipoDocumentoOC())
                .detalle(cargo.getNombreCargo())
                .porcentaje(cargo.getPorcentaje())
                .monto(cargo.getMontoCargo())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Extrae las exoneraciones únicas de todos los detalles
     * para mostrar un resumen en el ticket
     */
    private List<ExoneracionReimpresionDto> extraerExoneraciones(List<FacturaDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return Collections.emptyList();
        }

        // Usar un Map para agrupar por número de documento (evitar duplicados)
        Map<String, ExoneracionReimpresionDto> exoneracionesMap = new LinkedHashMap<>();

        for (FacturaDetalle detalle : detalles) {
            if (detalle.getImpuestos() == null) continue;

            for (FacturaDetalleImpuesto impuesto : detalle.getImpuestos()) {
                if (Boolean.TRUE.equals(impuesto.getTieneExoneracion()) 
                    && impuesto.getNumeroDocumentoExoneracion() != null) {
                    
                    String key = impuesto.getNumeroDocumentoExoneracion();
                    
                    if (!exoneracionesMap.containsKey(key)) {
                        exoneracionesMap.put(key, ExoneracionReimpresionDto.builder()
                            .tipoDocumento(impuesto.getTipoDocumentoExoneracion())
                            .numeroDocumento(impuesto.getNumeroDocumentoExoneracion())
                            .nombreInstitucion(impuesto.getNombreInstitucion())
                            .fechaEmision(impuesto.getFechaEmisionExoneracion())
                            .porcentajeExoneracion(impuesto.getTarifaExonerada())
                            .montoExoneracion(impuesto.getMontoExoneracion())
                            .build());
                    } else {
                        // Sumar el monto si es el mismo documento
                        ExoneracionReimpresionDto existing = exoneracionesMap.get(key);
                        if (impuesto.getMontoExoneracion() != null && existing.getMontoExoneracion() != null) {
                            existing.setMontoExoneracion(
                                existing.getMontoExoneracion().add(impuesto.getMontoExoneracion())
                            );
                        }
                    }
                }
            }
        }

        return new ArrayList<>(exoneracionesMap.values());
    }

    private List<ReferenciaReimpresionDto> construirReferencias(Factura factura) {
        // Solo para NC/ND que tienen referencia
        if (factura.getTipoDocReferencia() == null || factura.getNumeroReferencia() == null) {
            return Collections.emptyList();
        }

        return List.of(ReferenciaReimpresionDto.builder()
            .tipoDocumento(factura.getTipoDocReferencia().getCodigo())
            .tipoDocumentoNombre(getNombreTipoDocumento(factura.getTipoDocReferencia()))
            .numero(factura.getNumeroReferencia())
            .fechaEmision(factura.getFechaEmisionReferencia())
            .codigo(factura.getCodigoReferencia())
            .codigoNombre(getNombreCodigoReferencia(factura.getCodigoReferencia()))
            .razon(factura.getRazonReferencia())
            .build());
    }

    private List<MedioPagoReimpresionDto> construirMediosPago(List<FacturaMedioPago> mediosPago) {
        if (mediosPago == null || mediosPago.isEmpty()) {
            return Collections.emptyList();
        }

        return mediosPago.stream()
            .map(mp -> MedioPagoReimpresionDto.builder()
                .codigo(mp.getMedioPago().getCodigo())
                .nombre(getNombreMedioPago(mp.getMedioPago()))
                .monto(mp.getMonto())
                .referencia(mp.getReferencia())
                .banco(mp.getBanco())
                .build())
            .collect(Collectors.toList());
    }

    // ========== HELPERS DE NOMBRES ==========

    private String getNombreTipoDocumento(TipoDocumento tipo) {
        if (tipo == null) return "Documento";
        return switch (tipo) {
            case FACTURA_ELECTRONICA -> "Factura Electrónica";
            case NOTA_DEBITO -> "Nota de Débito";
            case NOTA_CREDITO -> "Nota de Crédito";
            case TIQUETE_ELECTRONICO -> "Tiquete Electrónico";
            case FACTURA_COMPRA -> "Factura de Compra";
            case FACTURA_EXPORTACION -> "Factura de Exportación";
            default -> tipo.name();
        };
    }

    private String getNombreCodigoReferencia(String codigo) {
        if (codigo == null) return "";
        return switch (codigo) {
            case "01" -> "Anula Documento";
            case "02" -> "Corrige Monto";
            case "03" -> "Corrige Texto";
            case "04" -> "Referencia Otro Doc";
            case "05" -> "Sustituye Contingencia";
            case "06" -> "Devolución Mercancía";
            case "07" -> "Sustituye Comprobante";
            case "99" -> "Otros";
            default -> codigo;
        };
    }

    private String getNombreMedioPago(MedioPago medio) {
        if (medio == null) return "Otro";
        return switch (medio) {
            case EFECTIVO -> "Efectivo";
            case TARJETA -> "Tarjeta";
            case CHEQUE -> "Cheque";
            case TRANSFERENCIA -> "Transferencia";
            case RECAUDADO_TERCEROS -> "Recaudado Terceros";
            case OTROS -> "Otros";
            default -> medio.name();
        };
    }

    private String construirDireccionEmpresa(Empresa empresa) {
        StringBuilder sb = new StringBuilder();
        
        if (empresa.getProvincia() != null) {
            sb.append(empresa.getProvincia().getProvincia());
        }
        if (empresa.getCanton() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(empresa.getCanton().getCanton());
        }
        if (empresa.getDistrito() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(empresa.getDistrito().getDistrito());
        }
        if (empresa.getOtrasSenas() != null && !empresa.getOtrasSenas().isBlank()) {
            if (!sb.isEmpty()) sb.append(". ");
            sb.append(empresa.getOtrasSenas());
        }
        
        return sb.toString();
    }

    private String construirDireccionCliente(Cliente cliente) {
        StringBuilder sb = new StringBuilder();
        
        if (cliente.getUbicacion().getProvincia() != null) {
            sb.append(cliente.getUbicacion().getProvincia().getProvincia());
        }
        if (cliente.getUbicacion().getCanton() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(cliente.getUbicacion().getCanton().getCanton());
        }
        if (cliente.getUbicacion().getDistrito() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(cliente.getUbicacion().getDistrito().getDistrito());
        }
        if (cliente.getUbicacion().getOtrasSenas() != null && !cliente.getUbicacion().getOtrasSenas().isBlank()) {
            if (!sb.isEmpty()) sb.append(". ");
            sb.append(cliente.getUbicacion().getOtrasSenas());
        }
        
        return sb.toString();
    }
}