package com.snnsoluciones.backnathbitpos.integrations.controller;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.FacturaDocumentoHacienda;
import com.snnsoluciones.backnathbitpos.enums.facturacion.EstadoFactura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.PasoFacturacion;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaCallbackRequest;
import com.snnsoluciones.backnathbitpos.repository.FacturaDocumentoHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRepository;
import com.snnsoluciones.backnathbitpos.service.FacturaJobService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/hacienda")
@RequiredArgsConstructor
public class HaciendaCallbackController {

    private final FacturaRepository facturaRepository;
    private final FacturaDocumentoHaciendaRepository docHaciendaRepository;
    private final FacturaJobService jobService;
    private final StorageService storageService;

    /**
     * Endpoint público para que Hacienda notifique el resultado.
     * Responder 200 siempre que el payload sea válido; Hacienda reintenta si no recibe 200.
     */
    @PostMapping("/callback")
    @Transactional
    public ResponseEntity<String> recibirCallback(@RequestBody HaciendaCallbackRequest body) {
        if (body == null || !StringUtils.hasText(body.getClave())) {
            log.warn("[Callback] Payload inválido (sin clave)");
            return ResponseEntity.badRequest().body("payload inválido");
        }

        String clave = body.getClave();
        String ind = body.getIndEstado() == null ? "" : body.getIndEstado().toLowerCase(Locale.ROOT);
        String detalle = body.getDetalleMensaje();

        log.info("[Callback][{}] ind-estado={} detalle={}", clave, ind, detalle);

        // 1) Resolver factura por clave (ajusta si tu repo usa otro método)
        Factura factura = facturaRepository.findByClave(clave)
                .orElse(null);
        if (factura == null) {
            log.error("[Callback][{}] Factura no encontrada por clave", clave);
            // Igualmente respondemos 200 para no provocar reintentos infinitos
            return ResponseEntity.ok("no-op");
        }

        // 2) Guardar respuesta XML si vino
        String s3RespKey = null;
        if (StringUtils.hasText(body.getRespuestaXmlBase64())) {
            byte[] bytes = Base64.getDecoder().decode(body.getRespuestaXmlBase64());
            s3RespKey = "NathBit-POS/RESPUESTAS/" + clave + "-respuesta-mh.xml";
            storageService.uploadFile(new java.io.ByteArrayInputStream(bytes), s3RespKey, "application/xml", bytes.length);
            log.info("[Callback][{}] respuesta-xml guardada en S3 => {}", clave, s3RespKey);
        }

        // 3) Sincronizar DocHacienda
        FacturaDocumentoHacienda hda = docHaciendaRepository.findByClave(clave)
                .orElseGet(() -> {
                    FacturaDocumentoHacienda nuevo = new FacturaDocumentoHacienda();
                    nuevo.setFacturaId(factura.getId());
                    nuevo.setClave(clave);
                    // ambiente desde empresa (por si no existía registro)
                    Empresa emp = factura.getSucursal().getEmpresa();
                    nuevo.setAmbiente(emp.getConfigHacienda().getAmbiente());
                    nuevo.setCreatedAt(LocalDateTime.now());
                    return nuevo;
                });
        if (s3RespKey != null) hda.setS3KeyXmlRespuesta(s3RespKey);
        hda.setFechaEstado(LocalDateTime.now());
        hda.setUpdatedAt(LocalDateTime.now());
        docHaciendaRepository.save(hda);

        // 4) Transiciones de estado/flujo
        if (ind.contains("acept")) {
            factura.setEstado(EstadoFactura.ACEPTADA);
            facturaRepository.save(factura);
            // Avanzar a GENERAR_PDF (o el siguiente paso que uses)
            jobService.avanzarPasoPorClave(clave, PasoFacturacion.GENERAR_PDF);
            log.info("[Callback][{}] ACEPTADO → avanzar a GENERAR_PDF", clave);
        } else if (ind.contains("rechaz")) {
            factura.setEstado(EstadoFactura.RECHAZADA);
            facturaRepository.save(factura);
            jobService.finalizarPorClave(clave, "RECHAZADO: " + (detalle == null ? "" : detalle));
            log.info("[Callback][{}] RECHAZADO → finalizar flujo", clave);
        } else {
            // recibido/procesando → opcional: reprogramar una consulta defensiva
            jobService.reprogramarPorClave(clave, LocalDateTime.now().plusMinutes(3));
            log.info("[Callback][{}] ind-estado={} → reprogramado para verificación", clave, ind);
        }

        return ResponseEntity.ok("ok");
    }
}