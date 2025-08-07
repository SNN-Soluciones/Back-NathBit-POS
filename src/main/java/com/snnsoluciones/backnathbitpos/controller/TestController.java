package com.snnsoluciones.backnathbitpos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Endpoint temporal para generar hash de contraseñas
     * ELIMINAR EN PRODUCCIÓN
     */
    @GetMapping("/generate-hash")
    public String generateHash(@RequestParam(name = "password") String password) {
        return passwordEncoder.encode(password);
    }
    
    /**
     * Endpoint para verificar si un hash coincide con una contraseña
     * ELIMINAR EN PRODUCCIÓN
     */
    @PostMapping("/verify-hash")
    public boolean verifyHash(@RequestParam String password, @RequestParam String hash) {
        return passwordEncoder.matches(password, hash);
    }
}