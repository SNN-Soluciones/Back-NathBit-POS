package com.snnsoluciones.backnathbitpos.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Utilidad para obtener la dirección IP del cliente
 */
@Slf4j
@Component
public class IpAddressUtils {

    private static final List<String> IP_HEADER_CANDIDATES = Arrays.asList(
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    );

    /**
     * Obtiene la IP del cliente desde el request actual
     */
    public static String getClientIpAddress() {
        HttpServletRequest request = getCurrentHttpRequest()
            .orElseThrow(() -> new IllegalStateException("No se puede obtener el request actual"));
        
        return getClientIpAddress(request);
    }

    /**
     * Obtiene la IP del cliente desde un HttpServletRequest específico
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        // Buscar en headers de proxy
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                // Tomar la primera IP si hay múltiples (separadas por comas)
                String ip = ipList.split(",")[0].trim();
                if (isValidIpAddress(ip)) {
                    log.trace("IP obtenida del header {}: {}", header, ip);
                    return ip;
                }
            }
        }

        // Usar remoteAddr como fallback
        String remoteAddr = request.getRemoteAddr();
        
        // Normalizar IPv6 localhost a IPv4
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        
        log.trace("IP obtenida de remoteAddr: {}", remoteAddr);
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Obtiene el User-Agent del request actual
     */
    public static String getUserAgent() {
        return getCurrentHttpRequest()
            .map(request -> request.getHeader("User-Agent"))
            .orElse("unknown");
    }

    /**
     * Obtiene información completa del cliente
     */
    public static ClientInfo getClientInfo() {
        HttpServletRequest request = getCurrentHttpRequest()
            .orElse(null);
        
        if (request == null) {
            return ClientInfo.unknown();
        }

        return ClientInfo.builder()
            .ipAddress(getClientIpAddress(request))
            .userAgent(request.getHeader("User-Agent"))
            .referer(request.getHeader("Referer"))
            .acceptLanguage(request.getHeader("Accept-Language"))
            .build();
    }

    /**
     * Obtiene el HttpServletRequest actual del contexto
     */
    private static Optional<HttpServletRequest> getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return Optional.of(attrs.getRequest());
            }
        } catch (Exception e) {
            log.warn("Error obteniendo request del contexto: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Valida si una cadena es una dirección IP válida
     */
    private static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // Validación básica para IPv4
        if (ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            String[] parts = ip.split("\\.");
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        }
        
        // Para IPv6, aceptar si contiene ':'
        return ip.contains(":");
    }

    /**
     * Clase para encapsular información del cliente
     */
    @lombok.Data
    @lombok.Builder
    public static class ClientInfo {
        private String ipAddress;
        private String userAgent;
        private String referer;
        private String acceptLanguage;

        public static ClientInfo unknown() {
            return ClientInfo.builder()
                .ipAddress("unknown")
                .userAgent("unknown")
                .build();
        }
    }
}