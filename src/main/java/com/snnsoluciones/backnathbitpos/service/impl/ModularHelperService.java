package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service helper para manejar la lógica de entidades globales vs locales por sucursal
 * Centraliza la lógica de determinación de alcance según configuración de empresa
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModularHelperService {

    private final SecurityContextService securityContextService;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Determina si una entidad debe ser asignada a una sucursal específica
     * basándose en la configuración de la empresa
     * 
     * @param empresaId ID de la empresa
     * @param tipoEntidad Tipo de entidad: "producto", "categoria", "cliente", "proveedor"
     * @return Sucursal a asignar o null si es global
     */
    @Transactional(readOnly = true)
    public Sucursal determinarSucursalParaEntidad(Long empresaId, String tipoEntidad) {
        // Obtener configuración de empresa
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada: " + empresaId));

        // Verificar si la entidad debe ser por sucursal
        boolean esPorSucursal = verificarConfiguracionPorSucursal(empresa, tipoEntidad);

        if (!esPorSucursal) {
            // Entidad global - no asignar sucursal
            return null;
        }

        // Entidad por sucursal - obtener sucursal del contexto
        Long sucursalId = securityContextService.getCurrentSucursalId();
        if (sucursalId == null) {
            throw new BusinessException(
                "Se requiere contexto de sucursal para crear " + tipoEntidad + " en esta empresa"
            );
        }

        // Validar que la sucursal pertenezca a la empresa
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new BusinessException("Sucursal no encontrada: " + sucursalId));

        if (!sucursal.getEmpresa().getId().equals(empresaId)) {
            throw new BusinessException("La sucursal no pertenece a la empresa especificada");
        }

        return sucursal;
    }

    /**
     * Verifica si una entidad debe manejarse por sucursal según configuración
     * 
     * @param empresa Empresa a verificar
     * @param tipoEntidad Tipo de entidad
     * @return true si es por sucursal, false si es global
     */
    public boolean verificarConfiguracionPorSucursal(Empresa empresa, String tipoEntidad) {
        switch (tipoEntidad.toLowerCase()) {
            case "producto":
                return Boolean.TRUE.equals(empresa.getProductosPorSucursal());
            case "categoria":
                return Boolean.TRUE.equals(empresa.getCategoriasPorSucursal());
            case "cliente":
                return Boolean.TRUE.equals(empresa.getClientesPorSucursal());
            case "proveedor":
                return Boolean.TRUE.equals(empresa.getProveedoresPorSucursal());
            default:
                throw new BusinessException("Tipo de entidad no válido: " + tipoEntidad);
        }
    }

    /**
     * Determina los parámetros de búsqueda según configuración global/local
     * 
     * @param empresaId ID de la empresa
     * @param tipoEntidad Tipo de entidad
     * @return QueryParams con empresaId y sucursalId (puede ser null)
     */
    @Transactional(readOnly = true)
    public QueryParams construirParametrosBusqueda(Long empresaId, String tipoEntidad) {
        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new BusinessException("Empresa no encontrada: " + empresaId));

        boolean esPorSucursal = verificarConfiguracionPorSucursal(empresa, tipoEntidad);

        if (!esPorSucursal) {
            // Búsqueda global - solo por empresa
            return new QueryParams(empresaId, null, false);
        }

        // Búsqueda por sucursal
        Long sucursalId = securityContextService.getCurrentSucursalId();
        if (sucursalId == null) {
            log.warn("No hay contexto de sucursal para búsqueda de {} por sucursal", tipoEntidad);
            // Podríamos retornar lista vacía o lanzar excepción según el caso
            throw new BusinessException(
                "Se requiere contexto de sucursal para listar " + tipoEntidad + " en esta empresa"
            );
        }

        return new QueryParams(empresaId, sucursalId, true);
    }

    /**
     * Valida que no se intente cambiar el alcance de una entidad
     * (de global a local o viceversa)
     * 
     * @param sucursalActual Sucursal actual de la entidad (puede ser null)
     * @param sucursalNueva Sucursal que se intenta asignar (puede ser null)
     * @param tipoEntidad Tipo de entidad para el mensaje de error
     */
    public void validarCambioAlcance(Long sucursalActual, Long sucursalNueva, String tipoEntidad) {
        boolean esGlobalActual = (sucursalActual == null);
        boolean esGlobalNueva = (sucursalNueva == null);

        if (esGlobalActual != esGlobalNueva) {
            throw new BusinessException(
                "No se puede cambiar el alcance de " + tipoEntidad + 
                " de " + (esGlobalActual ? "global" : "local") + 
                " a " + (esGlobalNueva ? "global" : "local")
            );
        }

        // Si ambos son locales, verificar que sea la misma sucursal
        if (!esGlobalActual && !sucursalActual.equals(sucursalNueva)) {
            throw new BusinessException(
                "No se puede cambiar la sucursal de " + tipoEntidad + " local"
            );
        }
    }

    /**
     * Clase interna para retornar parámetros de búsqueda
     */
    public static class QueryParams {
        private final Long empresaId;
        private final Long sucursalId;
        private final boolean esPorSucursal;

        public QueryParams(Long empresaId, Long sucursalId, boolean esPorSucursal) {
            this.empresaId = empresaId;
            this.sucursalId = sucursalId;
            this.esPorSucursal = esPorSucursal;
        }

        public Long getEmpresaId() {
            return empresaId;
        }

        public Long getSucursalId() {
            return sucursalId;
        }

        public boolean isEsPorSucursal() {
            return esPorSucursal;
        }

        public boolean esGlobal() {
            return !esPorSucursal;
        }
    }
}