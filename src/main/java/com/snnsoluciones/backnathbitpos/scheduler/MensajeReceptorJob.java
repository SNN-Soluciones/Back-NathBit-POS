package com.snnsoluciones.backnathbitpos.scheduler;

import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.*;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.*;
import com.snnsoluciones.backnathbitpos.repository.*;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.mr.MensajeReceptorXmlService;
import com.snnsoluciones.backnathbitpos.sign.SignerService;
import com.snnsoluciones.backnathbitpos.util.ByteArrayMultipartFile;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@Profile({"prod","qa"}) // en local lo puedes desactivar
@RequiredArgsConstructor
public class MensajeReceptorJob {

  private final MensajeReceptorBitacoraRepository bitacoraRepo;
  private final CompraRepository compraRepo;
  private final EmpresaRepository empresaRepo;
  private final MensajeReceptorXmlService xmlService;
  private final SignerService signerService;
  private final HaciendaClient haciendaClient;
  private final StorageService storage;
  private final S3PathBuilder s3;

  @Value("${mr.job.lote:20}") private int maxPorCiclo;
  @Value("${mr.job.max-intentos:6}") private int maxIntentos;
  @Value("${mr.job.backoff-min:15}") private int backoffMin; // minutos entre reintentos

  @Scheduled(fixedDelayString = "${mr.job.delay-ms:30000}")
  public void run() {
    try {
      List<MensajeReceptorBitacora> pendientes =
          bitacoraRepo.findPendientesParaProcesar(
              List.of(EstadoBitacora.PENDIENTE, EstadoBitacora.ERROR),
              LocalDateTime.now(),
              maxIntentos,
              PageRequest.of(0, maxPorCiclo));

      for (MensajeReceptorBitacora m : pendientes) {
        // Claim atómico
        int claimed = bitacoraRepo.claimParaProcesar(
            m.getId(), m.getEstado(), EstadoBitacora.PROCESANDO, LocalDateTime.now());
        if (claimed == 0) continue;

        try {
          procesarUno(m.getId());
        } catch (Exception ex) {
          log.error("MR job error id={}", m.getId(), ex);
        }
      }

    } catch (Exception e) {
      log.error("MR job ciclo falló", e);
    }
  }

  @Transactional
  protected void procesarUno(Long bitacoraId) {
    MensajeReceptorBitacora bit = bitacoraRepo.findById(bitacoraId).orElseThrow();
    Compra compra = compraRepo.findById(bit.getCompraId()).orElseThrow();
    Empresa empresa = empresaRepo.findById(compra.getEmpresa().getId()).orElseThrow();

    // 1) Construir XML (sin firmar)
    byte[] xmlBytes = xmlService.buildMensajeReceptorXml(compra, bit);

    // 2) Firmar
    byte[] firmado = signerService.signXmlForEmpresa(xmlBytes, empresa.getId(), TipoDocumento.MENSAJE_RECEPTOR);

    // 3) Guardar artefactos (MISMA ESTRATEGIA QUE consultarUno)
    //    Path consistente: .../facturas/mensaje-receptor/{YYYY}/{MMMM}/{clave}-{tag}.xml
    String pathXmlPlano = s3.buildXmlPathMR(compra.getClaveHacienda(), empresa, "mr");
    String pathXmlFirmado = s3.buildXmlPathMR(compra.getClaveHacienda(), empresa, "mr-firmado");

    MultipartFile xmlPlanoFile = createMultipartFile(
        xmlBytes,
        "compra_" + compra.getClaveHacienda() + "_mr.xml",
        "application/xml"
    );
    MultipartFile xmlFirmadoFile = createMultipartFile(
        firmado,
        "compra_" + compra.getClaveHacienda() + "_mr-firmado.xml",
        "application/xml"
    );

    storage.uploadFile(xmlPlanoFile, pathXmlPlano);
    storage.uploadFile(xmlFirmadoFile, pathXmlFirmado);

    bit.setXmlPath(pathXmlPlano);
    bit.setXmlFirmadoPath(pathXmlFirmado);

    // 4) Enviar a MH (MISMA FORMA DE OBTENER TOKEN QUE consultarUno)
    HaciendaTokenResponse token = haciendaClient.getToken(
        HaciendaAuthParams.builder()
            .empresaId(empresa.getId())
            .username(empresa.getConfigHacienda().getUsuarioHacienda())
            .password(empresa.getConfigHacienda().getClaveHacienda())
            .clientId(empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION ? "api-prod" : "api-test")
            .sandbox(empresa.getConfigHacienda().getAmbiente() != AmbienteHacienda.PRODUCCION)
            .build()
    );

    RecepcionRequest req = RecepcionRequest.builder()
        .clave(compra.getClaveHacienda())
        .fecha(OffsetDateTime.now(ZoneOffset.of("-06:00")).toString())
        // Emisor del XML original = proveedor
        .emisor(new IdentificacionDTO(
            compra.getProveedor().getNumeroIdentificacion(),
            compra.getProveedor().getTipoIdentificacion().toString()))
        // Receptor = esta empresa
        .receptor(new IdentificacionDTO(
            empresa.getIdentificacion(),
            empresa.getTipoIdentificacion().toString()))
        .comprobanteXml(Base64.getEncoder().encodeToString(firmado))
        .build();

    haciendaClient.postMensajeReceptor(
        token.getAccessToken(),
        empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION,
        req
    );

    // 5) Marcar “ENVIADO / PENDIENTE_RESPUESTA”
    bit.setEstado(EstadoBitacora.PENDIENTE);
    bit.setIntentos(bit.getIntentos() + 1);
    bit.setProximoIntento(LocalDateTime.now().plusMinutes(backoffMin));
    bit.setUpdatedAt(LocalDateTime.now());

    bitacoraRepo.save(bit);
  }

