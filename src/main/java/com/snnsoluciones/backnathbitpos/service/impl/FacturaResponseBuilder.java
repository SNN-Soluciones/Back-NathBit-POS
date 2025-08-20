package com.snnsoluciones.backnathbitpos.service.impl;

import static com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.CHEQUE;
import static com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.EFECTIVO;
import static com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.OTROS;
import static com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.RECAUDADO_TERCEROS;
import static com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.TARJETA;
import static com.snnsoluciones.backnathbitpos.enums.mh.MedioPago.TRANSFERENCIA;

import com.snnsoluciones.backnathbitpos.dto.factura.FacturaListaResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder para construir responses de factura
 */
@Component
public class FacturaResponseBuilder {

  /**
   * Construir response completo de factura
   */
  public FacturaResponse construirResponse(Factura factura) {
    return FacturaResponse.builder()
        .id(factura.getId())
        .consecutivo(factura.getConsecutivo())
        .clave(factura.getClave())
        .tipoDocumento(factura.getTipoDocumento())
        .estado(factura.getEstado())

        // Cliente
        .clienteId(factura.getCliente() != null ? factura.getCliente().getId() : null)
        .clienteNombre(obtenerNombreCliente(factura))
        .clienteIdentificacion(factura.getCliente() != null
            ? factura.getCliente().getNumeroIdentificacion() : null)

        // Moneda
        .moneda(factura.getMoneda())
        .tipoCambio(factura.getTipoCambio())

        // Totales
        .subtotal(factura.getTotalVentaNeta())
        .totalDescuentosLineas(calcularTotalDescuentosLineas(factura))
        .montoDescuentoGlobal(factura.getMontoDescuentoGlobal())
        .totalDescuentos(factura.getTotalDescuentos())
        .totalOtrosCargos(factura.getTotalOtrosCargos())
        .totalImpuestos(factura.getTotalImpuesto())
        .total(factura.getTotalComprobante())

        // Detalles
        .detalles(construirDetalles(factura.getDetalles()))

        // Otros cargos
        .otrosCargos(construirOtrosCargos(factura.getOtrosCargos()))

        // Medios de pago
        .mediosPago(construirMediosPago(factura.getMediosPago()))

        // Metadata

        .fechaEmision(factura.getFechaEmision())
        .sucursalNombre(factura.getSucursal().getNombre())
        .terminalNombre(factura.getTerminal().getNombre())
        .cajeroNombre(obtenerNombreCajero(factura))
        .situacionComprobante(factura.getSituacion().name())

        // Hacienda
//            .mensajeHacienda(factura.getMensajeRespuesta())
//            .xmlFirmado(factura.getXmlFirmado())

        .build();
  }

  /**
   * Construir response para lista
   */
  public FacturaListaResponse construirListaResponse(Factura factura) {
    return FacturaListaResponse.builder()
        .id(factura.getId())
        .consecutivo(factura.getConsecutivo())
        .fechaEmision(factura.getFechaEmision())
        .tipoDocumento(factura.getTipoDocumento().name())
        .clienteNombre(obtenerNombreCliente(factura))
        .estado(factura.getEstado().name())
        .moneda(factura.getMoneda())
        .total(factura.getTotalComprobante())
        .build();
  }

  /**
   * Construir detalles de factura
   */
  private List<DetalleFacturaResponse> construirDetalles(List<FacturaDetalle> detalles) {
    return detalles.stream()
        .sorted(Comparator.comparing(FacturaDetalle::getNumeroLinea))
        .map(this::construirDetalle)
        .collect(Collectors.toList());
  }

  /**
   * Construir un detalle
   */
  private DetalleFacturaResponse construirDetalle(FacturaDetalle detalle) {
    return DetalleFacturaResponse.builder()
        .numeroLinea(detalle.getNumeroLinea())
        .productoId(detalle.getProducto().getId())
        .productoNombre(detalle.getProducto().getNombre())
        .codigoCabys(detalle.getCodigoCabys())
        .cantidad(detalle.getCantidad())
        .unidadMedida(detalle.getUnidadMedida())
        .precioUnitario(detalle.getPrecioUnitario())
        .subtotal(detalle.getSubtotal())
        .montoImpuesto(detalle.getMontoImpuesto())
        .total(detalle.getFactura().getTotalComprobante())
        .descuentos(construirDescuentos(detalle.getDescuentos()))
        .build();
  }

