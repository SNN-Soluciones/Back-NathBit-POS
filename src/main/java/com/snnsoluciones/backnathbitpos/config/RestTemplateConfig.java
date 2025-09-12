package com.snnsoluciones.backnathbitpos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// Configuration para RestTemplate
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configurar timeouts
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 segundos
        factory.setReadTimeout(10000);    // 10 segundos

        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }
}