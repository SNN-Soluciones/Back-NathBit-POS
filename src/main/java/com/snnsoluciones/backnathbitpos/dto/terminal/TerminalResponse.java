package com.snnsoluciones.backnathbitpos.dto.terminal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalResponse {
    
    private Long id;
    private String numeroTerminal;
    private String nombre;
    private String descripcion;
    private Boolean activa;
    private String impresoraPredeterminada;
    private Boolean imprimirAutomatico;
    
    // Información de la sucursal
    private Long sucursalId;
    private String sucursalNombre;
    private String sucursalNumero;
    
    // Consecutivos actuales por tipo de documento
    private Map<String, Long> consecutivosActuales;
    
    // Información de sesión (si hay una activa)
    private Boolean tieneSesionActiva;
    private String usuarioSesion;
    
    // Auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Método helper para mostrar si está cerca del límite
    public boolean cercaDelLimite() {
        if (consecutivosActuales == null) return false;
        
        return consecutivosActuales.values().stream()
            .anyMatch(valor -> valor >= 9_999_999_000L);
    }
}