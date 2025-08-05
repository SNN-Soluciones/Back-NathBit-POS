package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioEmpresa;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioSucursal;
import com.snnsoluciones.backnathbitpos.entity.global.EmpresaSucursal;
import com.snnsoluciones.backnathbitpos.enums.RolNombre;
import org.mapstruct.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidad UsuarioGlobal y sus DTOs
 * Adaptado para el nuevo modelo multi-empresa
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UsuarioMapper {

  /**
   * Convierte una entidad UsuarioGlobal a UsuarioResponse
   * Nota: Este método necesita contexto de empresa/sucursal para ser completo
   */
  @Mapping(target = "rolId", ignore = true) // Se debe obtener del contexto
  @Mapping(target = "rolNombre", ignore = true) // Se debe obtener del contexto
  @Mapping(target = "sucursalPredeterminadaId", ignore = true) // Se debe obtener del contexto
  @Mapping(target = "sucursalPredeterminadaNombre", ignore = true) // Se debe obtener del contexto
  @Mapping(target = "sucursalesIds", ignore = true) // Se debe calcular según empresa
  @Mapping(target = "cajasIds", ignore = true) // Ahora está en el schema del tenant
  @Mapping(target = "activo", source = "activo")
  @Mapping(target = "bloqueado", source = "bloqueado")
  @Mapping(target = "email", source = "email")
  @Mapping(target = "nombre", source = "nombre")
  @Mapping(target = "apellidos", source = "apellidos")
  @Mapping(target = "telefono", source = "telefono")
  @Mapping(target = "identificacion", source = "identificacion")
  @Mapping(target = "tipoIdentificacion", source = "tipoIdentificacion")
  @Mapping(target = "ultimoAcceso", source = "ultimoAcceso")
  UsuarioResponse toResponse(UsuarioGlobal usuario);

  /**
   * Convierte UsuarioGlobal a UsuarioResponse con contexto de empresa
   * Este es el método preferido cuando se conoce la empresa
   */
  default UsuarioResponse toResponseWithContext(UsuarioGlobal usuario, UUID empresaId) {
    UsuarioResponse response = toResponse(usuario);

    // Buscar el acceso del usuario a la empresa específica
    UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
        .filter(ue -> ue.getEmpresa().getId().equals(empresaId) && ue.getActivo())
        .findFirst()
        .orElse(null);

    if (usuarioEmpresa != null) {
      // Establecer rol de la empresa
      response.setRolNombre(usuarioEmpresa.getRol());

      // Obtener sucursales disponibles para esta empresa
      Set<UUID> sucursalesIds = usuarioEmpresa.getUsuarioSucursales().stream()
          .filter(UsuarioSucursal::getActivo)
          .map(us -> us.getSucursal().getId())
          .collect(Collectors.toSet());
      response.setSucursalesIds(sucursalesIds);

      // Buscar sucursal principal
      UsuarioSucursal sucursalPrincipal = usuarioEmpresa.getUsuarioSucursales().stream()
          .filter(us -> us.getActivo() && us.getEsPrincipal())
          .findFirst()
          .orElse(null);

      if (sucursalPrincipal != null) {
        response.setSucursalPredeterminadaId(sucursalPrincipal.getSucursal().getId());
        response.setSucursalPredeterminadaNombre(sucursalPrincipal.getSucursal().getNombreSucursal());
      }
    }

    return response;
  }

  /**
   * Convierte UsuarioCreateRequest a entidad UsuarioGlobal
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "activo", constant = "true")
  @Mapping(target = "bloqueado", constant = "false")
  @Mapping(target = "intentosFallidos", constant = "0")
  @Mapping(target = "ultimoAcceso", ignore = true)
  @Mapping(target = "fechaPasswordExpira", ignore = true)
  @Mapping(target = "debeCambiarPassword", constant = "false")
  @Mapping(target = "usuarioEmpresas", ignore = true)
  @Mapping(target = "password", ignore = true) // Se debe encriptar antes de guardar
  UsuarioGlobal toEntity(UsuarioCreateRequest request);

  /**
   * Actualiza una entidad UsuarioGlobal con los datos de UsuarioUpdateRequest
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "intentosFallidos", ignore = true)
  @Mapping(target = "ultimoAcceso", ignore = true)
  @Mapping(target = "fechaPasswordExpira", ignore = true)
  @Mapping(target = "debeCambiarPassword", ignore = true)
  @Mapping(target = "usuarioEmpresas", ignore = true)
  void updateEntity(@MappingTarget UsuarioGlobal usuario, UsuarioUpdateRequest request);

  /**
   * Métodos auxiliares para mapeo
   */

  /**
   * Obtiene el rol del usuario para una empresa específica
   */
  default RolNombre getRolForEmpresa(UsuarioGlobal usuario, UUID empresaId) {
    return usuario.getUsuarioEmpresas().stream()
        .filter(ue -> ue.getEmpresa().getId().equals(empresaId) && ue.getActivo())
        .map(UsuarioEmpresa::getRol)
        .findFirst()
        .orElse(null);
  }

  /**
   * Obtiene las sucursales a las que tiene acceso el usuario en una empresa
   */
  default Set<EmpresaSucursal> getSucursalesForEmpresa(UsuarioGlobal usuario, UUID empresaId) {
    UsuarioEmpresa usuarioEmpresa = usuario.getUsuarioEmpresas().stream()
        .filter(ue -> ue.getEmpresa().getId().equals(empresaId) && ue.getActivo())
        .findFirst()
        .orElse(null);

    if (usuarioEmpresa == null) {
      return Set.of();
    }

    return usuarioEmpresa.getUsuarioSucursales().stream()
        .filter(UsuarioSucursal::getActivo)
        .map(UsuarioSucursal::getSucursal)
        .collect(Collectors.toSet());
  }

  /**
   * Verifica si el usuario tiene acceso a una empresa
   */
  default boolean hasAccessToEmpresa(UsuarioGlobal usuario, UUID empresaId) {
    return usuario.getUsuarioEmpresas().stream()
        .anyMatch(ue -> ue.getEmpresa().getId().equals(empresaId)
            && ue.getActivo()
            && ue.estaVigente());
  }
}