  /**
   * Construir descuentos de una línea
   */
  private List<DescuentoResponse> construirDescuentos(List<FacturaDescuento> descuentos) {
    return descuentos.stream()
        .sorted(Comparator.comparing(FacturaDescuento::getOrden))
        .map(desc -> DescuentoResponse.builder()
            .codigoDescuento(desc.getCodigoDescuento())
            .descripcion(obtenerDescripcionDescuento(desc))
            .porcentaje(desc.getPorcentaje())
            .montoDescuento(desc.getMontoDescuento())
            .orden(desc.getOrden())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Construir otros cargos
   */
  private List<OtroCargoResponse> construirOtrosCargos(List<OtroCargo> otrosCargos) {
    return otrosCargos.stream()
        .sorted(Comparator.comparing(OtroCargo::getNumeroLinea))
        .map(oc -> OtroCargoResponse.builder()
            .tipoDocumentoOC(oc.getTipoDocumentoOC())
            .nombreCargo(oc.getNombreCargo())
            .porcentaje(oc.getPorcentaje())
            .montoCargo(oc.getMontoCargo())
            .numeroLinea(oc.getNumeroLinea())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Construir medios de pago
   */
  private List<MedioPagoResponse> construirMediosPago(List<FacturaMedioPago> mediosPago) {
    return mediosPago.stream()
        .map(mp -> MedioPagoResponse.builder()
            .medioPago(mp.getMedioPago().name())
            .descripcion(obtenerDescripcionMedioPago(mp.getMedioPago()))
            .monto(mp.getMonto())
            .referencia(mp.getReferencia())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * Helpers
   */
  private String obtenerNombreCliente(Factura factura) {
    if (factura.getCliente() == null) {
      return null;
    }
    return factura.getCliente().getRazonSocial();
  }

  private String obtenerNombreCajero(Factura factura) {
    return "CAJERO";
  }

  private BigDecimal calcularTotalDescuentosLineas(Factura factura) {
    return factura.getDetalles().stream()
        .map(FacturaDetalle::getMontoDescuento)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal obtenerTasaImpuesto(String codigoTarifaIVA) {
    return switch (codigoTarifaIVA) {
      case "01", "05", "10", "11" -> BigDecimal.ZERO;
      case "09" -> new BigDecimal("0.5");
      case "02" -> BigDecimal.ONE;
      case "03" -> new BigDecimal("2");
      case "04", "06" -> new BigDecimal("4");
      case "07" -> new BigDecimal("8");
      case "08" -> new BigDecimal("13");
      default -> new BigDecimal("13");
    };
  }

  private String obtenerDescripcionDescuento(FacturaDescuento descuento) {
    if ("99".equals(descuento.getCodigoDescuento())) {
      return descuento.getNaturalezaDescuento();
    }

    return switch (descuento.getCodigoDescuento()) {
      case "01" -> "Descuento por Regalía";
      case "02" -> "Descuento por Regalía o Bonificaciones IVA";
      case "03" -> "Descuento por Bonificación";
      case "04" -> "Descuento por volumen";
      case "05" -> "Descuento por Temporada";
      case "06" -> "Descuento promocional";
      case "07" -> "Descuento Comercial";
      case "08" -> "Descuento por frecuencia";
      case "09" -> "Descuento sostenido";
      default -> "Descuento";
    };
  }

  private String obtenerDescripcionMedioPago(MedioPago medioPago) {
    return switch (medioPago) {
      case EFECTIVO -> "Efectivo";
      case TARJETA -> "Tarjeta";
      case CHEQUE -> "Cheque";
      case TRANSFERENCIA -> "Transferencia/Depósito";
      case RECAUDADO_TERCEROS -> "Recaudado por Terceros";
      case SINPE_MOVIL -> "Sinpe Movil";
      case PLATAFORMA_DIGITAL -> "Plataforma Digital";
      case OTROS -> "Otros";
    };
  }
}