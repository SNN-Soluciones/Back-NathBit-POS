// ProductoCompuestoService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.compuesto.ActualizarConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CalcularPrecioCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.CalculoPrecioResponse;
import com.snnsoluciones.backnathbitpos.dto.compuesto.CrearConfiguracionRequest;
import com.snnsoluciones.backnathbitpos.dto.compuesto.ProductoCompuestoConfiguracionDTO;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCompuestoRequest;
import com.snnsoluciones.backnathbitpos.dto.producto.ValidacionSeleccionResponse;
import com.snnsoluciones.backnathbitpos.dto.productocompuesto.ConfiguracionFlujoDTO;
import com.snnsoluciones.backnathbitpos.dto.slots.OpcionSlotDTO;
import java.util.List;

public interface ProductoCompuestoService {

    // ========== OPERACIONES CRUD ==========
    ProductoCompuestoDto crear(Long empresaId, Long productoId, ProductoCompuestoRequest request);

    ProductoCompuestoDto actualizar(Long empresaId, Long productoId, ProductoCompuestoRequest request);

    void eliminar(Long empresaId, Long productoId);

    // ========== CONSULTAS ==========
    ProductoCompuestoDto buscarPorProductoId(Long empresaId, Long productoId);

    List<ProductoCompuestoDto> listarPorEmpresa(Long empresaId);

    // ========== VALIDACIONES ==========
    /**
     * Calcula el precio total según las opciones seleccionadas
     */
    CalculoPrecioResponse calcularPrecio(CalcularPrecioCompuestoRequest request);

    /**
     * Valida que la selección cumpla las reglas y tenga stock
     */
    ValidacionSeleccionResponse validarSeleccion(Long productoId, Long sucursalId, List<Long> opcionesSeleccionadas);

    /**
     * Filtra compuestos por disponibilidad en sucursal
     */
    List<ProductoCompuestoDto> filtrarPorDisponibilidadSucursal(List<ProductoCompuestoDto> compuestos, Long sucursalId);

    List<OpcionSlotDTO> obtenerOpcionesSlot(Long slotId, Long sucursalId);

    // ========== CONFIGURACIONES CONDICIONALES (NUEVOS) ==========

    /**
     * Crea una nueva configuración condicional para un producto compuesto
     *
     * @param productoId ID del producto compuesto
     * @param request Datos de la configuración y sus slots
     * @return Configuración creada con sus slots
     */
    ProductoCompuestoConfiguracionDTO crearConfiguracion(Long productoId, CrearConfiguracionRequest request);

    /**
     * Actualiza una configuración existente
     *
     * @param configId ID de la configuración
     * @param request Datos a actualizar
     * @return Configuración actualizada
     */
    ProductoCompuestoConfiguracionDTO actualizarConfiguracion(Long configId, ActualizarConfiguracionRequest request);

    /**
     * Obtiene todas las configuraciones de un producto compuesto
     *
     * @param productoId ID del producto compuesto
     * @return Lista de configuraciones con sus slots
     */
    List<ProductoCompuestoConfiguracionDTO> obtenerConfiguraciones(Long productoId);

    /**
     * Elimina una configuración
     *
     * @param configId ID de la configuración a eliminar
     */
    void eliminarConfiguracion(Long configId);

    /**
     * Obtiene la configuración que se activa con una opción específica
     * Este método es CLAVE para el frontend - devuelve todo lo necesario
     * para renderizar el modal dinámico cuando el usuario elige una opción
     *
     * @param productoId ID del producto compuesto
     * @param opcionId ID de la opción seleccionada
     * @return Configuración con slots y sus opciones cargadas dinámicamente
     */
    ProductoCompuestoConfiguracionDTO obtenerConfiguracionPorOpcion(Long productoId, Long opcionId);

    /**
     * Obtiene el flujo de configuración inicial de un producto compuesto
     * Decide si mostrar pregunta inicial o configuración default
     *
     * @param productoId ID del producto compuesto
     * @param sucursalId ID de la sucursal para filtrar disponibilidad
     * @return DTO con el flujo de configuración
     */
    ConfiguracionFlujoDTO obtenerFlujoConfiguracion(Long productoId, Long sucursalId);

    /**
     * Obtiene la configuración que se activa al seleccionar una opción específica
     * Usado cuando hay pregunta inicial (ej: usuario elige "COMBO")
     *
     * @param productoId ID del producto compuesto
     * @param opcionId ID de la opción seleccionada (ej: opción "COMBO")
     * @param sucursalId ID de la sucursal para filtrar disponibilidad
     * @return Configuración completa con sus slots y opciones
     */
    ProductoCompuestoConfiguracionDTO obtenerConfiguracionPorOpcion(
        Long productoId,
        Long opcionId,
        Long sucursalId
    );
}