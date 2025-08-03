package com.snnsoluciones.backnathbitpos.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración para habilitar tareas programadas en la aplicación.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // La anotación @EnableScheduling es suficiente para habilitar el scheduling
    // Spring Boot configurará automáticamente un ThreadPoolTaskScheduler
}