package com.snnsoluciones.backnathbitpos.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snnsoluciones.backnathbitpos.config.HaciendaAuthService;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import com.snnsoluciones.backnathbitpos.enums.mh.AmbienteHacienda;
import com.snnsoluciones.backnathbitpos.exception.HaciendaAuthException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class HaciendaClientService {
    
    private final HaciendaAuthService authService;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();
    
    @Value("${hacienda.api.url.sandbox:https://api-sandbox.comprobanteselectronicos.go.cr/recepcion/v1}")
    private String urlSandbox;
    
    @Value("${hacienda.api.url.produccion:https://api.comprobanteselectronicos.go.cr/recepcion/v1}")
    private String urlProduccion;
    
    /**
     * Envía el documento a Hacienda
     */
    public HaciendaResponse enviarDocumento(String xmlFirmado, String clave, 
                                           EmpresaConfigHacienda config) {
        try {
            String token = authService.obtenerToken(config);
            String baseUrl = config.getAmbiente() == AmbienteHacienda.SANDBOX
                ? urlSandbox : urlProduccion;
            
            // Preparar JSON para envío
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode json = mapper.createObjectNode();
            json.put("clave", clave);
            json.put("fecha", LocalDateTime.now().toString());
            json.put("emisor", mapper.createObjectNode()
                .put("tipoIdentificacion", config.getEmpresa().getTipoIdentificacion().getCodigo())
                .put("numeroIdentificacion", config.getEmpresa().getIdentificacion()));
            
            // TODO: Agregar receptor si es necesario
            
            json.put("comprobanteXml", Base64.getEncoder().encodeToString(xmlFirmado.getBytes()));
            
            // Enviar
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(baseUrl + "/recepcion")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                
                if (response.code() == 403) {
                    throw new HaciendaAuthException("Credenciales inválidas o certificado vencido");
                }
                
                if (!response.isSuccessful()) {
                    log.error("Error Hacienda: {} - {}", response.code(), responseBody);
                    throw new RuntimeException("Error enviando a Hacienda: " + response.code());
                }
                
                return mapper.readValue(responseBody, HaciendaResponse.class);
            }
            
        } catch (Exception e) {
            log.error("Error enviando documento a Hacienda: {}", e.getMessage(), e);
            throw new RuntimeException("Error comunicación con Hacienda", e);
        }
    }
    
    /**
     * Consulta el estado de un documento
     */
    public HaciendaResponse consultarEstado(String clave, EmpresaConfigHacienda config) {
        try {
            String token = authService.obtenerToken(config);
            String baseUrl = config.getAmbiente() == AmbienteHacienda.SANDBOX 
                ? urlSandbox : urlProduccion;
            
            Request request = new Request.Builder()
                .url(baseUrl + "/recepcion/" + clave)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Error consultando estado: " + response.code());
                }
                
                return new ObjectMapper().readValue(responseBody, HaciendaResponse.class);
            }
            
        } catch (Exception e) {
            log.error("Error consultando estado: {}", e.getMessage(), e);
            throw new RuntimeException("Error consultando estado", e);
        }
    }
}

