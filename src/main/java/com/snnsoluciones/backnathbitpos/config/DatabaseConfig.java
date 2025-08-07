package com.snnsoluciones.backnathbitpos.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuración explícita de Flyway para asegurar el orden correcto de inicialización
 */
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class DatabaseConfig {

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @Value("${spring.datasource.username}")
  private String datasourceUsername;

  @Value("${spring.datasource.password}")
  private String datasourcePassword;

  /**
   * Bean de Flyway configurado manualmente para asegurar que se ejecute primero
   */
  @Bean(initMethod = "migrate")
  @Primary
  public Flyway flyway(DataSource dataSource, FlywayProperties properties) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations(properties.getLocations().toArray(new String[0]))
        .baselineOnMigrate(properties.isBaselineOnMigrate())
        .baselineVersion(String.valueOf(properties.getBaselineVersion()))
        .schemas(properties.getSchemas().toArray(new String[0]))
        .table(properties.getTable())
        .cleanDisabled(true) // Nunca hacer clean en producción
        .validateOnMigrate(false) // Deshabilitamos validación por ahora
        .outOfOrder(true) // Permitir migraciones fuera de orden
        .load();
  }
}