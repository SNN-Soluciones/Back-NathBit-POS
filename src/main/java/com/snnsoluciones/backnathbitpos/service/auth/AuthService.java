package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.dto.auth.ContextoDTO;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginResponse;
import com.snnsoluciones.backnathbitpos.dto.auth.RefreshTokenRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.SeleccionContextoRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.TokenResponse;

/**
 * Servicio para gestión de autenticación y autorización.
 * Maneja el login, selección de contexto empresa/sucursal, y tokens JWT.
 */
public interface AuthService {
    
    /**
     * Realiza el login de un usuario.
     * Determina si el usuario tiene acceso operativo directo o necesita seleccionar contexto.
     * 
     * @param loginRequest credenciales del usuario
     * @return LoginResponse con token, información del usuario y accesos disponibles
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * Selecciona el contexto de trabajo (empresa/sucursal) para usuarios administrativos.
     * Genera un nuevo JWT con el contexto seleccionado.
     * 
     * @param request contiene empresaId y opcionalmente sucursalId
     * @return TokenResponse con nuevo JWT contextualizado
     */
    TokenResponse seleccionarContexto(SeleccionContextoRequest request);
    
    /**
     * Renueva el token de acceso usando el refresh token.
     * 
     * @param refreshTokenRequest contiene el refresh token
     * @return TokenResponse con nuevos tokens
     */
    TokenResponse refresh(RefreshTokenRequest refreshTokenRequest);
    
    /**
     * Cierra la sesión del usuario invalidando sus tokens.
     * 
     * @param token JWT del usuario actual
     */
    void logout(String token);
    
    /**
     * Valida si un token es válido y no ha sido revocado.
     * 
     * @param token JWT a validar
     * @return true si el token es válido
     */
    boolean validarToken(String token);
    
    /**
     * Obtiene el ID del usuario desde el token.
     * 
     * @param token JWT del usuario
     * @return ID del usuario
     */
    Long obtenerUsuarioIdDesdeToken(String token);
    
    /**
     * Obtiene el contexto actual (empresa/sucursal) desde el token.
     * 
     * @param token JWT del usuario
     * @return ContextoDTO con empresa y sucursal actual
     */
    ContextoDTO obtenerContextoDesdeToken(String token);
}