// src/main/java/com/snnsoluciones/backnathbitpos/service/impl/EmpresaCreacionServiceImpl.java
package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ActividadEconomicaRequest;
import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaResponse;
import com.snnsoluciones.backnathbitpos.entity.*;
import com.snnsoluciones.backnathbitpos.mappers.EmpresaConfigHaciendaMapper;
import com.snnsoluciones.backnathbitpos.repository.ActividadEconomicaRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaActividadRepository;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.service.*;
import com.snnsoluciones.backnathbitpos.util.S3PathBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmpresaCreacionServiceImpl implements EmpresaCreacionService {

  private final EmpresaConfigHaciendaMapper empresaConfigHaciendaMapper;

  private final EmpresaRepository empresaRepository;
  private final ConfigHaciendaService configHaciendaService;
  private final StorageService storageService;
  private final CertificadoService certificadoService;
  private final ActividadEconomicaRepository actividadEconomicaRepository;
  private final EmpresaActividadRepository empresaActividadRepository;
  private final UbicacionService ubicacionService;
  private final EmailService emailService;
  private final S3PathBuilder s3PathBuilder;

  @Override
  @Transactional
  public CrearEmpresaCompletaResponse crearEmpresaCompleta(
      CrearEmpresaCompletaRequest request,
      MultipartFile logo,
      MultipartFile certificado) {

    log.info("=== INICIANDO CREACIÓN COMPLETA DE EMPRESA ===");
    log.info("Empresa: {} - {}", request.getNombreRazonSocial(), request.getIdentificacion());
    LocalDate fechaActual = null;

    try {
      // 1. Validaciones previas
      log.info("1. Validando datos de la empresa...");
      validarDatosEmpresa(request);

      // 2. Preparar nombre base para archivos
      String nombreEmpresa = request.getNombreComercial() != null ?
          request.getNombreComercial() : request.getNombreRazonSocial();
      log.info("2. Nombre de empresa para archivos: {}", nombreEmpresa);

      // 3. Validar certificado ANTES de crear cualquier cosa
      String certificadoUrl = null;
      if (request.getRequiereHacienda()) {
        log.info("3. Empresa requiere Hacienda, validando certificado...");

        if (certificado == null || certificado.isEmpty()) {
          throw new RuntimeException("El certificado es requerido para facturación electrónica");
        }

        if (request.getConfigHacienda() == null) {
          throw new RuntimeException("La configuración de Hacienda es requerida");
        }

        String pin = request.getConfigHacienda().getPinCertificado();
        if (pin == null || pin.trim().isEmpty()) {
          throw new RuntimeException("El PIN del certificado es requerido");
        }

        // Extraer información adicional
        LocalDate fechaVencimiento = certificadoService.extraerFechaVencimiento(certificado, pin);
        if (fechaVencimiento == null) {
          throw new RuntimeException("No se pudo extraer la fecha de vencimiento del certificado");
        }
        fechaActual = fechaVencimiento;

        if (fechaVencimiento.isBefore(LocalDate.now())) {
          throw new RuntimeException("El certificado está vencido");
        }

        if (fechaVencimiento.isBefore(LocalDate.now().plusDays(30))) {
          log.warn("⚠️ ADVERTENCIA: El certificado vence pronto: {}", fechaVencimiento);
        }

        X509Certificate x509 = certificadoService.extraerInformacionCertificado(certificado, pin);
        if (x509 != null) {
          log.info("   ✅ Certificado válido para: {}", x509.getSubjectDN().getName());
          log.info("   ✅ Emitido por: {}", x509.getIssuerDN().getName());
          log.info("   ✅ Válido hasta: {}", fechaVencimiento);
        }

        // Si todo está bien, proceder a subir
        log.info("   - Subiendo certificado encriptado...");
        certificadoUrl = subirCertificado(certificado, nombreEmpresa);
        log.info("   ✅ Certificado subido correctamente");
      } else {
        log.info("3. Empresa NO requiere Hacienda, omitiendo certificado");
      }

      // 4. Procesar logo si existe
      String logoUrl = null;
      if (logo != null && !logo.isEmpty()) {
        log.info("4. Subiendo logo de la empresa...");
        logoUrl = subirLogo(logo, nombreEmpresa);
        log.info("   ✅ Logo subido correctamente");
      } else {
        log.info("4. No se proporcionó logo");
      }

      // 5. AHORA SÍ crear la empresa (con archivos ya validados y subidos)
      log.info("5. Creando empresa en base de datos...");
      Empresa empresa = crearEmpresa(request, logoUrl);
      log.info("   ✅ Empresa creada con ID: {}", empresa.getId());

      // 6. Si requiere Hacienda, crear configuración y actividades
      if (request.getRequiereHacienda() && request.getConfigHacienda() != null) {
        log.info("6. Creando configuración de Hacienda...");
        crearConfiguracionHacienda(empresa, request.getConfigHacienda(), certificadoUrl,
            fechaActual);
        log.info("   ✅ Configuración de Hacienda creada");

        log.info("7. Creando actividades económicas...");
        crearActividadesEconomicas(empresa, request.getConfigHacienda().getActividades());
        log.info("   ✅ {} actividades económicas creadas",
            request.getConfigHacienda().getActividades().size());
      }

      try {
        log.info("8. Enviando email de bienvenida...");

        // Determinar email destino (puede ser el email principal o el de notificación)
        String emailDestino = empresa.getEmailNotificacion() != null ?
            empresa.getEmailNotificacion() : empresa.getEmail();

        boolean emailEnviado = emailService.enviarConfirmacionEmpresaCreada(
            empresa,
            emailDestino
        );

        if (emailEnviado) {
          log.info("   ✅ Email de bienvenida enviado a: {}", emailDestino);
        } else {
          log.warn(
              "   ⚠️ No se pudo enviar el email de bienvenida, pero la empresa fue creada correctamente");
        }
      } catch (Exception e) {
        // No fallar la creación por error de email
        log.error("Error enviando email de bienvenida, pero continuando: {}", e.getMessage());
      }

// 9. Construir respuesta
      log.info("=== EMPRESA CREADA EXITOSAMENTE ===");

      CrearEmpresaCompletaResponse response = CrearEmpresaCompletaResponse.builder()
          .empresaId(empresa.getId())
          .mensaje("Empresa creada exitosamente")
          .nombreComercial(empresa.getNombreComercial())
          .identificacion(empresa.getIdentificacion())
          .requiereHacienda(empresa.getRequiereHacienda())
          .build();

// Si se creó config de Hacienda, agregar info
      if (empresa.getRequiereHacienda() && empresa.getConfigHacienda() != null) {
        response.setConfigHacienda(empresaConfigHaciendaMapper.toDto(empresa.getConfigHacienda()));
      }

      return response;

    } catch (RuntimeException e) {
      log.error("❌ Error de negocio: {}", e.getMessage());
      throw e; // Re-lanzar para que haga rollback
    } catch (Exception e) {
      log.error("❌ Error inesperado creando empresa", e);
      throw new RuntimeException("Error al crear la empresa: " + e.getMessage(), e);
    }
  }

  private void validarDatosEmpresa(CrearEmpresaCompletaRequest request) {
    // Validar que no exista otra empresa con la misma identificación
    if (empresaRepository.existsByIdentificacion(request.getIdentificacion())) {
      throw new RuntimeException(
          "Ya existe una empresa con la identificación: " + request.getIdentificacion());
    }

    // Validar email único
    if (empresaRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Ya existe una empresa con el email: " + request.getEmail());
    }

    // Si requiere Hacienda, validar datos obligatorios
    if (request.getRequiereHacienda()) {
      if (request.getConfigHacienda() == null) {
        throw new RuntimeException("La configuración de Hacienda es requerida");
      }

      if (request.getConfigHacienda().getActividades() == null ||
          request.getConfigHacienda().getActividades().isEmpty()) {
        throw new RuntimeException("Debe seleccionar al menos una actividad económica");
      }

      // Validar que las actividades existan
      for (ActividadEconomicaRequest act : request.getConfigHacienda().getActividades()) {
        actividadEconomicaRepository.findByCodigo(act.getCodigo())
            .orElseThrow(RuntimeException::new);

      }
    }
  }

  private String subirLogo(MultipartFile logo, String nombreComercial) {
    // Validar tipo de archivo
    String contentType = logo.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new RuntimeException("El logo debe ser una imagen");
    }

    // Validar tamaño (5MB máximo)
    if (logo.getSize() > 5 * 1024 * 1024) {
      throw new RuntimeException("El logo no puede superar 5MB");
    }

    // Obtener extensión del archivo
    String filename = logo.getOriginalFilename();
    String extension = "jpg"; // default
    if (filename != null && filename.lastIndexOf('.') > 0) {
      extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    // USAR S3PathBuilder para generar la ruta correcta
    String s3Key = s3PathBuilder.buildLogoPath(nombreComercial, extension);

    log.info("Subiendo logo con key: {}", s3Key);

    // Subir a S3 (público)
    return storageService.subirArchivo(logo, s3Key, contentType, false);
  }

  private String subirCertificado(MultipartFile certificado, String nombreComercial) {
    try {
      // Leer bytes del certificado
      byte[] certificadoBytes = certificado.getBytes();

      // Generar nombre único con timestamp
      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
      String nombreArchivo = String.format("certificado_%s.p12", timestamp);

      // USAR S3PathBuilder para generar la ruta correcta
      String s3Key = s3PathBuilder.buildCertificadoPath(nombreComercial, nombreArchivo);

      log.info("Subiendo certificado con key: {}", s3Key);

      // Subir archivo encriptado
      return storageService.subirArchivo(
          certificadoBytes,
          s3Key,
          "application/x-pkcs12",
          true  // Privado
      );

    } catch (Exception e) {
      throw new RuntimeException("Error al procesar el certificado: " + e.getMessage(), e);
    }
  }

  private Empresa crearEmpresa(CrearEmpresaCompletaRequest request, String logoUrl) {
    Empresa empresa = new Empresa();

    // Datos básicos
    empresa.setNombreRazonSocial(request.getNombreRazonSocial());
    empresa.setNombreComercial(request.getNombreComercial());
    empresa.setTipoIdentificacion(request.getTipoIdentificacion());
    empresa.setIdentificacion(request.getIdentificacion());

    // Contacto
    empresa.setEmail(request.getEmail());
    empresa.setEmailNotificacion(request.getEmailNotificacion());
    empresa.setTelefono(request.getTelefono());
    empresa.setFax(request.getFax());

    // Ubicación
    if (request.getProvinciaId() != null) {
      empresa.setProvincia(
          ubicacionService.buscarProvinciaPorId(request.getProvinciaId()).orElse(null));
    }
    if (request.getCantonId() != null) {
      empresa.setCanton(ubicacionService.buscarCantonPorId(request.getCantonId()).orElse(null));
    }
    if (request.getDistritoId() != null) {
      empresa.setDistrito(
          ubicacionService.buscarDistritoPorId(request.getDistritoId()).orElse(null));
    }
    if (request.getBarrioId() != null) {
      empresa.setBarrio(ubicacionService.buscarBarrioPorId(request.getBarrioId()).orElse(null));
    }
    empresa.setOtrasSenas(request.getOtrasSenas());

    // Configuración
    empresa.setActiva(request.getActiva());
    empresa.setRequiereHacienda(request.getRequiereHacienda());
    empresa.setRegimenTributario(request.getRegimenTributario());

    // Logo
    empresa.setLogoUrl(logoUrl);

    return empresaRepository.save(empresa);
  }

  private void crearConfiguracionHacienda(Empresa empresa,
      CrearEmpresaCompletaRequest.ConfigHaciendaData configData, String certificadoUrl,
      LocalDate fechaVencimiento) {

    ConfigHaciendaRequest configRequest = new ConfigHaciendaRequest();
    configRequest.setEmpresaId(empresa.getId());
    configRequest.setAmbiente(configData.getAmbiente());
    configRequest.setUsuarioHacienda(configData.getUsuarioHacienda());
    configRequest.setClaveHacienda(configData.getClaveHacienda());
    configRequest.setUrlCertificadoKey(certificadoUrl);
    configRequest.setPinCertificado(configData.getPinCertificado());
    configRequest.setNotaFactura(configData.getNotaFactura());
    configRequest.setNotaValidezProforma(configData.getNotaValidezProforma());
    configRequest.setFechaVencimientoCertificado(fechaVencimiento);
    configRequest.setDetalleFactura1(configData.getDetalleFactura1());
    configRequest.setDetalleFactura2(configData.getDetalleFactura2());

    configHaciendaService.crearOActualizar(configRequest);
  }

  private void crearActividadesEconomicas(Empresa empresa,
      List<ActividadEconomicaRequest> actividadesRequest) {

    List<EmpresaActividad> actividades = new ArrayList<>();

    for (int i = 0; i < actividadesRequest.size(); i++) {
      ActividadEconomicaRequest actRequest = actividadesRequest.get(i);

      // Buscar la actividad económica
      ActividadEconomica actividad = actividadEconomicaRepository
          .findByCodigo(actRequest.getCodigo())
          .orElseThrow(() -> new RuntimeException(
              "Actividad económica no encontrada: " + actRequest.getCodigo()));

      // Crear relación empresa-actividad
      EmpresaActividad empresaActividad = new EmpresaActividad();
      empresaActividad.setEmpresa(empresa);
      empresaActividad.setActividad(actividad);
      empresaActividad.setEsPrincipal(i == 0); // La primera es principal
      empresaActividad.setOrden(i);

      actividades.add(empresaActividad);
    }

    empresaActividadRepository.saveAll(actividades);
  }
}