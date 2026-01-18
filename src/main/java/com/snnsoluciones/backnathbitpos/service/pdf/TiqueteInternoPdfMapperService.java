package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.dto.facturainterna.DetalleFacturaInternaRequest;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import java.math.RoundingMode;
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
import java.util.LinkedHashMap;
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

    // Datos de la empresa y sucursal
    Empresa empresa = empresaService.buscarPorId(facturaInterna.getEmpresa().getId());
    Sucursal sucursal = sucursalService.finById(facturaInterna.getSucursal().getId()).orElse(null);

    // Nombre comercial: Sucursal primero, si no Empresa
    String nombreComercial = "";
    if (sucursal != null && sucursal.getNombre() != null && !sucursal.getNombre().trim().isEmpty()) {
      nombreComercial = sucursal.getNombre();
    } else if (empresa.getNombreComercial() != null && !empresa.getNombreComercial().trim().isEmpty()) {
      nombreComercial = empresa.getNombreComercial();
    }
    parametros.put("empresa_nombre", nombreComercial);

    // Razón Social: Solo de empresa
    String razonSocial = empresa.getNombreRazonSocial() != null && !empresa.getNombreRazonSocial().trim().isEmpty()
        ? empresa.getNombreRazonSocial()
        : "";
    parametros.put("empresa_razon_social", razonSocial);

    parametros.put("empresa_identificacion", formatearIdentificacion(empresa));

    // Email: Sucursal primero, si no Empresa
    String email = "";
    if (sucursal != null && sucursal.getEmail() != null && !sucursal.getEmail().trim().isEmpty()) {
      email = sucursal.getEmail();
    } else if (empresa.getEmail() != null && !empresa.getEmail().trim().isEmpty()) {
      email = empresa.getEmail();
    }
    parametros.put("email_empresa", email);

    // Teléfono: Sucursal primero, si no Empresa
    String telefono = "";
    if (sucursal != null && sucursal.getTelefono() != null && !sucursal.getTelefono().trim().isEmpty()) {
      telefono = sucursal.getTelefono();
    } else if (empresa.getTelefono() != null && !empresa.getTelefono().trim().isEmpty()) {
      telefono = empresa.getTelefono();
    }
    parametros.put("telefono_empresa", telefono);

    // Dirección completa: Sucursal primero, si no Empresa
    String direccion = construirDireccionCompleta(sucursal, empresa);
    parametros.put("empresa_direccion", direccion);

    cargarLogoEmpresa(parametros, empresa, sucursal);

    // Datos de la factura interna
    parametros.put("numero_interno", facturaInterna.getNumero());
    parametros.put("fecha_hora", facturaInterna.getFecha().format(FECHA_FORMATO));
    parametros.put("sucursal_nombre", facturaInterna.getSucursal().getNombre());
    parametros.put("terminal",
        "Terminal 01"); // FacturaInterna no tiene terminal, usar valor por defecto
    parametros.put("cajero_nombre", obtenerNombreCajero(facturaInterna));
    parametros.put("numero_viper", facturaInterna.getNumeroViper());

    // Cliente - Información completa
    String clienteNombre = "";
    String clienteCedula = "";
    String clienteEmail = "";

    if (facturaInterna.getCliente() != null) {
      Cliente cliente = facturaInterna.getCliente();

      // Nombre
      clienteNombre = cliente.getRazonSocial() != null ? cliente.getRazonSocial() : "";

      // Cédula
      clienteCedula = cliente.getNumeroIdentificacion() != null ? cliente.getNumeroIdentificacion() : "";

      // Email principal
      if (cliente.getClienteEmails() != null && !cliente.getClienteEmails().isEmpty()) {
        // Buscar email principal, si no existe tomar el primero
        clienteEmail = cliente.getClienteEmails().stream()
            .filter(ce -> ce.getEsPrincipal() != null && ce.getEsPrincipal())
            .findFirst()
            .map(ClienteEmail::getEmail)
            .orElse(cliente.getClienteEmails().stream()
                .findFirst()
                .map(ClienteEmail::getEmail)
                .orElse(""));
      }
    } else if (facturaInterna.getNombreCliente() != null) {
      clienteNombre = facturaInterna.getNombreCliente();
    } else {
      clienteNombre = "Cliente General";
    }

    parametros.put("cliente_nombre", clienteNombre);
    parametros.put("cliente_cedula", clienteCedula);
    parametros.put("cliente_email", clienteEmail);

    // Totales (formateados como string para el reporte)
    parametros.put("subtotal", DECIMAL_FORMAT.format(facturaInterna.getSubtotal()));
    parametros.put("descuentos", DECIMAL_FORMAT.format(
        facturaInterna.getDescuento() != null ? facturaInterna.getDescuento() : BigDecimal.ZERO));
    parametros.put("descuento_porcentaje",
        facturaInterna.getDescuentoPorcentaje() != null
            ? facturaInterna.getDescuentoPorcentaje().toString()
            : "0");
    BigDecimal impuestoServicio = calcularImpuestoServicio(facturaInterna);
    parametros.put("impuesto_servicio", DECIMAL_FORMAT.format(impuestoServicio));

    parametros.put("total", DECIMAL_FORMAT.format(facturaInterna.getTotal()));

    // Detalles para el subreporte (agrupados si son iguales)
    List<DetalleDTO> detalles = mapearYAgruparDetalles(facturaInterna.getDetalles());
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

    return parametros;
  }

  /**
   * Construye la dirección completa concatenando provincia, canton, distrito, barrio y otras señas
   * Prioridad: Sucursal primero, si no hay datos usa Empresa
   */
  private String construirDireccionCompleta(Sucursal sucursal, Empresa empresa) {
    List<String> partes = new ArrayList<>();

    // Variables para almacenar los datos
    String provincia = "";
    String canton = "";
    String distrito = "";
    String barrio = "";
    String otrasSenas = "";

    // Intentar primero con datos de sucursal
    if (sucursal != null) {
      if (sucursal.getProvincia() != null && sucursal.getProvincia().getProvincia() != null) {
        provincia = sucursal.getProvincia().getProvincia();
      }
      if (sucursal.getCanton() != null && sucursal.getCanton().getCanton() != null) {
        canton = sucursal.getCanton().getCanton();
      }
      if (sucursal.getDistrito() != null && sucursal.getDistrito().getDistrito() != null) {
        distrito = sucursal.getDistrito().getDistrito();
      }
      if (sucursal.getBarrio() != null && sucursal.getBarrio().getBarrio() != null) {
        barrio = sucursal.getBarrio().getBarrio();
      }
      if (sucursal.getOtrasSenas() != null && !sucursal.getOtrasSenas().trim().isEmpty()) {
        otrasSenas = sucursal.getOtrasSenas();
      }
    }

    // Si no hay datos de sucursal, usar datos de empresa
    if (provincia.isEmpty() && empresa.getProvincia() != null && empresa.getProvincia().getProvincia() != null) {
      provincia = empresa.getProvincia().getProvincia();
    }
    if (canton.isEmpty() && empresa.getCanton() != null && empresa.getCanton().getCanton() != null) {
      canton = empresa.getCanton().getCanton();
    }
    if (distrito.isEmpty() && empresa.getDistrito() != null && empresa.getDistrito().getDistrito() != null) {
      distrito = empresa.getDistrito().getDistrito();
    }
    if (barrio.isEmpty() && empresa.getBarrio() != null && empresa.getBarrio().getBarrio() != null) {
      barrio = empresa.getBarrio().getBarrio();
    }
    if (otrasSenas.isEmpty() && empresa.getOtrasSenas() != null && !empresa.getOtrasSenas().trim().isEmpty()) {
      otrasSenas = empresa.getOtrasSenas();
    }

    // Construir la dirección concatenando las partes que existan
    if (!provincia.isEmpty()) {
      partes.add(provincia);
    }
    if (!canton.isEmpty()) {
      partes.add(canton);
    }
    if (!distrito.isEmpty()) {
      partes.add(distrito);
    }
    if (!barrio.isEmpty()) {
      partes.add(barrio);
    }
    if (!otrasSenas.isEmpty()) {
      partes.add(otrasSenas);
    }

    return String.join(", ", partes);
  }

  private String obtenerNombreCajero(FacturaInterna facturaInterna) {
    if (facturaInterna.getSesionCaja() != null
        && facturaInterna.getSesionCaja().getUsuario() != null) {
      return facturaInterna.getSesionCaja().getUsuario().getNombre();
    }
    return "Sistema";
  }

  private BigDecimal calcularImpuestoServicio(FacturaInterna facturaInterna) {
    // Si la factura interna tiene impuesto servicio, retornarlo
    if (facturaInterna.getImpuestoServicio() != null) {
      return facturaInterna.getImpuestoServicio();
    }

    // Si no, calcularlo como 10% del subtotal
    BigDecimal subtotal = facturaInterna.getSubtotal();
    BigDecimal descuento = facturaInterna.getDescuento() != null ?
        facturaInterna.getDescuento() : BigDecimal.ZERO;

    BigDecimal base = subtotal.subtract(descuento);
    return base.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Mapea y agrupa detalles que sean idénticos (mismo producto, notas y precio)
   * Suma las cantidades de items iguales
   */
  private List<DetalleDTO> mapearYAgruparDetalles(List<FacturaInternaDetalle> detalles) {
    // Mapa para agrupar: key = nombreProducto|notas|precioUnitario, value = DetalleDTO acumulado
    Map<String, DetalleDTO> detallesAgrupados = new LinkedHashMap<>();

    for (FacturaInternaDetalle detalle : detalles) {
      // Crear la clave única para agrupar
      String nombreProducto = detalle.getNombreProducto() != null ? detalle.getNombreProducto() : "";
      String notas = detalle.getNotas() != null ? detalle.getNotas() : "";
      BigDecimal precioUnitario = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : BigDecimal.ZERO;

      // Clave única: nombreProducto|notas|precio
      String clave = nombreProducto + "|" + notas + "|" + precioUnitario.toPlainString();

      if (detallesAgrupados.containsKey(clave)) {
        // Ya existe, sumar cantidad y recalcular subtotal
        DetalleDTO existente = detallesAgrupados.get(clave);
        BigDecimal nuevaCantidad = existente.getCantidad().add(detalle.getCantidad());
        BigDecimal nuevoSubtotal = nuevaCantidad.multiply(precioUnitario);

        existente.setCantidad(nuevaCantidad);
        existente.setSubtotal(nuevoSubtotal);
      } else {
        // Nuevo item, agregarlo
        DetalleDTO dto = new DetalleDTO();
        dto.setCantidad(detalle.getCantidad());

        // Construir descripción completa (nombreProducto + notas si existen)
        String descripcionCompleta = nombreProducto;
        if (notas != null && !notas.trim().isEmpty()) {
          descripcionCompleta = nombreProducto + " - " + notas;
        }
        dto.setDescripcion(descripcionCompleta);

        dto.setPrecioUnitario(precioUnitario);
        dto.setSubtotal(detalle.getSubtotal());

        detallesAgrupados.put(clave, dto);
      }
    }

    return new ArrayList<>(detallesAgrupados.values());
  }

  private List<DetalleDTO> mapearDetalles(List<FacturaInternaDetalle> detalles) {
    List<DetalleDTO> resultado = new ArrayList<>();

    for (FacturaInternaDetalle detalle : detalles) {
      DetalleDTO dto = new DetalleDTO();
      dto.setCantidad(detalle.getCantidad());
      dto.setDescripcion(detalle.getNombreProducto());
      dto.setPrecioUnitario(detalle.getPrecioUnitario());
      dto.setSubtotal(detalle.getSubtotal());
      resultado.add(dto);
    }

    return resultado;
  }

  private String formatearMediosPago(List<FacturaInternaMedioPago> mediosPago) {
    if (mediosPago == null || mediosPago.isEmpty()) {
      return "Efectivo";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < mediosPago.size(); i++) {
      FacturaInternaMedioPago medio = mediosPago.get(i);
      sb.append(traducirMedioPago(MedioPago.valueOf(medio.getTipo())));
      sb.append(": ");
      sb.append(DECIMAL_FORMAT.format(medio.getMonto()));

      if (i < mediosPago.size() - 1) {
        sb.append(" | ");
      }
    }
    return sb.toString();
  }

  private String traducirMedioPago(MedioPago medioPago) {
    switch (medioPago) {
      case EFECTIVO:
        return "Efectivo";
      case TARJETA:
        return "Tarjeta";
      case TRANSFERENCIA:
        return "Transferencia";
      case CHEQUE:
        return "Cheque";
      case SINPE_MOVIL:
        return "SINPE Móvil";
      default:
        return medioPago.name();
    }
  }

  private String formatearIdentificacion(Empresa empresa) {
    if (empresa.getIdentificacion() == null) {
      return "";
    }

    String cedula = empresa.getIdentificacion();

    // Si es cédula jurídica (10 dígitos)
    if (cedula.length() == 10) {
      return cedula.substring(0, 1) + "-" +
          cedula.substring(1, 4) + "-" +
          cedula.substring(4);
    }

    // Si es cédula física (9 dígitos)
    if (cedula.length() == 9) {
      return cedula.substring(0, 1) + "-" +
          cedula.substring(1, 5) + "-" +
          cedula.substring(5);
    }

    // Si no coincide con ningún formato, retornar tal cual
    return cedula;
  }

  private String obtenerDireccionEmpresa(Empresa empresa) {
    StringBuilder sb = new StringBuilder();

    if (empresa.getProvincia() != null) {
      sb.append(empresa.getProvincia().getProvincia());
    }

    if (empresa.getCanton() != null) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(empresa.getCanton().getCanton());
    }

    if (empresa.getDistrito() != null) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(empresa.getDistrito().getDistrito());
    }

    if (empresa.getOtrasSenas() != null && !empresa.getOtrasSenas().isEmpty()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(empresa.getOtrasSenas());
    }

    return sb.toString();
  }

  /**
   * Carga el logo de la empresa/sucursal con manejo robusto de errores.
   * Prioridad: Logo de sucursal → Logo de empresa → Sin logo
   *
   * @param parametros Mapa de parámetros del reporte
   * @param empresa Empresa
   * @param sucursal Sucursal (puede ser null)
   */
  private void cargarLogoEmpresa(Map<String, Object> parametros, Empresa empresa, Sucursal sucursal) {
    try {
      // Intentar logo de sucursal primero
      if (sucursal != null &&
          sucursal.getLogoSucursalPath() != null &&
          !sucursal.getLogoSucursalPath().trim().isEmpty()) {

        String logoKey = sucursal.getLogoSucursalPath();
        log.debug("Intentando cargar logo de sucursal: {}", logoKey);

        byte[] logoBytes = storageService.downloadFileAsBytes(logoKey);
        parametros.put("logo_empresa", new ByteArrayInputStream(logoBytes));
        parametros.put("tiene_logo", true);

        log.debug("Logo de sucursal cargado exitosamente, tamaño: {} bytes", logoBytes.length);
        return; // ✅ Logo encontrado, salir
      }

      // Si no hay logo de sucursal, intentar logo de empresa
      if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().trim().isEmpty()) {
        String logoKey = empresa.getLogoUrl();

        // Limpiar URL si viene completa (extraer solo la key)
        if (logoKey.startsWith("http")) {
          int startIndex = logoKey.indexOf("NathBit-POS/");
          if (startIndex != -1) {
            logoKey = logoKey.substring(startIndex);
          } else {
            String bucketPattern = "/snn-soluciones/";
            startIndex = logoKey.indexOf(bucketPattern);
            if (startIndex != -1) {
              logoKey = logoKey.substring(startIndex + bucketPattern.length());
            }
          }
        }

        log.debug("Intentando cargar logo de empresa: {}", logoKey);

        byte[] logoBytes = storageService.downloadFileAsBytes(logoKey);
        parametros.put("logo_empresa", new ByteArrayInputStream(logoBytes));
        parametros.put("tiene_logo", true);

        log.debug("Logo de empresa cargado exitosamente, tamaño: {} bytes", logoBytes.length);
        return; // ✅ Logo encontrado, salir
      }

      // No hay logo configurado
      log.debug("No hay logo configurado para empresa {} / sucursal {}",
          empresa.getId(),
          sucursal != null ? sucursal.getId() : "N/A");
      parametros.put("tiene_logo", false);

    } catch (Exception e) {
      // ⚠️ Si falla la descarga, continuar sin logo (no romper el PDF)
      log.error("Error cargando logo para tiquete interno (empresa: {}, sucursal: {}): {}",
          empresa.getId(),
          sucursal != null ? sucursal.getId() : "N/A",
          e.getMessage());
      parametros.put("tiene_logo", false);
    }
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