package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import com.snnsoluciones.backnathbitpos.service.TerminalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio simplificado para generación de consecutivos.
 * Delega la lógica principal al TerminalService existente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsecutivoService {
    
    private final TerminalService terminalService;
    
    /**
     * Genera el siguiente consecutivo para un tipo de documento
     * 
     * @param terminalId ID de la terminal
     * @param tipoDocumento Tipo de documento
     * @return Consecutivo formateado de 20 dígitos
     */
    public String generarConsecutivo(Long terminalId, TipoDocumento tipoDocumento) {
        // Delegar al servicio existente que ya maneja bloqueos y validaciones
        return terminalService.generarNumeroConsecutivo(terminalId, tipoDocumento);
    }
}