package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.auth.CambiarPinRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvRequest;
import com.snnsoluciones.backnathbitpos.dto.auth.LoginPdvResponse;

/**
 * Servicio para autenticación con PIN en PDV
 */
public interface AuthPdvService {
    
    /**
     * Realiza login con PIN en un dispositivo PDV
     * 
     * @param deviceToken Token del dispositivo desde donde se hace login
     * @param request Datos del login (usuarioId, pin)
     * @return Response con JWT y datos del usuario
     */
    LoginPdvResponse loginConPin(String deviceToken, LoginPdvRequest request);
    
    /**
     * Cambia el PIN de un usuario
     * 
     * @param usuarioId ID del usuario
     * @param request Datos del cambio (pinActual, nuevoPin, confirmarPin)
     */
    void cambiarPin(Long usuarioId, CambiarPinRequest request);
    
    /**
     * Genera un PIN aleatorio de 4 dígitos y lo asigna al usuario
     * 
     * @param usuarioId ID del usuario
     * @return PIN generado (sin hashear, para mostrarlo al admin)
     */
    String generarPinAleatorio(Long usuarioId);
    
    /**
     * Resetea el PIN de un usuario (solo admin)
     * 
     * @param usuarioId ID del usuario
     * @return Nuevo PIN generado
     */
    String resetearPin(Long usuarioId);
}