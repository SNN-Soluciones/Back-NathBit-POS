package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.compra.CompraDto;
import com.snnsoluciones.backnathbitpos.dto.compra.CrearCompraDesdeXmlRequest;
import com.snnsoluciones.backnathbitpos.dto.compra.FacturaXmlDto;
import com.snnsoluciones.backnathbitpos.dto.mr.AceptarFacturaRequest;
import com.snnsoluciones.backnathbitpos.dto.mr.CrearProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.mr.FacturaRecibidaDto;
import com.snnsoluciones.backnathbitpos.dto.mr.RechazarFacturaRequest;
import com.snnsoluciones.backnathbitpos.dto.mr.ResumenTotalesDto;
import com.snnsoluciones.backnathbitpos.dto.mr.SubirXmlCompraRequest;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorDto;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorRequest;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.EstadoBitacora;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.impl.ModularHelperService;
import com.snnsoluciones.backnathbitpos.service.mr.MensajeReceptorXmlService;
import com.snnsoluciones.backnathbitpos.util.XmlProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensajeReceptorService {

  private final XmlProcessorService xmlProcessorService;
  private final HaciendaClient haciendaClient;
  private final MensajeReceptorXmlService mrXmlService;
  private final CompraService compraService;
  private final ProveedorService proveedorService;

  private final EmpresaRepository empresaRepository;
  private final SucursalRepository sucursalRepository;
  private final ProveedorRepository proveedorRepository;
  private final MensajeReceptorBitacoraRepository bitacoraRepository;
  private final CompraRepository compraRepository;
  private final ModularHelperService modularHelper;

  /*
   * Procesa un XML subido manualmente y retorna la info para que el usuario revise
   */

  /*
   * Procesa un XML subido manualmente y retorna la info para que el usuario revise
   */
  /**
   * Procesa un XML subido manualmente y retorna la info para que el usuario revise
   */
  @Transactional(readOnly = true)
  public FacturaRecibidaDto procesarXmlSubido(SubirXmlCompraRequest request) {
    log.info("Procesando XML subido para empresa: {}, sucursal: {}",
        request.empresaId(), request.sucursalId());

    try {
      // 1. Validar empresa y sucursal
      Empresa empresa = empresaRepository.findById(request.empresaId())
          .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

      Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
          .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

      if (!sucursal.getEmpresa().getId().equals(empresa.getId())) {
        throw new BadRequestException("La sucursal no pertenece a la empresa");
      }

      // 2. Parsear XML
      FacturaXmlDto facturaXml = xmlProcessorService.procesarFacturaXml(request.xmlContent());
      log.info("XML parseado exitosamente. Clave: {}", facturaXml.getClave());

      // 3. Validar que no exista ya procesada
      if (compraRepository.existsByClaveHacienda(facturaXml.getClave())) {
        throw new BadRequestException("Esta factura ya fue procesada anteriormente");
      }

      // Validar que no exista MR
      if (bitacoraRepository.findByClave(facturaXml.getClave()).isPresent()) {
        throw new BadRequestException("Ya existe un mensaje receptor para esta factura");
      }

      // 4. Validar con Hacienda usando HaciendaClient
      String estadoHacienda = "DESCONOCIDO";
      boolean puedeAceptar = false;

      try {
        // Obtener token
        HaciendaTokenResponse token = haciendaClient.getToken(
            HaciendaAuthParams.builder()
                .empresaId(empresa.getId())
                .username(empresa.getConfigHacienda().getUsuarioHacienda())
                .password(empresa.getConfigHacienda().getClaveHacienda())
                .clientId(empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION
                    ? "api-prod" : "api-test")
                .sandbox(empresa.getConfigHacienda().getAmbiente() != AmbienteHacienda.PRODUCCION)
                .build()
        );

        // Consultar estado de la factura
        boolean sandbox = empresa.getConfigHacienda().getAmbiente() != AmbienteHacienda.PRODUCCION;
        ConsultaEstadoResponse respuesta = haciendaClient.getEstado(
            token.getAccessToken(),
            sandbox,
            facturaXml.getClave()
        );

        if (respuesta != null && respuesta.getIndEstado() != null) {
          estadoHacienda = respuesta.getIndEstado().toUpperCase();
          puedeAceptar = "ACEPTADO".equals(estadoHacienda);
          log.info("Estado Hacienda para clave {}: {}", facturaXml.getClave(), estadoHacienda);
        } else {
          log.warn("Respuesta de Hacienda sin indEstado para clave: {}", facturaXml.getClave());
          estadoHacienda = "DESCONOCIDO";
        }

      } catch (Exception e) {
        log.warn("Error consultando estado en Hacienda: {}", e.getMessage());
        estadoHacienda = "ERROR_CONSULTA";
        puedeAceptar = false;
      }

      // 5. Buscar si el proveedor existe
      boolean proveedorExiste = false;
      ProveedorDto proveedorDto = null;

      if (facturaXml.getEmisor() != null &&
          facturaXml.getEmisor().getNumeroIdentificacion() != null) {

        Optional<Proveedor> proveedorOpt = proveedorRepository
            .findByEmpresaIdAndNumeroIdentificacion(
                empresa.getId(),
                facturaXml.getEmisor().getNumeroIdentificacion()
            );

        if (proveedorOpt.isPresent()) {
          proveedorExiste = true;
          Proveedor prov = proveedorOpt.get();
          proveedorDto = ProveedorDto.builder()
              .id(prov.getId())
              .numeroIdentificacion(prov.getNumeroIdentificacion())
              .nombreComercial(prov.getNombreComercial())
              .razonSocial(prov.getRazonSocial())
              .email(prov.getEmail())
              .telefono(prov.getTelefono())
              .build();
        } else {
          // Preparar DTO con datos del XML para crear
          proveedorDto = ProveedorDto.builder()
              .numeroIdentificacion(facturaXml.getEmisor().getNumeroIdentificacion())
              .nombreComercial(facturaXml.getEmisor().getNombreComercial())
              .razonSocial(facturaXml.getEmisor().getNombre())
              .email(facturaXml.getEmisor().getCorreoElectronico())
              .telefono(facturaXml.getEmisor().getTelefono())
              .build();
        }
      }

      // 6. Construir respuesta - USANDO LOS MÉTODOS CORRECTOS
      ResumenTotalesDto totalesDto = null;
      if (facturaXml.getResumenFactura() != null) {
        totalesDto = ResumenTotalesDto.builder()
            .totalVentaNeta(facturaXml.getResumenFactura().getTotalVentaNeta())
            .totalImpuesto(facturaXml.getResumenFactura().getTotalImpuesto())
            .totalComprobante(facturaXml.getResumenFactura().getTotalComprobante())
            .build();
      }

      return FacturaRecibidaDto.builder()
          .claveHacienda(facturaXml.getClave())
          .consecutivo(facturaXml.getNumeroConsecutivo())
          .fechaEmision(String.valueOf(facturaXml.getFechaEmision()))
          .proveedor(proveedorDto)
          .proveedorExiste(proveedorExiste)
          .detalles(facturaXml.getDetalles())
          .totales(totalesDto)
          .estadoHacienda(estadoHacienda)
          .puedeAceptar(puedeAceptar)
          .condicionVenta(facturaXml.getCondicionVenta())
          .plazoCredito(facturaXml.getPlazoCredito())
          .medioPago(facturaXml.getMedioPago())
          .build();

    } catch (Exception e) {
      log.error("Error procesando XML: ", e);
      throw new BadRequestException("Error procesando XML: " + e.getMessage());
    }
  }

  /*
   * Acepta una factura y crea la compra + mensaje receptor
   */
  /**
   * Acepta una factura y crea la compra + mensaje receptor
   */
  @Transactional
  public CompraDto aceptarFactura(AceptarFacturaRequest request) {
    log.info("Aceptando factura con clave: {}", request.claveHacienda());

    // 1. Validar que no exista
    if (compraRepository.existsByClaveHacienda(request.claveHacienda())) {
      throw new BadRequestException("Esta factura ya fue procesada");
    }

    if (bitacoraRepository.findByClave(request.claveHacienda()).isPresent()) {
      throw new BadRequestException("Ya existe un mensaje receptor para esta factura");
    }

    // 2. Validar/Crear proveedor
    Long proveedorId = request.proveedorId();

    if (proveedorId == null && request.nuevoProveedor() != null) {
      // Crear proveedor desde datos del XML
      log.info("Creando nuevo proveedor desde XML: {}",
          request.nuevoProveedor().getNumeroIdentificacion());

      proveedorId = crearProveedorDesdeDto(
          request.nuevoProveedor(),
          request.empresaId(),
          request.sucursalId()
      );

      log.info("Proveedor creado exitosamente con ID: {}", proveedorId);
    }

    if (proveedorId == null) {
      throw new BadRequestException(
          "Debe especificar un proveedor o proporcionar datos para crearlo");
    }

    // 3. Crear compra usando el servicio existente
    CrearCompraDesdeXmlRequest compraRequest = CrearCompraDesdeXmlRequest.builder()
        .xmlContent(request.xmlContent())
        .proveedorId(proveedorId)
        .tipoMensajeReceptor("05") // Aceptación total
        .procesarInventario(request.procesarInventario())
        .observaciones(request.observaciones())
        .build();

    CompraDto compra = compraService.crearCompraDesdeXml(
        request.empresaId(),
        request.sucursalId(),
        compraRequest,
        null // terminalId
    );

    log.info("Compra creada exitosamente con ID: {}", compra.getId());

    return compra;
  }

  /**
   * Rechaza una factura generando el mensaje receptor correspondiente
   */
  @Transactional
  public void rechazarFactura(RechazarFacturaRequest request) {
    log.info("Rechazando factura con clave: {}", request.claveHacienda());

    // Validar que no exista ya
    if (bitacoraRepository.findByClave(request.claveHacienda()).isPresent()) {
      throw new BadRequestException("Ya existe un mensaje receptor para esta factura");
    }

    // Validar tipo de rechazo
    if (!"06".equals(request.tipoRechazo()) && !"07".equals(request.tipoRechazo())) {
      throw new BadRequestException("Tipo de rechazo inválido. Debe ser 06 (Parcial) o 07 (Total)");
    }

    // Validar justificación
    if (request.justificacion() == null || request.justificacion().trim().length() < 5) {
      throw new BadRequestException("La justificación debe tener al menos 5 caracteres");
    }

    // Crear bitácora de rechazo (sin compra asociada)
    MensajeReceptorBitacora bitacora = new MensajeReceptorBitacora();
    bitacora.setCompraId(null); // No hay compra porque se rechaza
    bitacora.setClave(request.claveHacienda());
    bitacora.setEstado(EstadoBitacora.PENDIENTE);
    bitacora.setTipoMensaje(request.tipoRechazo());
    bitacora.setJustificacion(request.justificacion());
    bitacora.setConsecutivo(mrXmlService.generarConsecutivoMR(request.sucursalId()));
    bitacora.setIntentos(0);
    bitacora.setCreatedAt(LocalDateTime.now());
    bitacora.setUpdatedAt(LocalDateTime.now());

    bitacoraRepository.save(bitacora);

    log.info("Mensaje receptor de rechazo creado. El job lo procesará automáticamente.");
  }

  /**
   * Crea un proveedor desde el DTO recibido
   */
  private Long crearProveedorDesdeDto(CrearProveedorDto dto, Long empresaId, Long sucursalId) {

    // Validar que no exista ya
    if (proveedorRepository.existsByEmpresaIdAndNumeroIdentificacion(
        empresaId, dto.getNumeroIdentificacion())) {

      // Si ya existe, buscar y retornar su ID
      Optional<Proveedor> existente = proveedorRepository
          .findByEmpresaIdAndNumeroIdentificacion(empresaId, dto.getNumeroIdentificacion());

      if (existente.isPresent()) {
        log.info("Proveedor ya existe, usando ID: {}", existente.get().getId());
        return existente.get().getId();
      }
    }

    // Obtener empresa
    Empresa empresa = empresaRepository.findById(empresaId)
        .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

    // Determinar si debe asignarse a sucursal según configuración
    Sucursal sucursal = modularHelper.determinarSucursalParaEntidad(
        empresaId,
        sucursalId,
        "proveedor"
    );

    // Construir el request para el servicio
    ProveedorRequest proveedorRequest = ProveedorRequest.builder()
        .empresaId(empresaId)
        .sucursalId(sucursal != null ? sucursal.getId() : null)
        .tipoIdentificacion(TipoIdentificacion.valueOf(dto.getTipoIdentificacion()))
        .numeroIdentificacion(dto.getNumeroIdentificacion())
        .nombreComercial(dto.getNombreComercial())
        .razonSocial(dto.getRazonSocial() != null ?
            dto.getRazonSocial() : dto.getNombreComercial())
        .telefono(dto.getTelefono())
        .email(dto.getEmail())
        .direccion(dto.getDireccion())
        .diasCredito(dto.getDiasCredito() != null ? dto.getDiasCredito() : 0)
        .contactoNombre(dto.getContactoNombre())
        .contactoTelefono(dto.getContactoTelefono())
        .notas(dto.getNotas() != null ?
            dto.getNotas() : "Creado automáticamente desde factura XML")
        .build();

    // Crear usando el servicio
    ProveedorDto proveedorCreado = proveedorService.crear(proveedorRequest);

    return proveedorCreado.getId();
  }
}