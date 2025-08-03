// Archivo: src/main/java/com/snnsoluciones/backnathbitpos/service/operacion/OrdenService.java

package com.snnsoluciones.backnathbitpos.service.operacion;

import com.snnsoluciones.backnathbitpos.dto.request.OrdenRequest;
import com.snnsoluciones.backnathbitpos.dto.response.OrdenResponse;
import com.snnsoluciones.backnathbitpos.entity.operacion.OrdenDetalle;
import com.snnsoluciones.backnathbitpos.enums.EstadoOrden;
import com.snnsoluciones.backnathbitpos.enums.TipoOrden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrdenService {

  // CRUD básico
  OrdenResponse crear(OrdenRequest request);
  OrdenResponse actualizar(UUID id, OrdenRequest request);
  OrdenResponse obtenerPorId(UUID id);
  void eliminar(UUID id);
  Page<OrdenResponse> listar(Pageable pageable);

  // Búsquedas específicas
  OrdenResponse obtenerPorNumero(String numeroOrden);
  List<OrdenResponse> obtenerPorMesa(UUID mesaId);
  List<OrdenResponse> obtenerPorEstado(EstadoOrden estado);
  List<OrdenResponse> obtenerPorMesero(UUID meseroId);

  // Gestión de detalles
  OrdenDetalle agregarDetalle(UUID ordenId, OrdenDetalle detalle);
  OrdenDetalle actualizarDetalle(UUID ordenId, Integer numeroLinea, OrdenDetalle detalle);
  void eliminarDetalle(UUID ordenId, Integer numeroLinea);

  // Operaciones de negocio
  void cambiarEstado(UUID ordenId, EstadoOrden nuevoEstado);
  void asignarMesero(UUID ordenId, UUID meseroId);
  void cambiarMesa(UUID ordenId, UUID nuevaMesaId);
  void aplicarDescuentoGlobal(UUID ordenId, BigDecimal porcentajeDescuento);

  // Operaciones para cocina
  void marcarEnPreparacion(UUID ordenId);
  void marcarLista(UUID ordenId);
  void marcarServida(UUID ordenId);

  // Pagos
  void marcarPagada(UUID ordenId, UUID cajaId);
  void cancelarOrden(UUID ordenId, String motivo);

  // Búsqueda avanzada
  Page<OrdenResponse> buscar(String numeroOrden,
      EstadoOrden estado,
      TipoOrden tipo,
      UUID meseroId,
      LocalDateTime fechaInicio,
      LocalDateTime fechaFin,
      Pageable pageable);
}