  @Scheduled(fixedDelayString = "${mr.job.consulta.delay-ms:45000}")
  public void consultarRespuestas() {
    try {
      List<MensajeReceptorBitacora> pendientes =
          bitacoraRepo.findPendientesParaProcesar(
              List.of(EstadoBitacora.PENDIENTE),
              LocalDateTime.now(),
              maxIntentos,
              PageRequest.of(0, maxPorCiclo));

      for (MensajeReceptorBitacora m : pendientes) {
        int claimed = bitacoraRepo.claimParaProcesar(
            m.getId(), EstadoBitacora.PENDIENTE, EstadoBitacora.PROCESANDO, LocalDateTime.now());
        if (claimed == 0) continue;

        try {
          consultarUno(m.getId());
        } catch (Exception ex) {
          log.error("MR consulta error id={}", m.getId(), ex);
        }
      }
    } catch (Exception e) {
      log.error("MR consulta ciclo falló", e);
    }
  }

  @Transactional
  protected void consultarUno(Long bitacoraId) {
    MensajeReceptorBitacora bit = bitacoraRepo.findById(bitacoraId).orElseThrow();
    Compra compra = compraRepo.findById(bit.getCompraId()).orElseThrow();
    Empresa empresa = empresaRepo.findById(compra.getEmpresa().getId()).orElseThrow();

    HaciendaTokenResponse token = haciendaClient.getToken(
        HaciendaAuthParams.builder()
            .empresaId(empresa.getId())
            .username(empresa.getConfigHacienda().getUsuarioHacienda())
            .password(empresa.getConfigHacienda().getClaveHacienda())
            .clientId(empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION
                ? "api-prod" : "api-test")
            .sandbox(empresa.getConfigHacienda().getAmbiente() != AmbienteHacienda.PRODUCCION)
            .build());

    ConsultaEstadoResponse resp = haciendaClient.getEstado(
        token.getAccessToken(), empresa.getConfigHacienda().getAmbiente() == AmbienteHacienda.PRODUCCION, compra.getClaveHacienda());

    byte[] respuestaBytes = null;

    // Asumo tu DTO trae la respuesta como Base64 (ajusta si ya la traes en bytes)
    // nombres típicos: getXmlRespuesta(), getRespuestaXml(), etc.
    if (resp.getRespuestaXmlBase64() != null && !resp.getRespuestaXmlBase64().isBlank()) {
      try {
        respuestaBytes = Base64.getDecoder().decode(resp.getRespuestaXmlBase64());
      } catch (IllegalArgumentException badB64) {
        // por si ya viene como xml plano
        respuestaBytes = resp.getRespuestaXmlBase64().getBytes(StandardCharsets.UTF_8);
      }
    }

    if (respuestaBytes != null) {
      String respuestaPath = s3.buildXmlPathMR(compra.getClaveHacienda(), empresa, TipoArchivoFactura.XML_RESPUESTA.name());
      MultipartFile respuestaFile = createMultipartFile(
          respuestaBytes,
          "compra_" + compra.getClaveHacienda() + "_respuesta.xml",
          "application/xml"
      );
      storage.uploadFile(respuestaFile, respuestaPath);
      bit.setXmlRespuestaPath(respuestaPath);
    }

    // Actualizar estado por resultado
    if (resp != null && "aceptado".equalsIgnoreCase(resp.getIndEstado())) {
      bit.setEstado(EstadoBitacora.ACEPTADA);
      compra.setEstado(EstadoCompra.ACEPTADA);
    } else if (resp != null && "rechazado".equalsIgnoreCase(resp.getIndEstado())) {
      bit.setEstado(EstadoBitacora.RECHAZADA);
      compra.setEstado(EstadoCompra.RECHAZADA);
    } else {
      // sigue pendiente: prepara reintento
      bit.setEstado(EstadoBitacora.PENDIENTE);
      bit.setProximoIntento(LocalDateTime.now().plusMinutes(backoffMin));
    }

    bit.setHaciendaMensaje(resp != null ? resp.getIndEstado() : null);
    bit.setIntentos(bit.getIntentos() + 1);
    bit.setUpdatedAt(LocalDateTime.now());

    compraRepo.save(compra);
    bitacoraRepo.save(bit);
  }

  /**
   * Helper para crear MultipartFile desde bytes
   */
  private MultipartFile createMultipartFile(byte[] content, String fileName, String contentType) {
    return new ByteArrayMultipartFile(content, "file", fileName, contentType);
  }
}