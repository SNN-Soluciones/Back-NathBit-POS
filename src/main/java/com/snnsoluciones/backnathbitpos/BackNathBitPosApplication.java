package com.snnsoluciones.backnathbitpos;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackNathBitPosApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Costa_Rica"));
    SpringApplication.run(BackNathBitPosApplication.class, args);
  }

}
