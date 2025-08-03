package com.snnsoluciones.backnathbitpos.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuración de caché para la aplicación.
 * Por ahora usa caché en memoria, pero puede ser fácilmente cambiado a Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    SimpleCacheManager cacheManager = new SimpleCacheManager();

    // Definir los cachés disponibles
    cacheManager.setCaches(Arrays.asList(
        new ConcurrentMapCache("tokens"),
        new ConcurrentMapCache("users"),
        new ConcurrentMapCache("permissions"),
        new ConcurrentMapCache("roles"),
        new ConcurrentMapCache("products"),
        new ConcurrentMapCache("categories"),
        new ConcurrentMapCache("tables"),
        new ConcurrentMapCache("orders")
    ));

    return cacheManager;
  }
}