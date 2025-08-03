package com.snnsoluciones.backnathbitpos.util;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {
    
    public void validarFortaleza(String password) {
        // Verificar longitud mínima
        // Verificar mayúsculas/minúsculas
        // Verificar números
        // Verificar caracteres especiales (opcional)
        // Verificar que no sea común (opcional)
    }
    
    public void validarNoEsComun(String password) {
        // Lista de contraseñas comunes
        // Verificar contra diccionario
    }
}