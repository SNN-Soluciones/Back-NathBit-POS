package com.snnsoluciones.backnathbitpos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Servicio para procesar templates Thymeleaf
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThymeleafService {

    private final TemplateEngine templateEngine;

    /**
     * Procesa un template Thymeleaf y retorna el HTML generado
     * 
     * @param templateName Nombre del template (sin extensión)
     * @param variables Variables a pasar al template
     * @return HTML procesado
     */
    public String processTemplate(String templateName, Map<String, Object> variables) {
        log.debug("Procesando template: {} con {} variables", templateName, variables.size());
        
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            String html = templateEngine.process(templateName, context);
            
            log.debug("Template procesado exitosamente: {}", templateName);
            return html;
            
        } catch (Exception e) {
            log.error("Error procesando template {}: {}", templateName, e.getMessage(), e);
            throw new RuntimeException("Error al procesar template: " + e.getMessage(), e);
        }
    }
}