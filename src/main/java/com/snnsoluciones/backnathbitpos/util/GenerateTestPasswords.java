package com.snnsoluciones.backnathbitpos.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad para generar hashes de contraseñas para los datos de prueba.
 * Ejecutar este main para obtener los hashes que se deben usar en las migraciones SQL.
 */
public class GenerateTestPasswords {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Contraseña por defecto para todos los usuarios de prueba
        String defaultPassword = "Test123!";
        
        System.out.println("=== Generador de Contraseñas para Datos de Prueba ===");
        System.out.println();
        System.out.println("Contraseña en texto plano: " + defaultPassword);
        System.out.println("Hash BCrypt: " + encoder.encode(defaultPassword));
        System.out.println();
        System.out.println("Usuarios de prueba:");
        System.out.println("------------------");
        
        String[][] usuarios = {
            {"admin@nathbit.com", "Super Admin"},
            {"gerente@saborescr.com", "Admin de Grupo Gastronómico"},
            {"jefecajas@saborescr.com", "Jefe de Cajas"},
            {"cajero1@saborescr.com", "Cajero San José Centro"},
            {"cajero2@saborescr.com", "Cajero Escazú"},
            {"mesero1@saborescr.com", "Mesero Heredia"},
            {"admin@cafedelvalle.com", "Admin Café del Valle"}
        };
        
        for (String[] usuario : usuarios) {
            System.out.printf("%-30s - %s%n", usuario[0], usuario[1]);
        }
        
        System.out.println();
        System.out.println("SQL Update para establecer las contraseñas:");
        System.out.println("------------------------------------------");
        System.out.println("UPDATE public.usuarios_global SET password = '" + encoder.encode(defaultPassword) + "' WHERE activo = true;");
        
        System.out.println();
        System.out.println("Contraseñas específicas (si se requieren diferentes):");
        System.out.println("---------------------------------------------------");
        
        // Generar contraseñas específicas si se necesitan
        String[] passwords = {"Admin123!", "Gerente123!", "JefeCajas123!", "Cajero123!", "Mesero123!"};
        for (String pwd : passwords) {
            System.out.println(pwd + " -> " + encoder.encode(pwd));
        }
    }
}