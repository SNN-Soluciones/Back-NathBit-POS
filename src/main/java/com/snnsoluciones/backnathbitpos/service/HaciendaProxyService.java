// HaciendaProxyService.java
package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.*;
import com.snnsoluciones.backnathbitpos.dto.ContribuyenteDTO.ActividadDTO;
import com.snnsoluciones.backnathbitpos.dto.ContribuyenteDTO.RegimenDTO;
import com.snnsoluciones.backnathbitpos.dto.ContribuyenteDTO.SituacionDTO;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.util.*;

@Service
public class HaciendaProxyService {

    private static final String HACIENDA_API = "https://api.hacienda.go.cr/fe/ae?identificacion=";
    private static final String GOMETA_API = "https://apis.gometa.org/cedulas/";
    private static final String GOMETA_KEY = "iyPaqeKXKCCgwKX";

    private final RestTemplate restTemplate = new RestTemplate();

    public ContribuyenteDTO consultar(String identificacion) {
        try {
            return consultarHacienda(identificacion);
        } catch (Exception e) {
            return consultarGometa(identificacion, e.getMessage());
        }
    }

    // ---------------- HACIENDA ----------------
    private ContribuyenteDTO consultarHacienda(String identificacion) {
        String url = HACIENDA_API + identificacion;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "NathBit-POS/1.0");

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        return mapHacienda(response.getBody(), identificacion);
    }

    // ---------------- GOMETA ----------------
    private ContribuyenteDTO consultarGometa(String identificacion, String haciendaError) {
        String url = GOMETA_API + identificacion + "?key=" + GOMETA_KEY;

        Map body = restTemplate.getForObject(url, Map.class);
        return mapGometa(body, identificacion);
    }

    // ---------------- MAPPERS ----------------

    private ContribuyenteDTO mapHacienda(Map body, String identificacion) {
        ContribuyenteDTO dto = new ContribuyenteDTO();
        dto.setFuente("HACIENDA");
        dto.setIdentificacion(identificacion);
        dto.setTipoIdentificacion((String) body.get("tipoIdentificacion"));
        dto.setNombre((String) body.get("nombre"));

        dto.setRegimen(mapRegimen((Map) body.get("regimen")));
        dto.setSituacion(mapSituacion((Map) body.get("situacion")));
        dto.setActividades(mapActividades((List<Map>) body.get("actividades")));

        return dto;
    }

    private ContribuyenteDTO mapGometa(Map body, String identificacion) {
        ContribuyenteDTO dto = new ContribuyenteDTO();
        dto.setFuente("GOMETA");
        dto.setIdentificacion(identificacion);
        dto.setTipoIdentificacion((String) body.get("tipoIdentificacion"));
        dto.setNombre((String) body.get("nombre"));

        dto.setRegimen(mapRegimen((Map) body.get("regimen")));
        dto.setSituacion(mapSituacion((Map) body.get("situacion")));
        dto.setActividades(mapActividades((List<Map>) body.get("actividades")));

        return dto;
    }

    private RegimenDTO mapRegimen(Map map) {
        if (map == null) return null;
        RegimenDTO dto = new RegimenDTO();
        dto.setCodigo((Integer) map.get("codigo"));
        dto.setDescripcion((String) map.get("descripcion"));
        return dto;
    }

    private SituacionDTO mapSituacion(Map map) {
        if (map == null) return null;
        SituacionDTO dto = new SituacionDTO();
        dto.setEstado((String) map.get("estado"));
        dto.setMoroso((String) map.get("moroso"));
        dto.setOmiso((String) map.get("omiso"));
        dto.setAdministracionTributaria((String) map.get("administracionTributaria"));
        dto.setMensaje((String) map.get("mensaje"));
        return dto;
    }

    private List<ActividadDTO> mapActividades(List<Map> list) {
        if (list == null) return List.of();

        List<ActividadDTO> actividades = new ArrayList<>();
        for (Map a : list) {
            ActividadDTO dto = new ActividadDTO();
            dto.setCodigo((String) a.get("codigo"));
            dto.setDescripcion((String) a.get("descripcion"));
            dto.setEstado((String) a.get("estado"));
            dto.setTipo((String) a.get("tipo"));
            actividades.add(dto);
        }
        return actividades;
    }
}