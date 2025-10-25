package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.DetalleFacturaInternaRequest;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para mapear datos de tiquetes internos a parámetros del reporte
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TiqueteInternoPdfMapperService {

  private final FacturaInternaRepository facturaInternaRepository;
  private final PdfGeneratorService pdfGeneratorService;
  private final StorageService storageService;
  private final EmpresaService empresaService;
  private final SucursalService sucursalService;

  private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy HH:mm:ss");
  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

  /**
   * Mapea una factura interna a parámetros para el reporte de tiquete
   *
   * @param numeroInterno Número interno de la factura (ej: INT-2024-00001)
   * @return Mapa con todos los parámetros necesarios para el reporte
   */
  @Transactional(readOnly = true)
  public Map<String, Object> mapearTiqueteInternoAParametros(String numeroInterno) {
    log.info("Mapeando tiquete interno: {}", numeroInterno);

    // Buscar la factura interna por número
    FacturaInterna facturaInterna = facturaInternaRepository.findByNumero(numeroInterno)
        .orElseThrow(() -> new RuntimeException("Tiquete interno no encontrado: " + numeroInterno));

    Map<String, Object> parametros = new HashMap<>();

    // Datos de la empresa
    Empresa empresa = empresaService.buscarPorId(facturaInterna.getEmpresa().getId());
    Sucursal sucursal = sucursalService.finById(facturaInterna.getSucursal().getId()).orElse(null);
    parametros.put("empresa_nombre", empresa.getNombreRazonSocial() != null ?
        empresa.getNombreRazonSocial() : empresa.getNombreComercial());
    parametros.put("empresa_identificacion", formatearIdentificacion(empresa));
    parametros.put("email_empresa", sucursal.getEmail() != null ? sucursal.getEmail()
        : empresa.getEmail() != null ? empresa.getEmail() : "");
    parametros.put("telefono_empresa", sucursal.getTelefono() != null ? sucursal.getTelefono()
        : empresa.getTelefono() != null ? empresa.getTelefono() : "");
    parametros.put("empresa_direccion", obtenerDireccionEmpresa(empresa));

    if (sucursal != null && !sucursal.getLogoSucursalPath().isEmpty()) {
      parametros.put("logo_empresa", new ByteArrayInputStream(
          storageService.downloadFileAsBytes(sucursal.getLogoSucursalPath())));
      parametros.put("tiene_logo", true);
    } else {
      if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().isEmpty()) {
        parametros.put("logo_empresa", new ByteArrayInputStream(
            storageService.downloadFileAsBytes(empresa.getLogoUrl())));
        parametros.put("tiene_logo", true);
      } else {
        parametros.put("tiene_logo", false);
      }
    }

    // Datos de la factura interna
    parametros.put("numero_interno", facturaInterna.getNumero());
    parametros.put("fecha_hora", facturaInterna.getFecha().format(FECHA_FORMATO));
    parametros.put("sucursal_nombre", facturaInterna.getSucursal().getNombre());
    parametros.put("terminal",
        "Terminal 01"); // FacturaInterna no tiene terminal, usar valor por defecto
    parametros.put("cajero_nombre", obtenerNombreCajero(facturaInterna));
    parametros.put("numero_viper", facturaInterna.getNumeroViper());

    // Cliente
    if (facturaInterna.getCliente() != null) {
      parametros.put("cliente_referencia", facturaInterna.getCliente().getRazonSocial());
    } else if (facturaInterna.getNombreCliente() != null) {
      parametros.put("cliente_referencia", facturaInterna.getNombreCliente());
    } else {
      parametros.put("cliente_referencia", "Cliente General");
    }

    // Totales (formateados como string para el reporte)
    parametros.put("subtotal", DECIMAL_FORMAT.format(facturaInterna.getSubtotal()));
    parametros.put("descuentos", DECIMAL_FORMAT.format(
        facturaInterna.getDescuento() != null ? facturaInterna.getDescuento() : BigDecimal.ZERO));
    parametros.put("descuento_porcentaje",
        facturaInterna.getDescuentoPorcentaje() != null
            ? facturaInterna.getDescuentoPorcentaje().toString()
            : "0");
    parametros.put("iva", "0.00"); // FacturaInterna no maneja IVA separado
    parametros.put("total", DECIMAL_FORMAT.format(facturaInterna.getTotal()));

    // Detalles para el subreporte
    List<DetalleDTO> detalles = mapearDetalles(facturaInterna.getDetalles());
    parametros.put("detalles", detalles); // Para referencia

    // IMPORTANTE: El datasource debe ir como parámetro del reporte principal
    parametros.put("datasource_detalles", new JRBeanCollectionDataSource(detalles));

    // Subreporte compilado debe ir como parámetro también
    JasperReport subreport = pdfGeneratorService.getCompiledReport("detalle_tiquete_interno_80mm");
    if (subreport != null) {
      parametros.put("subreport_detalles", subreport);
    } else {
      log.error("No se pudo obtener el subreporte compilado detalle_tiquete_interno_80mm");
      throw new RuntimeException("Subreporte no encontrado");
    }

    // Medios de pago
    String mediosPago = formatearMediosPago(facturaInterna.getMediosPago());
    parametros.put("medios_pago", mediosPago);

    // Mensaje de cortesía
    parametros.put("mensaje_cortesia", "¡Gracias por su preferencia!");

    // Vuelto (si aplica)
    if (facturaInterna.getVuelto() != null
        && facturaInterna.getVuelto().compareTo(BigDecimal.ZERO) > 0) {
      parametros.put("vuelto", DECIMAL_FORMAT.format(facturaInterna.getVuelto()));
    }

    return parametros;
  }

  /**
   * Mapea los detalles de la factura interna a DTOs para el reporte
   */
  private List<DetalleDTO> mapearDetalles(List<FacturaInternaDetalle> detalles) {
    List<DetalleDTO> dtos = new ArrayList<>();

    if (detalles != null) {
      for (FacturaInternaDetalle detalle : detalles) {
        DetalleDTO dto = new DetalleDTO();
        dto.setCantidad(detalle.getCantidad());
        dto.setDescripcion(!detalle.getNotas().isEmpty() ?
            detalle.getNombreProducto().concat("\n").concat(detalle.getNotas()) :
            detalle.getNombreProducto());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());
        dtos.add(dto);
      }
    }

    return dtos;
  }

  /**
   * Formatea la identificación de la empresa
   */
  private String formatearIdentificacion(Empresa empresa) {
    if (empresa.getTipoIdentificacion() != null && empresa.getIdentificacion() != null) {
      return empresa.getTipoIdentificacion() + ": " + empresa.getIdentificacion();
    }
    return "";
  }

  /**
   * Obtiene la dirección formateada de la empresa
   */
  private String obtenerDireccionEmpresa(Empresa empresa) {
    StringBuilder direccion = new StringBuilder();

    if (empresa.getOtrasSenas() != null) {
      direccion.append(empresa.getOtrasSenas());
    }

    // Si la empresa tiene distrito configurado
    if (empresa.getDistrito() != null) {
      if (!direccion.isEmpty()) {
        direccion.append(", ");
      }
      direccion.append(empresa.getDistrito().getDistrito());
      direccion.append(", ").append(empresa.getCanton().getCanton());
      direccion.append(", ").append(empresa.getProvincia().getProvincia());
    }

    return direccion.toString();
  }

  /**
   * Obtiene el nombre del cajero
   */
  private String obtenerNombreCajero(FacturaInterna facturaInterna) {
    if (facturaInterna.getCajero() != null) {
      Usuario usuario = facturaInterna.getCajero();
      return usuario.getNombre() + " " + usuario.getApellidos();
    }
    return "Sistema";
  }

  /**
   * Formatea los medios de pago para mostrar en el tiquete
   */
  private String formatearMediosPago(List<FacturaInternaMedioPago> mediosPago) {
    if (mediosPago == null || mediosPago.isEmpty()) {
      return "Efectivo";
    }

    StringBuilder sb = new StringBuilder();
    for (FacturaInternaMedioPago mp : mediosPago) {
      if (!sb.isEmpty()) {
        sb.append(", ");
      }
      sb.append(mp.getTipo()); // EFECTIVO, TARJETA, etc.
    }
    return sb.toString();
  }

  /**
   * DTO simple para los detalles del reporte
   */
  public static class DetalleDTO {

    private BigDecimal cantidad;
    private String descripcion;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    // Getters y setters
    public BigDecimal getCantidad() {
      return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
      this.cantidad = cantidad;
    }

    public String getDescripcion() {
      return descripcion;
    }

    public void setDescripcion(String descripcion) {
      this.descripcion = descripcion;
    }

    public BigDecimal getPrecioUnitario() {
      return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
      this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
      return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
      this.subtotal = subtotal;
    }
  }
}