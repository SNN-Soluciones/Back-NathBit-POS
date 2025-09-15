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
import java.util.Optional;
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
    analisis.setLineas(new ArrayList<>());

    try {
      // Procesar XML
      FacturaXmlDto facturaXml = xmlProcessorService.procesarFacturaXml(xmlContent);

      // Validar estructura básica
      if (facturaXml.getClave() == null || facturaXml.getClave().isEmpty()) {
        analisis.setEsValido(false);
        analisis.getErroresValidacion().add("XML inválido: No se encontró la clave del documento");
        return analisis;
      }

      // Llenar datos básicos
      analisis.setEsValido(true);
      analisis.setClave(facturaXml.getClave());
      analisis.setNumeroDocumento(facturaXml.getNumeroConsecutivo());
      analisis.setFechaEmision(facturaXml.getFechaEmision());
      analisis.setMoneda(facturaXml.getCodigoMoneda() != null ? facturaXml.getCodigoMoneda() : "CRC");
      analisis.setCondicionVenta(facturaXml.getCondicionVenta());
      analisis.setPlazoCredito(facturaXml.getPlazoCredito());
      analisis.setCantidadLineas(facturaXml.getDetalles() != null ? facturaXml.getDetalles().size() : 0);

      // Procesar emisor
      if (facturaXml.getEmisor() != null) {
        AnalisisXmlResponse.EmisorInfo emisorInfo = new AnalisisXmlResponse.EmisorInfo();
        emisorInfo.setIdentificacion(facturaXml.getEmisor().getNumeroIdentificacion());
        emisorInfo.setTipoIdentificacion(facturaXml.getEmisor().getTipoIdentificacion());
        emisorInfo.setNombre(facturaXml.getEmisor().getNombre());
        emisorInfo.setRazonSocial(facturaXml.getEmisor().getNombreComercial());
        emisorInfo.setTelefono(facturaXml.getEmisor().getTelefono());
        emisorInfo.setEmail(facturaXml.getEmisor().getCorreoElectronico());

        // Verificar si existe en el sistema
        if (empresaId != null) {
          Optional<Proveedor> proveedorOpt = proveedorRepository.findByNumeroIdentificacionAndEmpresaId(
              facturaXml.getEmisor().getNumeroIdentificacion(),
              empresaId
          );
          emisorInfo.setExisteEnSistema(proveedorOpt.isPresent());
          proveedorOpt.ifPresent(p -> emisorInfo.setProveedorId(p.getId()));
        } else {
          emisorInfo.setExisteEnSistema(false);
        }

        analisis.setEmisor(emisorInfo);
      }

      // Procesar líneas
      if (facturaXml.getDetalles() != null) {
        for (FacturaXmlDto.DetalleDto detalleXml : facturaXml.getDetalles()) {
          AnalisisXmlResponse.LineaAnalisis linea = new AnalisisXmlResponse.LineaAnalisis();
          linea.setNumeroLinea(detalleXml.getNumeroLinea());
          linea.setCodigo(detalleXml.getCodigo());
          linea.setCodigoCabys(detalleXml.getCodigoCabys());
          linea.setCantidad(detalleXml.getCantidad());
          linea.setUnidadMedida(detalleXml.getUnidadMedida());
          linea.setDescripcion(detalleXml.getDetalle());
          linea.setPrecioUnitario(detalleXml.getPrecioUnitario());
          linea.setMontoDescuento(detalleXml.getMontoDescuento() != null ? detalleXml.getMontoDescuento() : BigDecimal.ZERO);
          linea.setNaturalezaDescuento(detalleXml.getNaturalezaDescuento());
          linea.setMontoTotal(detalleXml.getSubTotal() != null ? detalleXml.getSubTotal() : detalleXml.getMontoTotal());

          // Procesar impuestos
          if (detalleXml.getImpuestos() != null && !detalleXml.getImpuestos().isEmpty()) {
            FacturaXmlDto.ImpuestoDto impuestoPrincipal = detalleXml.getImpuestos().get(0);
            linea.setCodigoTarifaIVA(impuestoPrincipal.getCodigoTarifa());
            linea.setTarifaIVA(impuestoPrincipal.getTarifa());
            linea.setMontoImpuesto(impuestoPrincipal.getMonto());
          } else {
            linea.setMontoImpuesto(BigDecimal.ZERO);
          }

          // Calcular monto total línea
          BigDecimal montoTotalLinea = linea.getMontoTotal().add(linea.getMontoImpuesto());
          linea.setMontoTotalLinea(montoTotalLinea);

          // Procesar exoneraciones
          if (detalleXml.getExoneracion() != null) {
            AnalisisXmlResponse.ExoneracionInfo exoneracion = new AnalisisXmlResponse.ExoneracionInfo();
            exoneracion.setTipoDocumento(detalleXml.getExoneracion().getTipoDocumento());
            exoneracion.setNumeroDocumento(detalleXml.getExoneracion().getNumeroDocumento());
            exoneracion.setNombreInstitucion(detalleXml.getExoneracion().getNombreInstitucion());
            exoneracion.setFechaEmision(detalleXml.getExoneracion().getFechaEmision());
            exoneracion.setPorcentajeExoneracion(detalleXml.getExoneracion().getPorcentajeExoneracion());
            exoneracion.setMontoExoneracion(detalleXml.getExoneracion().getMontoExoneracion());
            linea.setExoneracion(exoneracion);
          }

          // Verificar si producto existe
          if (empresaId != null && detalleXml.getCodigo() != null) {
            Optional<Producto> productoOpt = productoService.findByEmpresaIdAndCodigoCabys(
                empresaId,
                detalleXml.getCodigo()
            );
            linea.setExisteEnSistema(productoOpt.isPresent());
            productoOpt.ifPresent(p -> linea.setProductoId(p.getId()));

            if (!productoOpt.isPresent()) {
              AnalisisXmlResponse.ProductoNoEncontrado pne = new AnalisisXmlResponse.ProductoNoEncontrado();
              pne.setCodigo(detalleXml.getCodigo());
              pne.setDescripcion(detalleXml.getDetalle());
              pne.setCodigoCabys(detalleXml.getCodigoCabys());
              analisis.getProductosNoEncontrados().add(pne);
            }
          } else {
            linea.setExisteEnSistema(false);
          }

          analisis.getLineas().add(linea);
        }
      }

      // Procesar resumen de totales
      AnalisisXmlResponse.ResumenTotales resumen = new AnalisisXmlResponse.ResumenTotales();

      // Si el XML tiene la nueva estructura ResumenFactura
      if (facturaXml.getResumenFactura() != null) {
        resumen.setTotalGravado(facturaXml.getResumenFactura().getTotalGravado());
        resumen.setTotalExento(facturaXml.getResumenFactura().getTotalExento());
        resumen.setTotalExonerado(facturaXml.getResumenFactura().getTotalExonerado());
        resumen.setTotalVenta(facturaXml.getResumenFactura().getTotalVenta());
        resumen.setTotalDescuentos(facturaXml.getResumenFactura().getTotalDescuentos());
        resumen.setTotalVentaNeta(facturaXml.getResumenFactura().getTotalVentaNeta());
        resumen.setTotalImpuesto(facturaXml.getResumenFactura().getTotalImpuesto());
        resumen.setTotalIVADevuelto(facturaXml.getResumenFactura().getTotalIVADevuelto());
        resumen.setTotalOtrosCargos(facturaXml.getResumenFactura().getTotalOtrosCargos());
        resumen.setTotalComprobante(facturaXml.getResumenFactura().getTotalComprobante());
      } else {
        // Fallback para estructura antigua
        resumen.setTotalGravado(facturaXml.getTotalGravado() != null ? facturaXml.getTotalGravado() : BigDecimal.ZERO);
        resumen.setTotalExento(facturaXml.getTotalExento() != null ? facturaXml.getTotalExento() : BigDecimal.ZERO);
        resumen.setTotalExonerado(facturaXml.getTotalExonerado() != null ? facturaXml.getTotalExonerado() : BigDecimal.ZERO);
        resumen.setTotalVenta(facturaXml.getTotalVenta() != null ? facturaXml.getTotalVenta() : BigDecimal.ZERO);
        resumen.setTotalDescuentos(facturaXml.getTotalDescuentos() != null ? facturaXml.getTotalDescuentos() : BigDecimal.ZERO);
        resumen.setTotalVentaNeta(facturaXml.getTotalVentaNeta() != null ? facturaXml.getTotalVentaNeta() : BigDecimal.ZERO);
        resumen.setTotalImpuesto(facturaXml.getTotalImpuesto() != null ? facturaXml.getTotalImpuesto() : BigDecimal.ZERO);
        resumen.setTotalIVADevuelto(BigDecimal.ZERO);
        resumen.setTotalOtrosCargos(facturaXml.getTotalOtrosCargos() != null ? facturaXml.getTotalOtrosCargos() : BigDecimal.ZERO);
        resumen.setTotalComprobante(facturaXml.getTotalComprobante() != null ? facturaXml.getTotalComprobante() : BigDecimal.ZERO);
      }

      analisis.setResumenTotales(resumen);
      analisis.setTotalComprobante(resumen.getTotalComprobante());

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
        detalle.setCodigoTarifaIVA(detalleXml.getImpuestos().get(0).getCodigoTarifa());
        detalle.setTarifaIVA(detalleXml.getImpuestos().get(0).getTarifa());
        detalle.setMontoImpuesto(detalleXml.getImpuestos().get(0).getMonto() != null ?
            detalleXml.getImpuestos().get(0).getMonto() : BigDecimal.ZERO);
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

  private boolean sonDescripcionesSimilares(String desc1, String desc2) {
    if (desc1 == null || desc2 == null) return false;

    // Normalizar: quitar espacios extras, convertir a minúsculas
    String norm1 = desc1.trim().toLowerCase().replaceAll("\\s+", " ");
    String norm2 = desc2.trim().toLowerCase().replaceAll("\\s+", " ");

    // Verificar si una contiene a la otra
    return norm1.contains(norm2) || norm2.contains(norm1);
  }
}