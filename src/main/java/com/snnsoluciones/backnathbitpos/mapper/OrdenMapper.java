package com.snnsoluciones.backnathbitpos.mapper;

import com.snnsoluciones.backnathbitpos.dto.request.OrdenRequest;
import com.snnsoluciones.backnathbitpos.dto.response.OrdenDetalleResponse;
import com.snnsoluciones.backnathbitpos.dto.response.OrdenResponse;
import com.snnsoluciones.backnathbitpos.entity.operacion.Orden;
import com.snnsoluciones.backnathbitpos.entity.operacion.OrdenDetalle;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper para convertir entre entidad Orden y sus DTOs
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrdenMapper {

  /**
   * Convierte una entidad Orden a OrdenResponse
   */
  @Mapping(target = "mesaId", source = "mesa.id")
  @Mapping(target = "mesaNumero", source = "mesa.numero")
  @Mapping(target = "mesaNombre", source = "mesa.nombre")
  @Mapping(target = "mesaZona", source = "mesa.zona.nombre")
  @Mapping(target = "clienteId", source = "cliente.id")
  @Mapping(target = "clienteNombre", expression = "java(getClienteNombre(orden))")
  @Mapping(target = "clienteIdentificacion", source = "cliente.numeroIdentificacion")
  @Mapping(target = "meseroId", source = "mesero.id")
  @Mapping(target = "meseroNombre", expression = "java(getMeseroNombre(orden))")
  @Mapping(target = "cajaId", source = "caja.id")
  @Mapping(target = "cajaNombre", source = "caja.nombre")
  @Mapping(target = "cantidadProductos", expression = "java(getCantidadProductos(orden))")
  @Mapping(target = "detalles", source = "detalles")
  OrdenResponse toResponse(Orden orden);

  /**
   * Convierte OrdenRequest a entidad Orden (para crear nueva)
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "numeroOrden", ignore = true)
  @Mapping(target = "estado", ignore = true)
  @Mapping(target = "mesa", ignore = true)
  @Mapping(target = "cliente", ignore = true)
  @Mapping(target = "mesero", ignore = true)
  @Mapping(target = "caja", ignore = true)
  @Mapping(target = "fechaOrden", ignore = true)
  @Mapping(target = "subtotal", ignore = true)
  @Mapping(target = "totalDescuentos", ignore = true)
  @Mapping(target = "totalImpuestos", ignore = true)
  @Mapping(target = "total", ignore = true)
  @Mapping(target = "detalles", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "activo", ignore = true)
  Orden toEntity(OrdenRequest request);

  /**
   * Actualiza una entidad Orden con los datos de OrdenRequest
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "numeroOrden", ignore = true)
  @Mapping(target = "tipo", ignore = true) // No se puede cambiar el tipo
  @Mapping(target = "estado", ignore = true)
  @Mapping(target = "mesa", ignore = true)
  @Mapping(target = "cliente", ignore = true)
  @Mapping(target = "mesero", ignore = true)
  @Mapping(target = "caja", ignore = true)
  @Mapping(target = "fechaOrden", ignore = true)
  @Mapping(target = "subtotal", ignore = true)
  @Mapping(target = "totalDescuentos", ignore = true)
  @Mapping(target = "totalImpuestos", ignore = true)
  @Mapping(target = "total", ignore = true)
  @Mapping(target = "detalles", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "activo", ignore = true)
  void updateEntity(@MappingTarget Orden orden, OrdenRequest request);

  /**
   * Convierte OrdenDetalle a OrdenDetalleResponse
   */
  @Mapping(target = "productoId", source = "producto.id")
  @Mapping(target = "productoCodigo", source = "producto.codigo")
  @Mapping(target = "productoNombre", source = "producto.nombre")
  @Mapping(target = "productoCategoria", source = "producto.categoria.nombre")
  @Mapping(target = "cancelado", expression = "java(detalle.getEstado() == com.snnsoluciones.backnathbitpos.enums.EstadoOrdenDetalle.CANCELADO)")
  OrdenDetalleResponse toDetalleResponse(OrdenDetalle detalle);

  /**
   * Convierte lista de OrdenDetalle a lista de OrdenDetalleResponse
   */
  List<OrdenDetalleResponse> toDetallesResponse(List<OrdenDetalle> detalles);

  /**
   * Métodos helper para expresiones
   */
  default String getClienteNombre(Orden orden) {
    if (orden.getCliente() != null) {
      return orden.getCliente().getNombre() + " " + orden.getCliente().getApellidos();
    } else if (orden.getNombreClienteDelivery() != null) {
      return orden.getNombreClienteDelivery();
    }
    return null;
  }

  default String getMeseroNombre(Orden orden) {
    if (orden.getMesero() != null) {
      return orden.getMesero().getNombre() + " " + orden.getMesero().getApellidos();
    }
    return null;
  }

  default Integer getCantidadProductos(Orden orden) {
    if (orden.getDetalles() != null) {
      return orden.getDetalles().stream()
          .filter(d -> d.getEstado() != com.snnsoluciones.backnathbitpos.enums.EstadoOrdenDetalle.CANCELADO)
          .mapToInt(d -> d.getCantidad().intValue())
          .sum();
    }
    return 0;
  }
}