// ZonaLayoutService.java
package com.snnsoluciones.backnathbitpos.service.mesas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.dto.mesas.GuardarLayoutRequest;
import com.snnsoluciones.backnathbitpos.dto.mesas.MesaLayoutDTO;
import com.snnsoluciones.backnathbitpos.entity.ZonaLayout;
import com.snnsoluciones.backnathbitpos.entity.ZonaMesa;
import com.snnsoluciones.backnathbitpos.repository.ZonaLayoutRepository;
import com.snnsoluciones.backnathbitpos.repository.ZonaMesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZonaLayoutService {
    
    private final ZonaLayoutRepository layoutRepo;
    private final ZonaMesaRepository zonaRepo;
    private final ObjectMapper objectMapper;

    /**
     * Guardar o actualizar layout de una zona
     */
    @Transactional
    public void guardarLayout(Long zonaId, GuardarLayoutRequest request) {
        ZonaMesa zona = zonaRepo.findById(zonaId)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));

        try {
            String layoutJson = objectMapper.writeValueAsString(request.getMesas());
            
            ZonaLayout layout = layoutRepo.findByZonaId(zonaId)
                .orElse(ZonaLayout.builder()
                    .zona(zona)
                    .build());
            
            layout.setLayoutJson(layoutJson);
            layoutRepo.save(layout);
            
            log.info("Layout guardado para zona {} con {} mesas", zonaId, request.getMesas().size());
            
        } catch (JsonProcessingException e) {
            log.error("Error al serializar layout para zona {}", zonaId, e);
            throw new RuntimeException("Error al guardar layout", e);
        }
    }

    /**
     * Obtener layout de una zona
     */
    @Transactional(readOnly = true)
    public List<MesaLayoutDTO> obtenerLayout(Long zonaId) {
        return layoutRepo.findByZonaId(zonaId)
            .map(layout -> {
                try {
                    MesaLayoutDTO[] layouts = objectMapper.readValue(
                        layout.getLayoutJson(), 
                        MesaLayoutDTO[].class
                    );
                    return Arrays.asList(layouts);
                } catch (JsonProcessingException e) {
                    log.error("Error al deserializar layout de zona {}", zonaId, e);
                    return Collections.<MesaLayoutDTO>emptyList();
                }
            })
            .orElse(Collections.emptyList());
    }

    /**
     * Eliminar layout de una zona
     */
    @Transactional
    public void eliminarLayout(Long zonaId) {
        layoutRepo.deleteByZonaId(zonaId);
        log.info("Layout eliminado para zona {}", zonaId);
    }

    /**
     * Verificar si existe layout para una zona
     */
    @Transactional(readOnly = true)
    public boolean existeLayout(Long zonaId) {
        return layoutRepo.existsByZonaId(zonaId);
    }
}
