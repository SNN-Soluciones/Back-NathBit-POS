package com.snnsoluciones.backnathbitpos.config;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfiguration {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String starttls;
    
    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private String sslEnable;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:30000}")
    private String connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:60000}")
    private String timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:60000}")
    private String writeTimeout;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Configuración básica
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        // Propiedades adicionales
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.smtp.ssl.enable", sslEnable);
        props.put("mail.smtp.connectiontimeout", connectionTimeout);
        props.put("mail.smtp.timeout", timeout);
        props.put("mail.smtp.writetimeout", writeTimeout);
        
        // Propiedades adicionales para debug
        props.put("mail.debug", "false"); // Cambiado a true según tu config
        
        // Para Gmail con SSL (puerto 465)
        if (host.contains("gmail") && port == 465) {
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }
        
        return mailSender;
    }
    
    // Bean alternativo para desarrollo/testing sin configuración real
    @Bean
    @ConditionalOnProperty(name = "spring.mail.mock", havingValue = "true")
    public JavaMailSender mockMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(25);
        // Esta configuración no enviará emails reales
        return mailSender;
    }
}