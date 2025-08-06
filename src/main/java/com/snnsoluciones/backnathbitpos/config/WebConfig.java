package com.snnsoluciones.backnathbitpos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class WebConfig {
  /**
   * Bean para el encoder de contraseñas.
   * Utiliza BCrypt que es el estándar recomendado para aplicaciones Spring.
   *
   * @return PasswordEncoder configurado con BCrypt
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
