package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.security.ContextoUsuario;
import com.snnsoluciones.backnathbitpos.service.impl.SecurityContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Controller base con métodos útiles para todos los controllers
 */
public abstract class BaseController {
    
    @Autowired
    protected SecurityContextService securityContextService;
    
    /**
     * Obtiene el contexto del usuario actual
     */
    protected ContextoUsuario getContextoActual() {
        return securityContextService.getContextoActual();
    }
    
    /**
     * Obtiene el ID del usuario actual
     */
    protected Long getCurrentUserId() {
        return securityContextService.getCurrentUserId();
    }
    
    /**
     * Obtiene el ID de la empresa actual
     */
    protected Long getCurrentEmpresaId() {
        return securityContextService.getCurrentEmpresaId();
    }
    
    /**
     * Obtiene el ID de la sucursal actual
     */
    protected Long getCurrentSucursalId() {
        return securityContextService.getCurrentSucursalId();
    }
    
    /**
     * Obtiene el rol del usuario actual
     */
    protected String getCurrentUserRole() {
        return securityContextService.getCurrentUserRole();
    }
    
    /**
     * Verifica si el usuario es ROOT o SOPORTE
     */
    protected boolean isRolSistema() {
        return securityContextService.isRolSistema();
    }
    
    /**
     * Crea una respuesta exitosa
     */
    protected <T> ResponseEntity<ApiResponse<T>> successResponse(T data) {
        return ResponseEntity.ok(ApiResponse.success("Operación exitosa", data));
    }
    
    /**
     * Crea una respuesta exitosa con mensaje personalizado
     */
    protected <T> ResponseEntity<ApiResponse<T>> successResponse(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
    
    /**
     * Crea una respuesta de error
     */
    protected <T> ResponseEntity<ApiResponse<T>> errorResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
    
    /**
     * Crea una respuesta de error 400 Bad Request
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return errorResponse(message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Crea una respuesta de error 404 Not Found
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return errorResponse(message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Crea una respuesta de error 403 Forbidden
     */
    protected <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return errorResponse(message, HttpStatus.FORBIDDEN);
    }
}