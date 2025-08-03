package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.request.UsuarioCreateRequest;
import com.snnsoluciones.backnathbitpos.dto.request.UsuarioUpdateRequest;
import com.snnsoluciones.backnathbitpos.dto.response.UsuarioResponse;
import com.snnsoluciones.backnathbitpos.entity.operacion.Caja;
import com.snnsoluciones.backnathbitpos.entity.security.Usuario;
import com.snnsoluciones.backnathbitpos.entity.tenant.Sucursal;
import org.mapstruct.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidad Usuario y sus DTOs
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UsuarioMapper {

  /**
   * Convierte una entidad Usuario a UsuarioResponse
   */
  @Mapping(target = "rolId", source = "rol.id")
  @Mapping(target = "rolNombre", source = "rol.nombre")
  @Mapping(target = "sucursalPredeterminadaId", source = "sucursalPredeterminada.id")
  @Mapping(target = "sucursalPredeterminadaNombre", source = "sucursalPredeterminada.nombre")
  @Mapping(target = "sucursalesIds", source = "sucursales", qualifiedByName = "sucursalesToIds")
  @Mapping(target = "cajasIds", source = "cajas", qualifiedByName = "cajasToIds")
  @Mapping(target = "rolId", source = "rol.id")
  @Mapping(target = "rolNombre", source = "rol.nombre")
  UsuarioResponse toResponse(Usuario usuario);

  /**
   * Convierte UsuarioCreateRequest a entidad Usuario
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "rol", ignore = true)
  @Mapping(target = "sucursalPredeterminada", ignore = true)
  @Mapping(target = "sucursales", ignore = true)
  @Mapping(target = "cajas", ignore = true)
  @Mapping(target = "ultimoAcceso", ignore = true)
  @Mapping(target = "intentosFallidos", constant = "0")
  @Mapping(target = "bloqueado", constant = "false")
  @Mapping(target = "activo", constant = "true")
  @Mapping(target = "authorities", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "accountNonExpired", ignore = true)
  @Mapping(target = "accountNonLocked", ignore = true)
  @Mapping(target = "credentialsNonExpired", ignore = true)
  @Mapping(target = "enabled", ignore = true)
  Usuario toEntity(UsuarioCreateRequest request);

  /**
   * Actualiza una entidad Usuario con los datos de UsuarioUpdateRequest
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "rol", ignore = true)
  @Mapping(target = "sucursalPredeterminada", ignore = true)
  @Mapping(target = "sucursales", ignore = true)
  @Mapping(target = "cajas", ignore = true)
  @Mapping(target = "ultimoAcceso", ignore = true)
  @Mapping(target = "intentosFallidos", ignore = true)
  @Mapping(target = "authorities", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "accountNonExpired", ignore = true)
  @Mapping(target = "accountNonLocked", ignore = true)
  @Mapping(target = "credentialsNonExpired", ignore = true)
  @Mapping(target = "enabled", ignore = true)
  void updateEntity(@MappingTarget Usuario usuario, UsuarioUpdateRequest request);

  /**
   * Métodos auxiliares para mapeo de colecciones
   */
  @Named("sucursalesToIds")
  default Set<UUID> sucursalesToIds(Set<Sucursal> sucursales) {
    if (sucursales == null) {
      return null;
    }
    return sucursales.stream()
        .map(Sucursal::getId)
        .collect(Collectors.toSet());
  }

  @Named("cajasToIds")
  default Set<UUID> cajasToIds(Set<Caja> cajas) {
    if (cajas == null) {
      return null;
    }
    return cajas.stream()
        .map(Caja::getId)
        .collect(Collectors.toSet());
  }
}