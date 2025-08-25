package com.snnsoluciones.backnathbitpos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "hacienda.firma")
public class HaciendaFirmaConfig {
    
    private String version = "4.4";
    
    private Politica politica = new Politica();
    private Xades xades = new Xades();
    private Algoritmos algoritmos = new Algoritmos();
    
    @Data
    public static class Politica {
        private String url;
        private String hash;
    }
    
    @Data
    public static class Xades {
        private String namespace = "http://uri.etsi.org/01903/v1.3.2#";
        private String namespace141 = "http://uri.etsi.org/01903/v1.4.1#";
    }
    
    @Data
    public static class Algoritmos {
        private String canonicalizacion = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
        private String firma = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
        private String digest = "http://www.w3.org/2001/04/xmlenc#sha256";
    }
}