package com.snnsoluciones.backnathbitpos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    
    @Value("${app.mailreceptor.api-key}")
    private String validApiKey;
    
    @Value("${app.mailreceptor.enabled:true}")
    private boolean apiKeyEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 👇 SOLO estos endpoints específicos del MailReceptor requieren API Key
        if (path.equals("/api/facturas-recepcion/procesar") ||
            path.equals("/api/facturas-recepcion/procesar-email") ||
            path.startsWith("/api/facturas-recepcion/webhook") ||
            path.contains("/buscar-por-cedula-email")) {

            if (apiKeyEnabled) {
                String apiKey = request.getHeader(API_KEY_HEADER);

                if (apiKey == null || apiKey.isEmpty()) {
                    sendErrorResponse(response, "API Key requerida");
                    return;
                }

                if (!validApiKey.equals(apiKey)) {
                    sendErrorResponse(response, "API Key inválida");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
            "{\"success\": false, \"message\": \"%s\", \"error\": \"UNAUTHORIZED\"}", 
            message
        ));
    }
}