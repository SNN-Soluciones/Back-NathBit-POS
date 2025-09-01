package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ActividadEconomicaDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteEmailDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUbicacionDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ExoneracionClienteDto;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteActividad;
import com.snnsoluciones.backnathbitpos.entity.ClienteEmail;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracionCabys;
import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import com.snnsoluciones.backnathbitpos.entity.EmpresaCAByS;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionCabysRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/pos/clientes")
@RequiredArgsConstructor
@Tag(name = "POS - Clientes", description = "Búsqueda rápida de clientes para punto de venta")
@PreAuthorize("hasAnyRole('CAJERO', 'MESERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
public class ClientePOSController {

  private final ClienteService clienteService;
  private final ClienteExoneracionCabysRepository exoneracionCabysRepository;
  private final ClienteExoneracionRepository exoneracionRepository;
  private final ClienteRepository clienteRepository;
  private final ProductoCrudService productoCrudService;
  private final ModelMapper modelMapper;

  @Operation(summary = "Buscar cliente por identificación exacta")
  @GetMapping("/buscar-identificacion")
  public ResponseEntity<ApiResponse<List<ClientePOSDto>>> buscarPorIdentificacion(
      @RequestParam Long empresaId,
      @RequestParam String identificacion) {

    try {
      List<Cliente> clientes = clienteService.buscarPorIdentificacion(empresaId, identificacion);

      if (clientes.isEmpty()) {
        return ResponseEntity.ok(ApiResponse.ok(
            "No se encontró cliente con identificación: " + identificacion,
            List.of()
        ));
      }

      List<ClientePOSDto> clientesDto = clientes.stream()
          .map(c -> modelMapper.map(c, ClientePOSDto.class))
          .collect(Collectors.toList());

      String mensaje = clientes.size() == 1
          ? "Cliente encontrado"
          : "Se encontraron " + clientes.size() + " clientes";

      return ResponseEntity.ok(ApiResponse.ok(mensaje, clientesDto));

    } catch (Exception e) {
      log.error("Error buscando cliente por identificación: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al buscar cliente: " + e.getMessage()));
    }
  }

  @GetMapping("/listar-pos")
  public ResponseEntity<ApiResponse<Page<ClientePOSDto>>> listarPorPos(
      @RequestParam Long empresaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    try {
      Page<ClientePOSDto> clientes = clienteService.buscarPorEmpresaActivosDTO(
          empresaId,
          PageRequest.of(page, size)
      );

      String mensaje = clientes.isEmpty()
          ? "No se encontraron clientes"
          : "Se encontraron " + clientes.getTotalElements() + " clientes";

      return ResponseEntity.ok(ApiResponse.ok(mensaje, clientes));
    } catch (Exception e) {
      log.error("Error en búsqueda rápida de clientes: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al buscar clientes: " + e.getMessage()));
    }
  }

  @Operation(summary = "Búsqueda rápida de clientes (cédula, nombre, email)")
  @GetMapping("/busqueda-rapida")
  public ResponseEntity<ApiResponse<Page<ClientePOSDto>>> busquedaRapida(
      @RequestParam Long empresaId,
      @RequestParam String termino,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    try {
      if (termino == null || termino.trim().length() < 3) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("El término de búsqueda debe tener al menos 3 caracteres"));
      }

      Page<ClientePOSDto> clientes = clienteService.buscarPorEmpresaDto(
          empresaId,
          termino.trim(),
          PageRequest.of(page, size)
      );

      String mensaje = clientes.isEmpty()
          ? "No se encontraron clientes"
          : "Se encontraron " + clientes.getTotalElements() + " clientes";

      return ResponseEntity.ok(ApiResponse.ok(mensaje, clientes));

    } catch (Exception e) {
      log.error("Error en búsqueda rápida de clientes: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al buscar clientes: " + e.getMessage()));
    }
  }

  @GetMapping("/validar-exoneracion/cliente")
  public ResponseEntity<ApiResponse<Boolean>> validarExoneracionCliente(
      @RequestParam Long clienteId,
      @RequestParam Long productoId
  ) {
    String cabys = null;
    try {
      // 1) Producto + CAByS
      var producto = productoCrudService.obtenerEntidadPorId(productoId);
      if (producto == null) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Producto no encontrado: " + productoId));
      }

      cabys = Optional.ofNullable(producto.getEmpresaCabys())
          .map(EmpresaCAByS::getCodigoCabys)
          .map(CodigoCAByS::getCodigo)
          .orElse(null);

      if (cabys == null || !cabys.matches("\\d{13}")) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Producto sin CAByS válido (13 dígitos): " + cabys));
      }

      // 2) Cliente
      var clienteOpt = clienteRepository.findById(clienteId);
      if (clienteOpt.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("Cliente no encontrado: " + clienteId));
      }

      // 3) Exoneración vigente (toma la última activa)
      var exOpt = exoneracionRepository
          .findByClienteIdAndActivoTrue(clienteId); // <-- agrega este método en el repo

      if (exOpt.isEmpty()) {
        return ResponseEntity.ok(ApiResponse.ok("Cliente sin exoneración activa", false));
      }

      var ex = exOpt.get(0);
      if (!ex.estaVigente()) {
        return ResponseEntity.ok(ApiResponse.ok("Exoneración no vigente", false));
      }

      // 4) ¿Aplica para el CAByS?
      boolean aplica = ex.aplicaParaCabys(
          cabys,
          c -> exoneracionCabysRepository.existsByExoneracionIdAndCabys_Codigo(ex.getId(), c)
      );

      String msg = aplica
          ? String.format("Exoneración vigente (%s%%) aplica al CAByS %s",
          ex.getPorcentajeExoneracion(), cabys)
          : (Boolean.TRUE.equals(ex.getPoseeCabys())
              ? "CAByS no autorizado en la lista de la exoneración"
              : "La exoneración no aplica a este CAByS");

      return ResponseEntity.ok(ApiResponse.ok(msg, aplica));

    } catch (Exception e) {
      log.error("Error validando exoneración (clienteId={}, cabys={}): {}", clienteId, cabys,
          e.getMessage(), e);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al validar exoneración: " + e.getMessage()));
    }
  }

  @Operation(summary = "Crear cliente rápido desde POS")
  @PostMapping("/crear-rapido")
  public ResponseEntity<ApiResponse<ClienteDTO>> crearClienteRapido(
      @RequestBody ClientePOSDto request,
      @RequestParam Long empresaId) {

    try {
      // Validaciones básicas
      if (request.getNumeroIdentificacion() == null || request.getNumeroIdentificacion().trim()
          .isEmpty()) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("La identificación es requerida"));
      }

      Cliente clienteCreado = clienteService.crear(request, empresaId);
      ClienteDTO clienteDto = modelMapper.map(clienteCreado, ClienteDTO.class);

      return ResponseEntity.ok(ApiResponse.ok("Cliente creado exitosamente", clienteDto));

    } catch (Exception e) {
      log.error("Error creando cliente rápido: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Error al crear cliente: " + e.getMessage()));
    }
  }

  @Operation(summary = "Obtener clientes frecuentes")
  @GetMapping("/frecuentes")
  public ResponseEntity<ApiResponse<List<ClientePOSDto>>> clientesFrecuentes(
      @RequestParam Long empresaId,
      @RequestParam(defaultValue = "10") int limite) {

    // TODO: Implementar cuando tengamos el histórico de ventas
    return ResponseEntity.ok(ApiResponse.ok(
        "Funcionalidad pendiente de implementar",
        List.of()
    ));
  }

  @Operation(summary = "Obtener un cliente en específico por id")
  @GetMapping
  public ResponseEntity<ApiResponse<ClientePOSDto>> obtenerPorId(@RequestParam Long clienteId){
    Cliente cliente = clienteService.obtenerPorId(clienteId);

    ClientePOSDto clienteDto = ClientePOSDto.builder()
        .id(cliente.getId())
        .tipoIdentificacion(cliente.getTipoIdentificacion())
        .numeroIdentificacion(cliente.getNumeroIdentificacion())
        .razonSocial(cliente.getRazonSocial())
        .telefonoNumero(cliente.getTelefonoNumero())
        .tieneExoneracion(cliente.getTieneExoneracion())
        .activo(cliente.getActivo())
        .inscritoHacienda(cliente.getInscritoHacienda())
        .permiteCredito(cliente.getPermiteCredito())
        .build();

    // Mapear emails
    if (cliente.getClienteEmails() != null) {
      Set<ClienteEmailDTO> emails = new HashSet<>();
      for (ClienteEmail email : cliente.getClienteEmails()) {
        ClienteEmailDTO emailDto = new ClienteEmailDTO();
        emailDto.setId(email.getId());
        emailDto.setEmail(email.getEmail());
        emailDto.setEsPrincipal(email.getEsPrincipal());
        emailDto.setVecesUsado(email.getVecesUsado());
        emailDto.setUltimoUso(email.getUltimoUso());
        emails.add(emailDto);
      }
      clienteDto.setClienteEmails(emails);
    }

    // Mapear ubicación CORRECTAMENTE
    if (cliente.getUbicacion() != null) {
      ClienteUbicacionDTO ubicDto = new ClienteUbicacionDTO();

      // Mapear IDs, no objetos completos
      if (cliente.getUbicacion().getProvincia() != null) {
        ubicDto.setProvincia(cliente.getUbicacion().getProvincia().getId());
      }
      if (cliente.getUbicacion().getCanton() != null) {
        ubicDto.setCanton(cliente.getUbicacion().getCanton().getId());
      }
      if (cliente.getUbicacion().getDistrito() != null) {
        ubicDto.setDistrito(cliente.getUbicacion().getDistrito().getId());
      }
      if (cliente.getUbicacion().getBarrio() != null) {
        ubicDto.setBarrio(cliente.getUbicacion().getBarrio().getId());
      }
      ubicDto.setOtrasSenas(cliente.getUbicacion().getOtrasSenas());

      clienteDto.setUbicacion(ubicDto);
    }

    // Mapear actividades
    if (cliente.getActividades() != null) {
      Set<ActividadEconomicaDto> actividades = new HashSet<>();
      for (ClienteActividad ca : cliente.getActividades()) {
        ActividadEconomicaDto actDto = new ActividadEconomicaDto();
        actDto.setCodigo(ca.getCodigoActividad());
        actDto.setDescripcion(ca.getDescripcion());
        actividades.add(actDto);
      }
      clienteDto.setActividades(actividades);
    }

    // Mapear exoneración vigente
    cliente.getExoneraciones().stream()
        .filter(e -> Boolean.TRUE.equals(e.getActivo()))
        .filter(e -> e.getFechaVencimiento() == null || e.getFechaVencimiento().isAfter(LocalDate.now()))
        .findFirst()
        .ifPresent(exo -> {
          ExoneracionClienteDto exoDto = new ExoneracionClienteDto();
          exoDto.setTipoDocumento(exo.getTipoDocumento().name());
          exoDto.setNumeroDocumento(exo.getNumeroDocumento());
          exoDto.setNombreInstitucion(exo.getNombreInstitucion());
          exoDto.setFechaEmision(exo.getFechaEmision());
          exoDto.setFechaVencimiento(exo.getFechaVencimiento());
          exoDto.setPorcentajeExoneracion(exo.getPorcentajeExoneracion());
          exoDto.setCodigoAutorizacion(exo.getCodigoAutorizacion());
          exoDto.setNumeroAutorizacion(exo.getNumeroAutorizacion());
          exoDto.setPoseeCabys(exo.getPoseeCabys());

          if (exo.getCabysAutorizados() != null) {
            List<String> cabys = new ArrayList<>();
            for (ClienteExoneracionCabys ec : exo.getCabysAutorizados()) {
              cabys.add(ec.getCabys().getCodigo());
            }
            exoDto.setCodigosCabys(cabys);
          }

          clienteDto.setExoneracion(exoDto);
        });

    return ResponseEntity.ok(ApiResponse.ok("Cliente encontrado", clienteDto));
  }
}