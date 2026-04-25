package com.snnsoluciones.backnathbitpos;

import java.util.TimeZone;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class BackNathBitPosApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Costa_Rica"));
    SpringApplication.run(BackNathBitPosApplication.class, args);
  }

  @Bean
  CommandLineRunner generatePin() {
    return args -> {
      BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
      String hash = encoder.encode("1234");
      System.out.println("HASH PIN 1234: " + hash);
    };
  }
}