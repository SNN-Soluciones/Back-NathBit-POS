package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.cliente.ActividadEconomicaDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteEmailDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUbicacionDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ExoneracionClienteDto;
import com.snnsoluciones.backnathbitpos.entity.ActividadEconomica;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteActividad;
import com.snnsoluciones.backnathbitpos.entity.ClienteEmail;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracionCabys;
import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.repository.ActividadEconomicaRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteActividadRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionCabysRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteUbicacionRepository;
import com.snnsoluciones.backnathbitpos.repository.CodigoCABySRepository;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.UbicacionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServiceImpl implements ClienteService {

  private final ClienteRepository clienteRepository;
  private final ClienteUbicacionRepository ubicacionRepository;
  private final ClienteExoneracionRepository exoneracionRepository;
  private final ClienteExoneracionCabysRepository exoneracionCabysRepository;
  private final CodigoCABySRepository codigoCabysRepository;
  private final UbicacionService ubicacionService;
  private final EmpresaService empresaService;
  private final ModularHelperService modularHelper;
  private final ClienteActividadRepository clienteActividadRepository;
  private final ActividadEconomicaRepository actividadEconomicaRepository;
  private final RestTemplate restTemplate;

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
  );
  private static final int MAX_EMAILS = 4;

  @Override
  public Cliente crear(ClientePOSDto dto, Long empresaId) {
    // =========================
    // 0) Validaciones mínimas
    // =========================
    validarMinimo(dto);

    final TipoIdentificacion tipo = Objects.requireNonNull(dto.getTipoIdentificacion(),
        "tipoIdentificacion requerido");
    final String numeroId = normalizarIdentificacion(dto.getNumeroIdentificacion());
    if (!tipo.esValido(numeroId)) {
      throw new IllegalArgumentException("Identificación no válida para el tipo " + tipo.name());
    }
    if (empresaId == null) {
      throw new IllegalArgumentException("empresaId es requerido");
    }

    Long sucursalId = dto.getSucursalId() != null ? dto.getSucursalId() : null;

    Sucursal sucursal = modularHelper.determinarSucursalParaEntidad(empresaId, sucursalId, "cliente");

    // =========================
    // 1) Empresa
    // =========================
    final Empresa empresa = empresaService.buscarPorId(empresaId);

    // =========================
    // 2) Cliente base
    // =========================
    final Cliente cliente = new Cliente();
    cliente.setEmpresa(empresa);
    cliente.setSucursal(sucursal);
    cliente.setTipoIdentificacion(tipo);
    cliente.setNumeroIdentificacion(numeroId);
    cliente.setRazonSocial(StringUtils.trimToEmpty(dto.getRazonSocial()));     // primer email o CSV
    cliente.setTelefonoCodigoPais("506");                                  // CR por defecto
    cliente.setTelefonoNumero(StringUtils.trimToEmpty(dto.getTelefonoNumero()));
    cliente.setPermiteCredito(Boolean.TRUE.equals(dto.getPermiteCredito()));
    cliente.setInscritoHacienda(Boolean.TRUE.equals(dto.getInscritoHacienda()));
    cliente.setActivo(Boolean.TRUE.equals(dto.getActivo()));

    if (dto.getActividades() != null && !dto.getActividades().isEmpty()) {
      for (ActividadEconomicaDto actividadDto : dto.getActividades()) {
        if (actividadDto.getCodigo() != null && actividadDto.getDescripcion() != null) {
          ClienteActividad actividad = ClienteActividad.builder()
              .cliente(cliente)
              .codigoActividad(actividadDto.getCodigo())
              .descripcion(actividadDto.getDescripcion())
              .build();

          cliente.getActividades().add(actividad);
        }
      }
    }

    // Agregar emails antes de guardar
    if (dto.getClienteEmails() != null && !dto.getClienteEmails().isEmpty()) {
      for (ClienteEmailDTO emailDto : dto.getClienteEmails()) {
        if (emailDto.getEmail() != null) {
          ClienteEmail clienteEmail = ClienteEmail.builder()
              .cliente(cliente)
              .email(emailDto.getEmail().trim().toLowerCase())
              .esPrincipal(emailDto.getEsPrincipal() != null ? emailDto.getEsPrincipal() : false)
              .build();
          cliente.getClienteEmails().add(clienteEmail);
        }
      }
    }

    // =========================
// 3) Ubicación (Cliente es dueño)
// =========================
    if (dto.getUbicacion() != null) {
      var u = dto.getUbicacion();

      // Solo si TODOS los IDs vienen presentes
      boolean tieneIds =
          u.getProvincia() != null &&
              u.getCanton() != null &&
              u.getDistrito() != null &&
              u.getBarrio() != null;

      if (tieneIds) {
        var provincia = ubicacionService.buscarProvinciaPorId(u.getProvincia()).orElse(null);
        var canton = ubicacionService.buscarCantonPorId(u.getCanton()).orElse(null);
        var distrito = ubicacionService.buscarDistritoPorId(u.getDistrito()).orElse(null);
        var barrio = ubicacionService.buscarBarrioPorId(u.getBarrio()).orElse(null);

        if (provincia != null && canton != null && distrito != null && barrio != null) {
          var ubic = new ClienteUbicacion();
          ubic.setProvincia(provincia);
          ubic.setCanton(canton);
          ubic.setDistrito(distrito);
          ubic.setBarrio(barrio);
          ubic.setOtrasSenas(u.getOtrasSenas());
          cliente.setUbicacion(ubic); // Cliente es dueño
        }
      }
    }

    // =========================
    // 4) Exoneración (Cliente es dueño vía @OneToMany + @JoinColumn)
    // =========================
    if (dto.getExoneracion() != null) {
      final var exDto = dto.getExoneracion();

      // Validaciones clave (simplificadas — puedes endurecer si quieres)
      if (StringUtils.isBlank(exDto.getCodigoAutorizacion())) {
        throw new IllegalArgumentException("codigoAutorizacion es requerido para la exoneración");
      }
      if (exDto.getPorcentajeExoneracion() == null) {
        throw new IllegalArgumentException("porcentajeExoneracion es requerido");
      }
      if (exDto.getFechaEmision() == null) {
        throw new IllegalArgumentException("fechaEmision es requerida");
      }
      if (exDto.getFechaVencimiento() != null && exDto.getFechaVencimiento()
          .isBefore(exDto.getFechaEmision())) {
        throw new IllegalArgumentException("fechaVencimiento no puede ser anterior a fechaEmision");
      }

      final ClienteExoneracion exo = new ClienteExoneracion();
      exo.setTipoDocumento(
          mapTipoDocumento(exDto.getTipoDocumento())); // mapea "04"/"EXONERACION" a tu enum
      exo.setNumeroDocumento(StringUtils.trimToEmpty(exDto.getNumeroDocumento()));
      exo.setNombreInstitucion(StringUtils.trimToEmpty(exDto.getNombreInstitucion()));
      exo.setFechaEmision(exDto.getFechaEmision());        // ya es LocalDate
      exo.setFechaVencimiento(exDto.getFechaVencimiento());// ya es LocalDate (o null)
      exo.setPorcentajeExoneracion(parsePorcentaje(exDto.getPorcentajeExoneracion()));
      exo.setCategoriaCompra(exDto.getCategoriaCompra());
      exo.setMontoMaximo(exDto.getMontoMaximo());
      exo.setActivo(true);

      // Campos faltantes en tu primer intento (muy importantes)
      exo.setCodigoAutorizacion(StringUtils.trimToEmpty(exDto.getCodigoAutorizacion()));
      exo.setNumeroAutorizacion(exDto.getNumeroAutorizacion());
      exo.setPoseeCabys(Boolean.TRUE.equals(exDto.getPoseeCabys()));

      // CAByS autorizados (si aplica). Valida 13 dígitos y evita nulls.
      if (exDto.getCodigosCabys() != null && !exDto.getCodigosCabys().isEmpty()) {
        final Set<ClienteExoneracionCabys> links = new HashSet<>();
        for (String codigoCabys : exDto.getCodigosCabys()) {
          if (codigoCabys == null || !codigoCabys.matches("\\d{13}")) {
            continue;
          }
          final var cabys = codigoCabysRepository.findByCodigo(codigoCabys).orElse(null);
          if (cabys == null) {
            continue;
          }

          final var link = new ClienteExoneracionCabys();
          link.setCabys(cabys);
          link.setExoneracion(exo); // dueño natural del link es ClienteExoneracion
          links.add(link);
        }
        exo.setCabysAutorizados(links);
      }

      // Como Cliente es dueño del OneToMany, basta con agregar a la colección:
      cliente.getExoneraciones().add(exo);

      // marca resumen en cliente
      cliente.setTieneExoneracion(true);
    } else {
      cliente.setTieneExoneracion(false);
    }

    // =========================
    // 5) Persistir todo (cascade desde Cliente)
    // =========================
    return clienteRepository.save(cliente);
  }

  @Override
  public Cliente actualizar(Long id, ClientePOSDto clienteActualizado) {
    log.info("Actualizando cliente ID: {}", id);

    Cliente clienteExistente = obtenerPorId(id);

    if (clienteExistente == null) {
      throw new IllegalArgumentException("Cliente no encontrado");
    }

    // Actualizar campos básicos del DTO
    if (clienteActualizado.getRazonSocial() != null) {
      clienteExistente.setRazonSocial(clienteActualizado.getRazonSocial());
    }
    if (clienteActualizado.getTelefonoNumero() != null) {
      clienteExistente.setTelefonoNumero(clienteActualizado.getTelefonoNumero());
    }

    // Validaciones
    if (clienteActualizado.getClienteEmails() != null) {
      // Limpiar emails existentes
      clienteExistente.getClienteEmails().clear();

      // Agregar nuevos
      for (ClienteEmailDTO emailDto : clienteActualizado.getClienteEmails()) {
        if (emailDto.getEmail() != null) {
          ClienteEmail clienteEmail = ClienteEmail.builder()
              .cliente(clienteExistente)
              .email(emailDto.getEmail().trim().toLowerCase())
              .esPrincipal(emailDto.getEsPrincipal() != null ? emailDto.getEsPrincipal() : false)
              .build();
          clienteExistente.getClienteEmails().add(clienteEmail);
        }
      }

      // Validar que no quede sin emails
      if (clienteExistente.getClienteEmails().isEmpty()) {
        throw new IllegalArgumentException("El cliente debe tener al menos un email");
      }
    }

    // Actualizar campos
    clienteExistente.setTipoIdentificacion(clienteActualizado.getTipoIdentificacion());
    clienteExistente.setNumeroIdentificacion(clienteActualizado.getNumeroIdentificacion());
    clienteExistente.setRazonSocial(clienteActualizado.getRazonSocial());
    clienteExistente.setTelefonoNumero(clienteActualizado.getTelefonoNumero());
    clienteExistente.setPermiteCredito(clienteActualizado.getPermiteCredito());

    return clienteRepository.save(clienteExistente);
  }

  @Override
  @Transactional(readOnly = true)
  public Cliente obtenerPorId(Long id) {
    return clienteRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
  }

  @Override
  public Optional<Cliente> findById(Long id) {
    return clienteRepository.findById(id);
  }

  @Override
  public void eliminar(Long id) {
    log.info("Eliminando cliente ID: {}", id);
    Cliente cliente = obtenerPorId(id);
    clienteRepository.delete(cliente);
  }

  @Override
  public void save(Cliente cliente) {
    this.clienteRepository.save(cliente);
  }

  @Override
  public Page<ClientePOSDto> buscarPorEmpresaActivosDTO(Long empresaId, Pageable pageable) {
    Page<Cliente> clientes = clienteRepository.findAllByEmpresaId(empresaId, pageable);

    return clientes.map(cliente -> {
      ClientePOSDto dto = new ClientePOSDto();
      dto.setId(cliente.getId());
      dto.setTipoIdentificacion(cliente.getTipoIdentificacion());
      dto.setNumeroIdentificacion(cliente.getNumeroIdentificacion());
      dto.setRazonSocial(cliente.getRazonSocial());
      dto.setTelefonoNumero(cliente.getTelefonoNumero());
      dto.setPermiteCredito(cliente.getPermiteCredito());
      dto.setInscritoHacienda(cliente.getInscritoHacienda());
      dto.setTieneExoneracion(cliente.getTieneExoneracion());
      dto.setActivo(cliente.getActivo());

      // NUEVO: Mapear emails
      if (cliente.getClienteEmails() != null && !cliente.getClienteEmails().isEmpty()) {
        Set<ClienteEmailDTO> emailDtos = cliente.getClienteEmails().stream()
            .map(email -> {
              ClienteEmailDTO emailDto = new ClienteEmailDTO();
              emailDto.setId(email.getId());
              emailDto.setEmail(email.getEmail());
              emailDto.setEsPrincipal(email.getEsPrincipal());
              emailDto.setUltimoUso(email.getUltimoUso());
              emailDto.setVecesUsado(email.getVecesUsado());
              return emailDto;
            })
            .sorted((e1, e2) -> {
              // Ordenar por frecuencia de uso (más usado primero)
              int compareVeces = e2.getVecesUsado().compareTo(e1.getVecesUsado());
              if (compareVeces != 0) return compareVeces;

              // Si tienen el mismo uso, ordenar por último uso
              if (e1.getUltimoUso() != null && e2.getUltimoUso() != null) {
                return e2.getUltimoUso().compareTo(e1.getUltimoUso());
              }

              return 0;
            })
            .collect(Collectors.toCollection(LinkedHashSet::new)); // Mantener orden

        dto.setClienteEmails(emailDtos);
      } else {
        // Si no hay emails, crear set vacío
        dto.setClienteEmails(new HashSet<>());
      }

      // Ubicación
      if (cliente.getUbicacion() != null) {
        var u = cliente.getUbicacion();
        var ubicDto = new ClienteUbicacionDTO();
        ubicDto.setProvincia(u.getProvincia() != null ? u.getProvincia().getId() : null);
        ubicDto.setCanton(u.getCanton() != null ? u.getCanton().getId() : null);
        ubicDto.setDistrito(u.getDistrito() != null ? u.getDistrito().getId() : null);
        ubicDto.setBarrio(u.getBarrio() != null ? u.getBarrio().getId() : null);
        ubicDto.setOtrasSenas(u.getOtrasSenas());
        dto.setUbicacion(ubicDto);
      }

      if (cliente.getActividades() != null && !cliente.getActividades().isEmpty()) {
        Set<ActividadEconomicaDto> actividadesDto = new HashSet<>();
        for (ClienteActividad ca : cliente.getActividades()) {
          ActividadEconomicaDto actDto = new ActividadEconomicaDto();
          actDto.setCodigo(ca.getCodigoActividad());
          actDto.setDescripcion(ca.getDescripcion());
          actividadesDto.add(actDto);
        }
        dto.setActividades(actividadesDto);
      } else {
        dto.setActividades(new HashSet<>()); // Set vacío si no hay actividades
      }

      // Exoneración (si tiene)
      if (cliente.getExoneraciones() != null && !cliente.getExoneraciones().isEmpty()) {
        var exo = cliente.getExoneraciones().iterator().next(); // ejemplo: tomamos la 1ª
        var exoDto = new ExoneracionClienteDto();
        exoDto.setTipoDocumento(
            exo.getTipoDocumento() != null ? exo.getTipoDocumento().name() : null);
        exoDto.setNumeroDocumento(exo.getNumeroDocumento());
        exoDto.setNombreInstitucion(exo.getNombreInstitucion());
        exoDto.setFechaEmision(exo.getFechaEmision());
        exoDto.setFechaVencimiento(exo.getFechaVencimiento());
        exoDto.setPorcentajeExoneracion(exo.getPorcentajeExoneracion());
        exoDto.setCategoriaCompra(exo.getCategoriaCompra());
        exoDto.setMontoMaximo(exo.getMontoMaximo());
        exoDto.setCodigoAutorizacion(exo.getCodigoAutorizacion());
        exoDto.setNumeroAutorizacion(exo.getNumeroAutorizacion());
        exoDto.setPoseeCabys(exo.getPoseeCabys());
        exoDto.setCodigoInstitucion(exo.getCodigoInstitucion());

        // CABYS autorizados
        if (exo.getCabysAutorizados() != null && !exo.getCabysAutorizados().isEmpty()) {
          exoDto.setCodigosCabys(
              exo.getCabysAutorizados().stream()
                  .map(link -> link.getCabys().getCodigo())
                  .toList()
          );
        }

        dto.setExoneracion(exoDto);
      }

      return dto;
    });
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Cliente> buscarPorEmpresa(Long empresaId, String busqueda, Pageable pageable) {
    return clienteRepository.buscarPorEmpresa(empresaId, busqueda, pageable);
  }

  @Override
  public Page<ClientePOSDto> buscarPorEmpresaDto(Long empresaId, String busqueda,
      Pageable pageable) {
    Page<Cliente> clientes;

    if (StringUtils.isNotBlank(busqueda)) {
      clientes = clienteRepository
          .buscarPorEmpresa(empresaId, "%" + busqueda.trim().toLowerCase() + "%", pageable);
    } else {
      clientes = clienteRepository.findAllByEmpresaId(empresaId, pageable);
    }

    return clientes.map(cliente -> {
      ClientePOSDto dto = new ClientePOSDto();
      dto.setId(cliente.getId());
      dto.setTipoIdentificacion(cliente.getTipoIdentificacion());
      dto.setNumeroIdentificacion(cliente.getNumeroIdentificacion());
      dto.setRazonSocial(cliente.getRazonSocial());
      dto.setTelefonoNumero(cliente.getTelefonoNumero());
      dto.setPermiteCredito(cliente.getPermiteCredito());
      dto.setInscritoHacienda(cliente.getInscritoHacienda());
      dto.setTieneExoneracion(cliente.getTieneExoneracion());
      dto.setActivo(cliente.getActivo());

      if (cliente.getClienteEmails() != null && !cliente.getClienteEmails().isEmpty()) {
        Set<ClienteEmailDTO> emailDtos = cliente.getClienteEmails().stream()
            .map(email -> {
              ClienteEmailDTO emailDto = new ClienteEmailDTO();
              emailDto.setId(email.getId());
              emailDto.setEmail(email.getEmail());
              emailDto.setEsPrincipal(email.getEsPrincipal());
              emailDto.setUltimoUso(email.getUltimoUso());
              emailDto.setVecesUsado(email.getVecesUsado());
              return emailDto;
            })
            .sorted((e1, e2) -> {
              // Ordenar por frecuencia de uso
              int compareVeces = e2.getVecesUsado().compareTo(e1.getVecesUsado());
              if (compareVeces != 0) return compareVeces;

              // Si tienen el mismo uso, ordenar por último uso
              if (e1.getUltimoUso() != null && e2.getUltimoUso() != null) {
                return e2.getUltimoUso().compareTo(e1.getUltimoUso());
              }

              return 0;
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));

        dto.setClienteEmails(emailDtos);
      } else {
        dto.setClienteEmails(new HashSet<>());
      }

      // Ubicación
      if (cliente.getUbicacion() != null) {
        var u = cliente.getUbicacion();
        var ubicDto = new ClienteUbicacionDTO();
        ubicDto.setProvincia(u.getProvincia() != null ? u.getProvincia().getId() : null);
        ubicDto.setCanton(u.getCanton() != null ? u.getCanton().getId() : null);
        ubicDto.setDistrito(u.getDistrito() != null ? u.getDistrito().getId() : null);
        ubicDto.setBarrio(u.getBarrio() != null ? u.getBarrio().getId() : null);
        ubicDto.setOtrasSenas(u.getOtrasSenas());
        dto.setUbicacion(ubicDto);
      }

      // Exoneración (primera activa)
      if (cliente.getExoneraciones() != null && !cliente.getExoneraciones().isEmpty()) {
        var exo = cliente.getExoneraciones().iterator().next();
        var exoDto = new ExoneracionClienteDto();
        exoDto.setTipoDocumento(
            exo.getTipoDocumento() != null ? exo.getTipoDocumento().name() : null);
        exoDto.setNumeroDocumento(exo.getNumeroDocumento());
        exoDto.setNombreInstitucion(exo.getNombreInstitucion());
        exoDto.setFechaEmision(exo.getFechaEmision());
        exoDto.setFechaVencimiento(exo.getFechaVencimiento());
        exoDto.setPorcentajeExoneracion(exo.getPorcentajeExoneracion());
        exoDto.setCategoriaCompra(exo.getCategoriaCompra());
        exoDto.setMontoMaximo(exo.getMontoMaximo());
        exoDto.setCodigoAutorizacion(exo.getCodigoAutorizacion());
        exoDto.setNumeroAutorizacion(exo.getNumeroAutorizacion());
        exoDto.setPoseeCabys(exo.getPoseeCabys());

        if (exo.getCabysAutorizados() != null && !exo.getCabysAutorizados().isEmpty()) {
          exoDto.setCodigosCabys(
              exo.getCabysAutorizados().stream()
                  .map(link -> link.getCabys().getCodigo())
                  .toList()
          );
        }

        dto.setExoneracion(exoDto);
      }

      return dto;
    });
  }

  @Override
  @Transactional(readOnly = true)
  public List<Cliente> buscarPorIdentificacion(Long empresaId, String numeroIdentificacion) {
    return clienteRepository.findByEmpresaIdAndNumeroIdentificacionAndActivoTrue(
        empresaId, numeroIdentificacion
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<Cliente> obtenerClientesConExoneracion(Long empresaId) {
    return clienteRepository.findClientesConExoneracionActiva(empresaId);
  }

  // Gestión de ubicación
  @Override
  public ClienteUbicacion guardarUbicacion(Long clienteId, ClienteUbicacion ubicacion) {
    log.info("Guardando ubicación para cliente ID: {}", clienteId);

    Cliente cliente = obtenerPorId(clienteId);

    // Verificar si ya tiene ubicación
    ClienteUbicacion ubicacionExistente = ubicacionRepository.findByClienteId(clienteId)
        .orElse(null);

    if (ubicacionExistente != null) {
      // Actualizar existente
      ubicacionExistente.setProvincia(ubicacion.getProvincia());
      ubicacionExistente.setCanton(ubicacion.getCanton());
      ubicacionExistente.setDistrito(ubicacion.getDistrito());
      ubicacionExistente.setBarrio(ubicacion.getBarrio());
      ubicacionExistente.setOtrasSenas(ubicacion.getOtrasSenas());
      return ubicacionRepository.save(ubicacionExistente);
    } else {
      // Crear nueva
      ubicacion.setCliente(cliente);
      return ubicacionRepository.save(ubicacion);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ClienteUbicacion obtenerUbicacion(Long clienteId) {
    return ubicacionRepository.findByClienteId(clienteId)
        .orElse(null);
  }

  @Override
  public void eliminarUbicacion(Long clienteId) {
    log.info("Eliminando ubicación del cliente ID: {}", clienteId);

    ClienteUbicacion ubicacion = ubicacionRepository.findByClienteId(clienteId)
        .orElseThrow(() -> new IllegalArgumentException(
            "El cliente no tiene ubicación registrada"
        ));

    ubicacionRepository.delete(ubicacion);
  }


  @Override
  @Transactional(readOnly = true)
  public List<ClienteExoneracion> obtenerExoneracionesVigentes(Long clienteId) {
    return exoneracionRepository.findExoneracionesVigentes(clienteId, LocalDate.now());
  }

  @Override
  public void desactivarExoneracion(Long exoneracionId) {
    log.info("Desactivando exoneración ID: {}", exoneracionId);

    ClienteExoneracion exoneracion = exoneracionRepository.findById(exoneracionId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Exoneración no encontrada: " + exoneracionId
        ));

    exoneracion.setActivo(false);
    exoneracionRepository.save(exoneracion);

    // Verificar si el cliente tiene más exoneraciones activas
    Cliente cliente = exoneracion.getCliente();
    List<ClienteExoneracion> exoneracionesActivas =
        exoneracionRepository.findByClienteIdAndActivoTrue(cliente.getId());

    if (exoneracionesActivas.isEmpty()) {
      cliente.setTieneExoneracion(false);
      clienteRepository.save(cliente);
    }
  }

  // Utilidades
  @Override
  @Transactional(readOnly = true)
  public long contarClientesPorEmpresa(Long empresaId) {
    return clienteRepository.countByEmpresaIdAndActivoTrue(empresaId);
  }

  @Override
  public void procesarExoneracionesVencidas() {
    log.info("Procesando exoneraciones vencidas");

    int actualizadas = exoneracionRepository.desactivarExoneracionesVencidas(LocalDate.now());

    if (actualizadas > 0) {
      log.info("Se desactivaron {} exoneraciones vencidas", actualizadas);

      // Actualizar flag en clientes afectados
      List<Cliente> clientesConExoneracion =
          clienteRepository.findAll().stream()
              .filter(c -> c.getTieneExoneracion() &&
                  exoneracionRepository.findByClienteIdAndActivoTrue(c.getId()).isEmpty())
              .toList();

      for (Cliente cliente : clientesConExoneracion) {
        cliente.setTieneExoneracion(false);
        clienteRepository.save(cliente);
      }
    }
  }


  @Transactional
  public ClienteExoneracion agregarExoneracion(Long clienteId, ClienteExoneracion exoneracion,
      List<String> codigosCabys) {
    log.info("Agregando exoneración a cliente ID: {} con {} códigos CABYS", clienteId,
        codigosCabys != null ? codigosCabys.size() : 0);

    Cliente cliente = clienteRepository.findById(clienteId)
        .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + clienteId));

    // Validar que no exista otra exoneración activa con el mismo número
    if (exoneracionRepository.existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
        exoneracion.getNumeroDocumento(), clienteId)) {
      throw new IllegalArgumentException(
          "Ya existe otra exoneración activa con el número de documento: " +
              exoneracion.getNumeroDocumento()
      );
    }

    // Asignar cliente y guardar exoneración
    exoneracion.setCliente(cliente);
    exoneracion.setActivo(true);
    ClienteExoneracion exoneracionGuardada = exoneracionRepository.save(exoneracion);

    // Procesar códigos CABYS si los hay
    if (Boolean.TRUE.equals(exoneracion.getPoseeCabys()) && codigosCabys != null
        && !codigosCabys.isEmpty()) {
      for (String codigoCabys : codigosCabys) {
        // Buscar o crear el código CABYS
        CodigoCAByS cabys = codigoCabysRepository.findByCodigo(codigoCabys)
            .orElseGet(() -> {
              // Crear nuevo si no existe
              CodigoCAByS nuevo = new CodigoCAByS();
              nuevo.setCodigo(codigoCabys);
              nuevo.setDescripcion("Código importado desde exoneración");
              nuevo.setActivo(true);
              return codigoCabysRepository.save(nuevo);
            });

        // Crear la relación
        ClienteExoneracionCabys relacion = ClienteExoneracionCabys.builder()
            .exoneracion(exoneracionGuardada)
            .cabys(cabys)
            .build();

        exoneracionCabysRepository.save(relacion);
      }
    }

    // Actualizar flag en cliente
    cliente.setTieneExoneracion(true);
    clienteRepository.save(cliente);

    log.info("Exoneración agregada exitosamente con ID: {}", exoneracionGuardada.getId());
    return exoneracionGuardada;
  }

  @Transactional
  public ClienteExoneracion actualizarExoneracion(Long exoneracionId,
      ClienteExoneracion exoneracionActualizada, List<String> codigosCabys) {
    log.info("Actualizando exoneración ID: {} con {} códigos CABYS", exoneracionId,
        codigosCabys != null ? codigosCabys.size() : 0);

    ClienteExoneracion exoneracionExistente = exoneracionRepository.findById(exoneracionId)
        .orElseThrow(
            () -> new IllegalArgumentException("Exoneración no encontrada: " + exoneracionId));

    // Verificar número documento si cambió
    if (!exoneracionExistente.getNumeroDocumento()
        .equals(exoneracionActualizada.getNumeroDocumento())) {
      if (exoneracionRepository.existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
          exoneracionActualizada.getNumeroDocumento(),
          exoneracionExistente.getCliente().getId())) {
        throw new IllegalArgumentException(
            "Ya existe otra exoneración activa con ese número de documento"
        );
      }
    }

    // Actualizar campos básicos
    exoneracionExistente.setTipoDocumento(exoneracionActualizada.getTipoDocumento());
    exoneracionExistente.setNumeroDocumento(exoneracionActualizada.getNumeroDocumento());
    exoneracionExistente.setNombreInstitucion(exoneracionActualizada.getNombreInstitucion());
    exoneracionExistente.setFechaEmision(exoneracionActualizada.getFechaEmision());
    exoneracionExistente.setFechaVencimiento(exoneracionActualizada.getFechaVencimiento());
    exoneracionExistente.setPorcentajeExoneracion(
        exoneracionActualizada.getPorcentajeExoneracion());
    exoneracionExistente.setCategoriaCompra(exoneracionActualizada.getCategoriaCompra());
    exoneracionExistente.setMontoMaximo(exoneracionActualizada.getMontoMaximo());
    exoneracionExistente.setObservaciones(exoneracionActualizada.getObservaciones());
    exoneracionExistente.setCodigoAutorizacion(exoneracionActualizada.getCodigoAutorizacion());
    exoneracionExistente.setNumeroAutorizacion(exoneracionActualizada.getNumeroAutorizacion());
    exoneracionExistente.setPoseeCabys(exoneracionActualizada.getPoseeCabys());

    // Actualizar códigos CABYS (reemplazar lista completa)
    if (Boolean.TRUE.equals(exoneracionActualizada.getPoseeCabys())) {
      // Limpiar relaciones existentes usando el Set
      exoneracionExistente.getCabysAutorizados().clear();
      exoneracionRepository.saveAndFlush(exoneracionExistente);

      // Agregar nuevos códigos
      if (codigosCabys != null && !codigosCabys.isEmpty()) {
        for (String codigoCabys : codigosCabys) {
          CodigoCAByS cabys = codigoCabysRepository.findByCodigo(codigoCabys)
              .orElseGet(() -> {
                CodigoCAByS nuevo = new CodigoCAByS();
                nuevo.setCodigo(codigoCabys);
                nuevo.setDescripcion("Código importado desde exoneración");
                nuevo.setActivo(true);
                return codigoCabysRepository.save(nuevo);
              });

          ClienteExoneracionCabys relacion = ClienteExoneracionCabys.builder()
              .exoneracion(exoneracionExistente)
              .cabys(cabys)
              .build();

          exoneracionExistente.getCabysAutorizados().add(relacion);
        }
      }
    } else {
      // Si no posee CABYS, limpiar cualquier relación existente
      exoneracionExistente.getCabysAutorizados().clear();
    }

    return exoneracionRepository.save(exoneracionExistente);
  }

  @Override
  @Transactional(readOnly = true)
  public ClienteExoneracion obtenerExoneracionPorId(Long exoneracionId) {
    return exoneracionRepository.findById(exoneracionId)
        .orElseThrow(
            () -> new IllegalArgumentException("Exoneración no encontrada: " + exoneracionId));
  }

  private void validarMinimo(ClientePOSDto dto) {
    if (dto == null) {
      throw new IllegalArgumentException("DTO requerido");
    }
    if (dto.getTipoIdentificacion() == null) {
      throw new IllegalArgumentException("tipoIdentificacion requerido");
    }
    if (StringUtils.isBlank(dto.getNumeroIdentificacion())) {
      throw new IllegalArgumentException("numeroIdentificacion requerido");
    }
    if (StringUtils.isBlank(dto.getRazonSocial())) {
      throw new IllegalArgumentException("razonSocial requerido");
    }
  }

  private void validarExoneracionDto(ExoneracionClienteDto exo) {
    if (StringUtils.isBlank(exo.getCodigoAutorizacion())) {
      throw new IllegalArgumentException("codigoAutorizacion de exoneración es requerido");
    }

    if (exo.getPorcentajeExoneracion() == null) {
      throw new IllegalArgumentException("porcentajeExoneracion es requerido");
    }

    BigDecimal pct = parsePorcentaje(exo.getPorcentajeExoneracion());
    if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalArgumentException("porcentajeExoneracion debe estar entre 0 y 100");
    }

    LocalDate emision = parseFecha(exo.getFechaEmision().toString(), "fechaEmision");
    LocalDate venc = parseFechaNullable(exo.getFechaVencimiento().toString());
    if (venc != null && venc.isBefore(emision)) {
      throw new IllegalArgumentException("fechaVencimiento no puede ser anterior a fechaEmision");
    }

    if (Boolean.TRUE.equals(exo.getPoseeCabys()) && (exo.getCodigosCabys() == null
        || exo.getCodigosCabys().isEmpty())) {
      throw new IllegalArgumentException("poseeCabys=true requiere lista codigosCabys no vacía");
    }
  }

  private String normalizarIdentificacion(String id) {
    return StringUtils.trimToEmpty(id).replaceAll("-", "");
  }

  private TipoDocumentoExoneracion mapTipoDocumento(String codigoMh) {
    if (StringUtils.isBlank(codigoMh)) {
      // fallback a un tipo por defecto si tu enum lo contempla
      return TipoDocumentoExoneracion.COMPRAS_AUTORIZADAS_DGT; // ajusta si tu enum difiere
    }
    // Si tu enum tiene códigos MH distintos al nombre, implementa aquí el mapping.
    // Por ahora: intenta por nombre, si no, por un método 'porCodigo' en el enum si lo tienes.
    try {
      return TipoDocumentoExoneracion.valueOf(codigoMh); // ej: "EXONERACION"
    } catch (IllegalArgumentException ex) {
      // Si viene "04", "05", etc., implementa un porCodigo(...) en el enum y úsalo aquí:
      try {
        return TipoDocumentoExoneracion.fromCodigo(codigoMh);
      } catch (Exception ignored) {
        return TipoDocumentoExoneracion.EXENCIONES_DGH_AUT_LOCAL_GENERICA;
      }
    }
  }

  private LocalDate parseFecha(String isoDate, String fieldName) {
    try {
      return LocalDate.parse(isoDate);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Fecha inválida en " + fieldName + " (esperado ISO yyyy-MM-dd): " + isoDate);
    }
  }

  private LocalDate parseFechaNullable(String isoDate) {
    if (StringUtils.isBlank(isoDate)) {
      return null;
    }
    return parseFecha(isoDate, "fechaVencimiento");
  }

  private BigDecimal parsePorcentaje(Number n) {
    if (n == null) {
      return BigDecimal.ZERO;
    }
    return (n instanceof BigDecimal) ? (BigDecimal) n : new BigDecimal(n.toString());
  }

  private void vincularCabys(ClienteExoneracion exo, String codigoCabys) {
    CodigoCAByS cabys = codigoCabysRepository.findByCodigo(codigoCabys)
        .orElseThrow(() -> new IllegalArgumentException("CAByS no encontrado: " + codigoCabys));

    // Evitar duplicados por unique constraint (exoneracion_id, cabys_id)
    boolean yaExiste = exoneracionCabysRepository.existsByExoneracionIdAndCabys_Codigo(exo.getId(),
        codigoCabys);
    if (yaExiste) {
      return;
    }

    ClienteExoneracionCabys link = new ClienteExoneracionCabys();
    link.setExoneracion(exo);
    link.setCabys(cabys);

    exoneracionCabysRepository.save(link);
  }

  @Override
  @Transactional
  public Cliente agregarEmail(Long clienteId, String email) {
    Cliente cliente = obtenerPorId(clienteId);

    if (cliente == null) {
      throw new IllegalArgumentException("Cliente no encontrado");
    }

    // Validar formato de email
    if (!EMAIL_PATTERN.matcher(email).matches()) {
      throw new IllegalArgumentException("Formato de email inválido");
    }

    // Normalizar email
    email = email.toLowerCase().trim();

    // Verificar si ya existe para este cliente
    String finalEmail = email;
    boolean yaExiste = cliente.getClienteEmails().stream()
        .anyMatch(ce -> ce.getEmail().equalsIgnoreCase(finalEmail));

    if (yaExiste) {
      throw new IllegalArgumentException("Este email ya está registrado para este cliente");
    }

    // Crear nuevo ClienteEmail
    ClienteEmail clienteEmail = ClienteEmail.builder()
        .cliente(cliente)
        .email(email)
        .build();

    cliente.getClienteEmails().add(clienteEmail);

    return clienteRepository.save(cliente);
  }

  @Override
  @Transactional
  public Cliente quitarEmail(Long clienteId, String email) {
    Cliente cliente = obtenerPorId(clienteId);

    if (cliente == null) {
      throw new IllegalArgumentException("Cliente no encontrado");
    }

    // No permitir quitar si solo tiene un email
    if (cliente.getClienteEmails().size() <= 1) {
      throw new IllegalArgumentException("El cliente debe tener al menos un email");
    }

    email = email.toLowerCase().trim();

    String finalEmail = email;
    boolean removido = cliente.getClienteEmails().removeIf(
        ce -> ce.getEmail().equalsIgnoreCase(finalEmail)
    );

    if (!removido) {
      throw new IllegalArgumentException("Email no encontrado");
    }

    return clienteRepository.save(cliente);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> obtenerEmails(Long clienteId) {
    Cliente cliente = obtenerPorId(clienteId);

    if (cliente == null) {
      throw new IllegalArgumentException("Cliente no encontrado");
    }

    return cliente.getClienteEmails().stream()
        .sorted((e1, e2) -> {
          // Ordenar por frecuencia de uso
          int compareVeces = e2.getVecesUsado().compareTo(e1.getVecesUsado());
          if (compareVeces != 0) {
            return compareVeces;
          }

          // Si tienen mismo uso, ordenar por más reciente
          if (e1.getUltimoUso() != null && e2.getUltimoUso() != null) {
            return e2.getUltimoUso().compareTo(e1.getUltimoUso());
          } else if (e1.getUltimoUso() != null) {
            return -1;
          } else if (e2.getUltimoUso() != null) {
            return 1;
          }

          // Por último, orden alfabético
          return e1.getEmail().compareTo(e2.getEmail());
        })
        .map(ClienteEmail::getEmail)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public String obtenerEmailSugerido(Long clienteId) {
    List<String> emails = obtenerEmails(clienteId);

    if (emails.isEmpty()) {
      return null;
    }

    return emails.get(0); // El primero es el más usado/reciente
  }


  /**
   * Validar si cliente puede comprar a crédito
   */
  @Override
  public boolean puedeComprarACredito(Long clienteId, BigDecimal montoNuevaVenta) {
    Cliente cliente = obtenerPorId(clienteId);

    // No permite crédito
    if (!Boolean.TRUE.equals(cliente.getPermiteCredito())) {
      return false;
    }

    // Está bloqueado por mora
    if (Boolean.TRUE.equals(cliente.getBloqueadoPorMora())) {
      return false;
    }

    // Verificar límite de crédito (0 = sin límite)
    if (cliente.getLimiteCredito() != null &&
        cliente.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {

      BigDecimal saldoActual = cliente.getSaldoActual() != null ?
          cliente.getSaldoActual() : BigDecimal.ZERO;

      BigDecimal saldoProyectado = saldoActual.add(montoNuevaVenta);

      if (saldoProyectado.compareTo(cliente.getLimiteCredito()) > 0) {
        log.warn("Cliente {} excede límite de crédito. Límite: {}, Proyectado: {}",
            clienteId, cliente.getLimiteCredito(), saldoProyectado);
        return false;
      }
    }

    return true;
  }

  @Override
  @Transactional
  public void desbloquearCredito(Long clienteId, String motivo) {
    Cliente cliente = obtenerPorId(clienteId);

    if (!Boolean.TRUE.equals(cliente.getBloqueadoPorMora())) {
      throw new BadRequestException("Cliente no está bloqueado por mora");
    }

    cliente.setBloqueadoPorMora(false);
    cliente.setEstadoCredito("ACTIVO");

    clienteRepository.save(cliente);

    log.warn("Cliente {} desbloqueado manualmente. Motivo: {}",
        cliente.getRazonSocial(), motivo);
  }

  @Override
  @Transactional
  public List<ActividadEconomicaDto> actualizarActividadesDesdeHacienda(Long clienteId) {
    log.info("🔄 Actualizando actividades desde Hacienda para cliente: {}", clienteId);

    Cliente cliente = clienteRepository.findById(clienteId)
        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

    if (!cliente.getInscritoHacienda()) {
      throw new RuntimeException("Cliente no está inscrito en Hacienda");
    }

    String identificacion = cliente.getNumeroIdentificacion();
    String url = "https://api.hacienda.go.cr/fe/ae?identificacion=" + identificacion;

    try {
      log.info("📡 Consultando Hacienda: {}", url);

      Map<String, Object> response = restTemplate.getForObject(url, Map.class);

      if (response == null || !response.containsKey("actividades")) {
        throw new RuntimeException("No se obtuvieron actividades desde Hacienda");
      }

      // ELIMINAR anteriores
      clienteActividadRepository.deleteByClienteId(clienteId);

      // Limpiar colección del cliente
      if (cliente.getActividades() != null) {
        cliente.getActividades().clear();
      } else {
        cliente.setActividades(new HashSet<>());
      }

      log.info("🗑️ Actividades anteriores eliminadas");

      // Procesar nuevas
      List<Map<String, Object>> actividadesHacienda =
          (List<Map<String, Object>>) response.get("actividades");

      log.info("📥 Procesando {} actividades", actividadesHacienda.size());

      for (Map<String, Object> actData : actividadesHacienda) {
        String codigoCiiu4 = (String) actData.get("codigo");
        String descripcion = (String) actData.get("descripcion");
        String tipo = (String) actData.get("tipo");
        String estado = (String) actData.get("estado");

        // Extraer CIIU3 con manejo seguro
        String codigoCiiu3 = null;
        if (actData.containsKey("ciiu3") && actData.get("ciiu3") != null) {
          Object ciiu3Obj = actData.get("ciiu3");

          if (ciiu3Obj instanceof Map) {
            Map<String, Object> ciiu3Data = (Map<String, Object>) ciiu3Obj;
            codigoCiiu3 = (String) ciiu3Data.get("codigo");
          }
        }

        // Solo activas
        if (!"A".equals(estado)) {
          log.debug("⏭️ Saltando actividad inactiva: {}", codigoCiiu4);
          continue;
        }

        // ✅ CREAR con Builder
        ClienteActividad ca = ClienteActividad.builder()
            .codigoCiiu4(codigoCiiu4)
            .codigoCiiu3(codigoCiiu3)
            .codigoActividad(codigoCiiu3 != null ? codigoCiiu3 : codigoCiiu4)
            .descripcion(descripcion != null ? descripcion.trim() : null)
            .tipo(tipo)
            .estado(estado)
            .build();

        // ✅ IMPORTANTE: Agregar al Set del cliente (esto hace que JPA maneje el cliente_id)
        cliente.getActividades().add(ca);
        ca.setCliente(cliente); // Relación bidireccional

        log.debug("✅ Agregada: {} - {} - {}", tipo, codigoCiiu4, descripcion);
      }

      // Guardar cliente (cascade guardará las actividades)
      clienteRepository.save(cliente);

      log.info("✅ Actividades actualizadas correctamente");

      // Retornar lista actualizada
      return listarActividadesCliente(clienteId);

    } catch (Exception e) {
      log.error("❌ Error consultando Hacienda: {}", e.getMessage(), e);
      throw new RuntimeException("Error al consultar Hacienda: " + e.getMessage());
    }
  }

  // Helper privado
  private List<ActividadEconomicaDto> listarActividadesCliente(Long clienteId) {
    List<ClienteActividad> actividades = clienteActividadRepository.findByClienteId(clienteId);

    return actividades.stream()
        .map(ca -> {
          ActividadEconomicaDto dto = new ActividadEconomicaDto();
          dto.setCodigo(ca.getTipo()); // "P" o "S"
          dto.setCodigo(ca.getCodigoCiiu4()); // Usar CIIU4 nuevo
          dto.setDescripcion(ca.getDescripcion());
          return dto;
        })
        .collect(Collectors.toList());
  }
}