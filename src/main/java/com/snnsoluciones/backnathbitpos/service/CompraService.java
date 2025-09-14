package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.compra.*;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.Moneda;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoCompra;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.util.XmlProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompraService {

  private final CompraRepository compraRepository;
  private final ProveedorRepository proveedorRepository;
  private final ProductoCrudService productoService;
  private final ProductoCodigoProveedorRepository productoCodigoProveedorRepository;
  private final EmpresaRepository empresaRepository;
  private final SucursalRepository sucursalRepository;
  private final UsuarioRepository usuarioRepository;
  private final XmlProcessorService xmlProcessorService;
  private final ProductoInventarioService inventarioService;

  /**
   * Analiza un XML de factura sin procesarlo
   */
  @Transactional(readOnly = true)
  public AnalisisXmlResponse analizarXml(String xmlContent, Long empresaId, Long sucursalId) {
    AnalisisXmlResponse analisis = new AnalisisXmlResponse();
    analisis.setErroresValidacion(new ArrayList<>());
    analisis.setProductosNoEncontrados(new ArrayList<>());

    try {
      // Procesar XML
      FacturaXmlDto facturaXml = xmlProcessorService.procesarFacturaXml(xmlContent);

      analisis.setEsValido(true);
      analisis.setClave(facturaXml.getClave());
      analisis.setNumeroDocumento(facturaXml.getNumeroConsecutivo());
      analisis.setFechaEmision(facturaXml.getFechaEmision());
      analisis.setTotalComprobante(facturaXml.getTotalComprobante());
      analisis.setMoneda(facturaXml.getCodigoMoneda());
      analisis.setCantidadLineas(facturaXml.getDetalles().size());

      // Verificar si ya existe la factura
      if (compraRepository.existsByClaveHacienda(facturaXml.getClave())) {
        analisis.setEsValido(false);
        analisis.getErroresValidacion().add("Esta factura ya fue procesada anteriormente");
      }

      // Analizar emisor
      if (facturaXml.getEmisor() != null) {
        AnalisisXmlResponse.EmisorInfo emisorInfo = new AnalisisXmlResponse.EmisorInfo();
        emisorInfo.setIdentificacion(facturaXml.getEmisor().getNumeroIdentificacion());
        emisorInfo.setNombre(facturaXml.getEmisor().getNombre());

        // Buscar proveedor por empresa
        Proveedor proveedor = proveedorRepository.findByNumeroIdentificacionAndEmpresaId(
            facturaXml.getEmisor().getNumeroIdentificacion(),
            empresaId
        ).orElse(null);

        if (proveedor != null) {
          emisorInfo.setExisteEnSistema(true);
          emisorInfo.setProveedorId(proveedor.getId());

          // Analizar productos usando código del proveedor
          for (FacturaXmlDto.DetalleDto detalle : facturaXml.getDetalles()) {
            boolean productoEncontrado = false;

            // Buscar por código del proveedor PRIMERO
            ProductoCodigoProveedor pcp = productoCodigoProveedorRepository
                .findByProveedorAndCodigo(proveedor.getId(), detalle.getCodigo())
                .orElse(null);

            if (pcp != null && pcp.getProducto().getEmpresa().getId().equals(empresaId)) {
              productoEncontrado = true;
            }

            if (!productoEncontrado) {
              AnalisisXmlResponse.ProductoNoEncontrado productoNoEncontrado =
                  new AnalisisXmlResponse.ProductoNoEncontrado();
              productoNoEncontrado.setCodigo(detalle.getCodigo());
              productoNoEncontrado.setCodigoCabys(detalle.getCodigoCabys());
              productoNoEncontrado.setDescripcion(detalle.getDetalle());
              analisis.getProductosNoEncontrados().add(productoNoEncontrado);
            }
          }
        } else {
          emisorInfo.setExisteEnSistema(false);
          analisis.getErroresValidacion()
              .add("El proveedor no existe en el sistema. Debe crearlo primero.");
        }

        analisis.setEmisor(emisorInfo);
      }

      // Determinar tipo de documento
      analisis.setTipoDocumento("FACTURA_ELECTRONICA");

    } catch (Exception e) {
      log.error("Error analizando XML: ", e);
      analisis.setEsValido(false);
      analisis.getErroresValidacion().add("Error procesando XML: " + e.getMessage());
    }

    return analisis;
  }

  /**
   * Crea una compra desde un XML de factura
   */
  @Transactional
  public CompraDto crearCompraDesdeXml(Long empresaId, Long sucursalId,
      CrearCompraDesdeXmlRequest request) {
    try {
      // Procesar XML
      FacturaXmlDto facturaXml = xmlProcessorService.procesarFacturaXml(request.getXmlContent());

      // Validar que no exista
      if (compraRepository.existsByClaveHacienda(facturaXml.getClave())) {
        throw new BadRequestException("Esta factura ya fue procesada anteriormente");
      }

      // Obtener entidades
      Empresa empresa = empresaRepository.findById(empresaId)
          .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

      Sucursal sucursal = sucursalRepository.findById(sucursalId)
          .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

      // Validar que la sucursal pertenezca a la empresa
      if (!sucursal.getEmpresa().getId().equals(empresaId)) {
        throw new BadRequestException("La sucursal no pertenece a la empresa especificada");
      }

      Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
          .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

      // Validar que el proveedor pertenezca a la empresa
      if (!proveedor.getEmpresa().getId().equals(empresaId)) {
        throw new BadRequestException("El proveedor no pertenece a la empresa especificada");
      }

      // Validar que el proveedor del XML coincida
      if (!proveedor.getNumeroIdentificacion().equals(
          facturaXml.getEmisor().getNumeroIdentificacion())) {
        throw new BadRequestException(
            "El proveedor seleccionado no coincide con el emisor del XML");
      }

      // Obtener usuario actual
      String username = SecurityContextHolder.getContext().getAuthentication().getName();
      Usuario usuario = usuarioRepository.findByEmail(username)
          .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

      // Obtener moneda
      Moneda moneda = Moneda.fromCodigo(facturaXml.getCodigoMoneda());

      // Crear compra
      Compra compra = new Compra();
      compra.setEmpresa(empresa);
      compra.setSucursal(sucursal);
      compra.setProveedor(proveedor);
      compra.setUsuario(usuario);
      compra.setTipoCompra(TipoCompra.FACTURA_PROVEEDOR_INSCRITO);
      compra.setTipoDocumentoHacienda(TipoDocumento.FACTURA_ELECTRONICA);
      compra.setNumeroDocumento(facturaXml.getNumeroConsecutivo());
      compra.setClaveHacienda(facturaXml.getClave());
      compra.setFechaEmision(facturaXml.getFechaEmision());
      compra.setFechaRecepcion(LocalDateTime.now());
      compra.setCondicionVenta(facturaXml.getCondicionVenta());
      compra.setPlazoCredito(facturaXml.getPlazoCredito());
      compra.setMedioPago(facturaXml.getMedioPago());
      compra.setMoneda(moneda);
      compra.setTipoCambio(facturaXml.getTipoCambio());

      // Asignar totales
      compra.setTotalServiciosGravados(facturaXml.getTotalServiciosGravados() != null ?
          facturaXml.getTotalServiciosGravados() : BigDecimal.ZERO);
      compra.setTotalServiciosExentos(facturaXml.getTotalServiciosExentos() != null ?
          facturaXml.getTotalServiciosExentos() : BigDecimal.ZERO);
      compra.setTotalMercanciasGravadas(facturaXml.getTotalMercanciasGravadas() != null ?
          facturaXml.getTotalMercanciasGravadas() : BigDecimal.ZERO);
      compra.setTotalMercanciasExentas(facturaXml.getTotalMercanciasExentas() != null ?
          facturaXml.getTotalMercanciasExentas() : BigDecimal.ZERO);

      compra.setTotalGravado(facturaXml.getTotalGravado());
      compra.setTotalExento(facturaXml.getTotalExento());
      compra.setTotalExonerado(facturaXml.getTotalExonerado());
      compra.setTotalVenta(facturaXml.getTotalVenta());
      compra.setTotalDescuentos(facturaXml.getTotalDescuentos());
      compra.setTotalVentaNeta(facturaXml.getTotalVentaNeta());
      compra.setTotalImpuesto(facturaXml.getTotalImpuesto());
      compra.setTotalOtrosCargos(facturaXml.getTotalOtrosCargos() != null ?
          facturaXml.getTotalOtrosCargos() : BigDecimal.ZERO);
      compra.setTotalComprobante(facturaXml.getTotalComprobante());

      compra.setEstado(EstadoCompra.ACEPTADA);
      compra.setXmlOriginal(request.getXmlContent());
      compra.setObservaciones(request.getObservaciones());

      // Procesar detalles
      for (FacturaXmlDto.DetalleDto detalleXml : facturaXml.getDetalles()) {
        CompraDetalle detalle = new CompraDetalle();
        detalle.setNumeroLinea(detalleXml.getNumeroLinea());
        detalle.setCodigo(detalleXml.getCodigo());
        detalle.setCodigoCabys(detalleXml.getCodigoCabys());
        detalle.setDescripcion(detalleXml.getDetalle());
        detalle.setCantidad(detalleXml.getCantidad());
        detalle.setUnidadMedida(detalleXml.getUnidadMedida());
        detalle.setPrecioUnitario(detalleXml.getPrecioUnitario());
        detalle.setMontoTotal(detalleXml.getMontoTotal());
        detalle.setMontoDescuento(detalleXml.getMontoDescuento() != null ?
            detalleXml.getMontoDescuento() : BigDecimal.ZERO);
        detalle.setNaturalezaDescuento(detalleXml.getNaturalezaDescuento());
        detalle.setSubTotal(detalleXml.getSubTotal());
        detalle.setCodigoTarifaIVA(detalleXml.getCodigoTarifa());
        detalle.setTarifaIVA(detalleXml.getTarifa());
        detalle.setMontoImpuesto(detalleXml.getMontoImpuesto() != null ?
            detalleXml.getMontoImpuesto() : BigDecimal.ZERO);
        detalle.setMontoTotalLinea(detalleXml.getMontoTotalLinea());

        // Buscar producto por código del proveedor
        ProductoCodigoProveedor pcp = productoCodigoProveedorRepository
            .findByProveedorAndCodigo(proveedor.getId(), detalleXml.getCodigo())
            .orElse(null);

        if (pcp != null && pcp.getProducto().getEmpresa().getId().equals(empresaId)) {
          Producto producto = pcp.getProducto();
          detalle.setProducto(producto);
          detalle.setEsServicio(producto.getEsServicio());

          // Usar el factor de conversión si existe
          if (pcp.getFactorConversion() != null) {
            detalle.setFactorConversion(new BigDecimal(pcp.getFactorConversion()));
            detalle.setUnidadMedidaComercial(pcp.getUnidadCompra());
          }

          // Actualizar precio de compra en el producto
          producto.setPrecioCompra(detalleXml.getPrecioUnitario());
          producto.setUltimoPrecioCompra(detalleXml.getPrecioUnitario());
          producto.setFechaUltimaCompra(LocalDateTime.now());
          productoService.save(producto);
        } else if (request.getCrearProductosSiNoExisten()) {
          // TODO: Implementar creación automática de productos
          log.warn("Producto no encontrado y creación automática no implementada: " +
              detalleXml.getDetalle());
        }

        compra.addDetalle(detalle);
      }

      // Guardar compra
      compra = compraRepository.save(compra);

      // Actualizar inventario si se solicitó
      if (request.getProcesarInventario() && !compra.getDetalles().isEmpty()) {
        inventarioService.procesarCompra(compra);
      }

      return convertirADto(compra);

    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error creando compra desde XML: ", e);
      throw new BadRequestException("Error procesando la factura: " + e.getMessage());
    }
  }

  /**
   * Crea una Factura Electrónica de Compra manual (para proveedores no inscritos)
   */
  @Transactional
  public CompraDto crearFacturaCompraManual(Long empresaId, Long sucursalId,
      CrearFacturaCompraRequest request) {
    // Validar entidades
    Empresa empresa = empresaRepository.findById(empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

    Sucursal sucursal = sucursalRepository.findById(sucursalId)
        .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

    // Validar que la sucursal pertenezca a la empresa
    if (!sucursal.getEmpresa().getId().equals(empresaId)) {
      throw new BadRequestException("La sucursal no pertenece a la empresa especificada");
    }

    Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
        .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

    // Validar que el proveedor pertenezca a la empresa
    if (!proveedor.getEmpresa().getId().equals(empresaId)) {
      throw new BadRequestException("El proveedor no pertenece a la empresa especificada");
    }

    Moneda moneda = Moneda.fromCodigo(request.getMoneda());

    // Obtener usuario actual
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    Usuario usuario = usuarioRepository.findByEmail(username)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    // Crear compra
    Compra compra = new Compra();
    compra.setEmpresa(empresa);
    compra.setSucursal(sucursal);
    compra.setProveedor(proveedor);
    compra.setUsuario(usuario);
    compra.setTipoCompra(TipoCompra.FACTURA_ELECTRONICA_COMPRA);
    compra.setTipoDocumentoHacienda(TipoDocumento.FACTURA_COMPRA); // Tipo 08
    compra.setNumeroDocumento(request.getNumeroFacturaProveedor());
    compra.setFechaEmision(request.getFechaEmision());
    compra.setFechaRecepcion(LocalDateTime.now());
    compra.setCondicionVenta(request.getCondicionVenta());
    compra.setPlazoCredito(request.getPlazoCredito());
    compra.setMedioPago(request.getMedioPago());
    compra.setMoneda(moneda);
    compra.setTipoCambio(request.getTipoCambio());
    compra.setObservaciones(request.getObservaciones());
    compra.setEstado(EstadoCompra.BORRADOR);

    // Inicializar totales
    BigDecimal totalServiciosGravados = BigDecimal.ZERO;
    BigDecimal totalServiciosExentos = BigDecimal.ZERO;
    BigDecimal totalMercanciasGravadas = BigDecimal.ZERO;
    BigDecimal totalMercanciasExentas = BigDecimal.ZERO;
    BigDecimal totalDescuentos = BigDecimal.ZERO;
    BigDecimal totalImpuesto = BigDecimal.ZERO;

    // Procesar detalles
    int numeroLinea = 1;
    for (CrearFacturaCompraRequest.DetalleCompraRequest detalleReq : request.getDetalles()) {
      CompraDetalle detalle = new CompraDetalle();
      detalle.setNumeroLinea(numeroLinea++);
      detalle.setCodigo(detalleReq.getCodigo());
      detalle.setCodigoCabys(detalleReq.getCodigoCabys());
      detalle.setDescripcion(detalleReq.getDescripcion());
      detalle.setEsServicio(detalleReq.getEsServicio());
      detalle.setCantidad(detalleReq.getCantidad());
      detalle.setUnidadMedida(detalleReq.getUnidadMedida());
      detalle.setPrecioUnitario(detalleReq.getPrecioUnitario());

      // Calcular montos
      detalle.setMontoTotal(detalleReq.getCantidad().multiply(detalleReq.getPrecioUnitario()));
      detalle.setMontoDescuento(detalleReq.getMontoDescuento() != null ?
          detalleReq.getMontoDescuento() : BigDecimal.ZERO);
      detalle.setNaturalezaDescuento(detalleReq.getNaturalezaDescuento());
      detalle.setSubTotal(detalle.getMontoTotal().subtract(detalle.getMontoDescuento()));

      // Impuestos
      detalle.setCodigoTarifaIVA(detalleReq.getCodigoTarifaIVA());
      BigDecimal tarifaIVA = obtenerTarifaIVA(detalleReq.getCodigoTarifaIVA());
      detalle.setTarifaIVA(tarifaIVA);

      if (tarifaIVA.compareTo(BigDecimal.ZERO) > 0) {
        detalle.setBaseImponible(detalle.getSubTotal());
        detalle.setMontoImpuesto(detalle.getBaseImponible()
            .multiply(tarifaIVA).divide(BigDecimal.valueOf(100)));
        detalle.setImpuestoNeto(detalle.getMontoImpuesto());
      } else {
        detalle.setMontoImpuesto(BigDecimal.ZERO);
        detalle.setImpuestoNeto(BigDecimal.ZERO);
      }

      detalle.setMontoTotalLinea(detalle.getSubTotal().add(detalle.getImpuestoNeto()));

      // Producto
      if (detalleReq.getProductoId() != null) {
        Producto producto = productoService.obtenerEntidadPorId(detalleReq.getProductoId());
        if (producto == null) {
          throw new ResourceNotFoundException("Producto no encontrado");
        }

        // Validar que el producto pertenezca a la empresa
        if (!producto.getEmpresa().getId().equals(empresaId)) {
          throw new BadRequestException("El producto no pertenece a la empresa");
        }

        detalle.setProducto(producto);

        // Actualizar precio de compra
        producto.setPrecioCompra(detalleReq.getPrecioUnitario());
        producto.setUltimoPrecioCompra(detalleReq.getPrecioUnitario());
        producto.setFechaUltimaCompra(LocalDateTime.now());
        productoService.save(producto);
      }

      // Acumular totales
      totalDescuentos = totalDescuentos.add(detalle.getMontoDescuento());
      totalImpuesto = totalImpuesto.add(detalle.getImpuestoNeto());

      if (detalleReq.getEsServicio()) {
        if (tarifaIVA.compareTo(BigDecimal.ZERO) > 0) {
          totalServiciosGravados = totalServiciosGravados.add(detalle.getSubTotal());
        } else {
          totalServiciosExentos = totalServiciosExentos.add(detalle.getSubTotal());
        }
      } else {
        if (tarifaIVA.compareTo(BigDecimal.ZERO) > 0) {
          totalMercanciasGravadas = totalMercanciasGravadas.add(detalle.getSubTotal());
        } else {
          totalMercanciasExentas = totalMercanciasExentas.add(detalle.getSubTotal());
        }
      }

      compra.addDetalle(detalle);
    }

    // Asignar totales a la compra
    compra.setTotalServiciosGravados(totalServiciosGravados);
    compra.setTotalServiciosExentos(totalServiciosExentos);
    compra.setTotalMercanciasGravadas(totalMercanciasGravadas);
    compra.setTotalMercanciasExentas(totalMercanciasExentas);

    BigDecimal totalGravado = totalServiciosGravados.add(totalMercanciasGravadas);
    BigDecimal totalExento = totalServiciosExentos.add(totalMercanciasExentas);

    compra.setTotalGravado(totalGravado);
    compra.setTotalExento(totalExento);
    compra.setTotalExonerado(BigDecimal.ZERO);

    BigDecimal totalVenta = totalGravado.add(totalExento);
    compra.setTotalVenta(totalVenta);
    compra.setTotalDescuentos(totalDescuentos);
    compra.setTotalVentaNeta(totalVenta.subtract(totalDescuentos));
    compra.setTotalImpuesto(totalImpuesto);
    compra.setTotalOtrosCargos(BigDecimal.ZERO);
    compra.setTotalComprobante(compra.getTotalVentaNeta().add(totalImpuesto));

    // Guardar compra
    compra = compraRepository.save(compra);

    return convertirADto(compra);
  }

  /**
   * Obtiene la tarifa de IVA según el código
   */
  private BigDecimal obtenerTarifaIVA(String codigoTarifa) {
    return switch (codigoTarifa) {
      case "01" -> BigDecimal.ZERO;      // 0%
      case "02" -> BigDecimal.ONE;       // 1%
      case "03" -> new BigDecimal("2");  // 2%
      case "04" -> new BigDecimal("4");  // 4%
      case "08" -> new BigDecimal("13"); // 13%
      default -> BigDecimal.ZERO;
    };
  }

  /**
   * Convierte entidad a DTO
   */
  private CompraDto convertirADto(Compra compra) {
    CompraDto dto = new CompraDto();
    dto.setId(compra.getId());
    dto.setEmpresaId(compra.getEmpresa().getId());
    dto.setEmpresaNombre(compra.getEmpresa().getNombreComercial());
    dto.setSucursalId(compra.getSucursal().getId());
    dto.setSucursalNombre(compra.getSucursal().getNombre());
    dto.setProveedorId(compra.getProveedor().getId());
    dto.setProveedorNombre(compra.getProveedor().getNombreComercial());
    dto.setProveedorIdentificacion(compra.getProveedor().getNumeroIdentificacion());
    dto.setTipoCompra(compra.getTipoCompra().toString());
    dto.setTipoDocumentoHacienda(compra.getTipoDocumentoHacienda() != null ?
        compra.getTipoDocumentoHacienda().toString() : null);
    dto.setNumeroDocumento(compra.getNumeroDocumento());
    dto.setClaveHacienda(compra.getClaveHacienda());
    dto.setConsecutivoHacienda(compra.getConsecutivoHacienda());
    dto.setFechaEmision(compra.getFechaEmision());
    dto.setFechaRecepcion(compra.getFechaRecepcion());
    dto.setCondicionVenta(compra.getCondicionVenta());
    dto.setPlazoCredito(compra.getPlazoCredito());
    dto.setMedioPago(compra.getMedioPago());
    dto.setMoneda(compra.getMoneda().getCodigo());
    dto.setTipoCambio(compra.getTipoCambio());
    dto.setTotalGravado(compra.getTotalGravado());
    dto.setTotalExento(compra.getTotalExento());
    dto.setTotalExonerado(compra.getTotalExonerado());
    dto.setTotalVenta(compra.getTotalVenta());
    dto.setTotalDescuentos(compra.getTotalDescuentos());
    dto.setTotalVentaNeta(compra.getTotalVentaNeta());
    dto.setTotalImpuesto(compra.getTotalImpuesto());
    dto.setTotalOtrosCargos(compra.getTotalOtrosCargos());
    dto.setTotalComprobante(compra.getTotalComprobante());
    dto.setEstado(compra.getEstado().toString());
    dto.setEstadoHacienda(compra.getEstadoBitacora() != null ?
        compra.getEstadoBitacora().toString() : null);
    dto.setMensajeHacienda(compra.getMensajeHacienda());
    dto.setObservaciones(compra.getObservaciones());
    dto.setCreatedAt(compra.getCreatedAt());
    dto.setUpdatedAt(compra.getUpdatedAt());

    // Convertir detalles
    List<CompraDetalleDto> detallesDto = compra.getDetalles().stream()
        .map(this::convertirDetalleADto)
        .collect(Collectors.toList());
    dto.setDetalles(detallesDto);

    return dto;
  }

  /**
   * Convierte detalle a DTO
   */
  private CompraDetalleDto convertirDetalleADto(CompraDetalle detalle) {
    CompraDetalleDto dto = new CompraDetalleDto();
    dto.setId(detalle.getId());
    dto.setNumeroLinea(detalle.getNumeroLinea());
    dto.setProductoId(detalle.getProducto() != null ? detalle.getProducto().getId() : null);
    dto.setProductoNombre(detalle.getProducto() != null ?
        detalle.getProducto().getNombre() : detalle.getDescripcion());
    dto.setCodigo(detalle.getCodigo());
    dto.setCodigoCabys(detalle.getCodigoCabys());
    dto.setDescripcion(detalle.getDescripcion());
    dto.setEsServicio(detalle.getEsServicio());
    dto.setCantidad(detalle.getCantidad());
    dto.setUnidadMedida(detalle.getUnidadMedida());
    dto.setPrecioUnitario(detalle.getPrecioUnitario());
    dto.setMontoTotal(detalle.getMontoTotal());
    dto.setMontoDescuento(detalle.getMontoDescuento());
    dto.setSubTotal(detalle.getSubTotal());
    dto.setCodigoTarifaIVA(detalle.getCodigoTarifaIVA());
    dto.setTarifaIVA(detalle.getTarifaIVA());
    dto.setMontoImpuesto(detalle.getMontoImpuesto());
    dto.setMontoTotalLinea(detalle.getMontoTotalLinea());
    return dto;
  }

  /**
   * Buscar compras por empresa
   */
  @Transactional(readOnly = true)
  public List<CompraDto> buscarPorEmpresa(Long empresaId) {
    return compraRepository.findByEmpresaIdOrderByFechaEmisionDesc(empresaId)
        .stream()
        .map(this::convertirADto)
        .collect(Collectors.toList());
  }

  /**
   * Buscar compras por sucursal
   */
  @Transactional(readOnly = true)
  public List<CompraDto> buscarPorSucursal(Long sucursalId) {
    return compraRepository.findBySucursalIdOrderByFechaEmisionDesc(sucursalId)
        .stream()
        .map(this::convertirADto)
        .collect(Collectors.toList());
  }

  /**
   * Obtener compra por ID
   */
  @Transactional(readOnly = true)
  public CompraDto obtenerPorId(Long id) {
    Compra compra = compraRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada"));
    return convertirADto(compra);
  }
}