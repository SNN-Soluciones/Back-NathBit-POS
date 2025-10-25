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

  // src/main/java/com/snnsoluciones/backnathbitpos/service/impl/EmpresaCreacionServiceImpl.java
  @Override
  @Transactional
  public CrearEmpresaCompletaResponse actualizarEmpresaCompleta(
      Long empresaId,
      CrearEmpresaCompletaRequest request,
      MultipartFile logo,
      MultipartFile certificado,
      String usuarioEmail
  ) {
    log.info("=== INICIANDO ACTUALIZACIÓN COMPLETA DE EMPRESA [{}] ===", empresaId);

    try {
      // 0) Cargar empresa existente
      Empresa empresa = empresaRepository.findById(empresaId)
          .orElseThrow(() -> new RuntimeException("Empresa no encontrada: " + empresaId));

      // 1) Validaciones previas (exclusión por ID para uniqueness)
      log.info("1. Validando datos de la empresa (actualización)...");
      validarDatosEmpresaActualizacion(request, empresaId);

      // 2) Determinar nombre base para archivos (para rutas S3)
      String nombreEmpresaParaArchivos = (request.getNombreComercial() != null && !request.getNombreComercial().isBlank())
          ? request.getNombreComercial()
          : (request.getNombreRazonSocial() != null && !request.getNombreRazonSocial().isBlank())
              ? request.getNombreRazonSocial()
              : (empresa.getNombreComercial() != null ? empresa.getNombreComercial() : empresa.getNombreRazonSocial());

      // 3) CERTIFICADO (opcional pero obligatorio si requiere Hacienda y no existe config previa)
      String certificadoUrl = null;
      LocalDate fechaVencimientoCert = null;

      boolean requiereHacienda = (request.getRequiereHacienda() != null)
          ? request.getRequiereHacienda()
          : Boolean.TRUE.equals(empresa.getRequiereHacienda());

      if (requiereHacienda) {
        log.info("3. Validando/actualizando certificado (si aplica)...");
        // Situaciones:
        //  A) Empresa ya tenía ConfigHacienda y no suben nuevo certificado -> mantener
        //  B) Suben un certificado nuevo -> validar/extraer/guardar y actualizar
        //  C) No existía ConfigHacienda y se requiere Hacienda -> certificado obligatorio

        boolean yaTieneConfig = empresa.getConfigHacienda() != null;
        boolean vieneCertNuevo = certificado != null && !certificado.isEmpty();

        // Necesitamos PIN para validar si hay certificado nuevo o si no había config previa
        String pin = (request.getConfigHacienda() != null) ? request.getConfigHacienda().getPinCertificado() : null;

        if (!yaTieneConfig && !vieneCertNuevo) {
          throw new RuntimeException("Se requiere certificado (.p12) para habilitar Hacienda por primera vez");
        }
        if (vieneCertNuevo && (pin == null || pin.isBlank())) {
          throw new RuntimeException("El PIN del certificado es requerido para validar el nuevo certificado");
        }

        if (vieneCertNuevo) {
          LocalDate fechaVenc = certificadoService.extraerFechaVencimiento(certificado, pin);
          if (fechaVenc == null) throw new RuntimeException("No se pudo extraer la fecha de vencimiento del certificado");
          if (fechaVenc.isBefore(LocalDate.now())) throw new RuntimeException("El certificado está vencido");
          if (fechaVenc.isBefore(LocalDate.now().plusDays(30))) {
            log.warn("⚠️ ADVERTENCIA: El certificado vence pronto: {}", fechaVenc);
          }
          X509Certificate x509 = certificadoService.extraerInformacionCertificado(certificado, pin);
          if (x509 != null) {
            log.info("   ✅ Certificado válido para: {}", x509.getSubjectDN().getName());
            log.info("   ✅ Emitido por: {}", x509.getIssuerDN().getName());
            log.info("   ✅ Válido hasta: {}", fechaVenc);
          }
          // Subir y registrar
          certificadoUrl = subirCertificado(certificado, nombreEmpresaParaArchivos);
          fechaVencimientoCert = fechaVenc;
        } else {
          // Mantener lo que ya existe
          if (yaTieneConfig) {
            certificadoUrl = empresa.getConfigHacienda().getUrlCertificadoKey();
            fechaVencimientoCert = empresa.getConfigHacienda().getFechaVencimientoCertificado();
          }
        }
      } else {
        log.info("3. Empresa NO requiere Hacienda (se mantendrá/ajustará estado)");
      }

      // 4) LOGO (opcional)
      String nuevoLogoUrl = null;
      if (logo != null && !logo.isEmpty()) {
        log.info("4. Subiendo nuevo logo...");
        nuevoLogoUrl = subirLogo(logo, nombreEmpresaParaArchivos);
        log.info("   ✅ Logo actualizado");
      }

      // 5) Actualizar entidad Empresa (con valores parciales o totales)
      log.info("5. Actualizando datos principales de empresa...");
      actualizarDatosEmpresaDesdeRequest(empresa, request, nuevoLogoUrl);

      // 6) Configuración Hacienda (crear/actualizar/omitir según corresponda)
      if (requiereHacienda) {
        log.info("6. Creando/actualizando configuración de Hacienda...");
        CrearEmpresaCompletaRequest.ConfigHaciendaData cfg = request.getConfigHacienda();
        // Si no viene DTO, pero requiere Hacienda, permitir mantener lo existente
        if (cfg == null && empresa.getConfigHacienda() == null) {
          throw new RuntimeException("La configuración de Hacienda es requerida para habilitarla");
        }

        // Armar request para servicio (merge entre lo entrante y lo ya existente)
        ConfigHaciendaRequest configReq = new ConfigHaciendaRequest();
        configReq.setEmpresaId(empresa.getId());
        configReq.setAmbiente(cfg != null && cfg.getAmbiente() != null ? cfg.getAmbiente()
            : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getAmbiente() : null));
        configReq.setUsuarioHacienda(cfg != null && cfg.getUsuarioHacienda() != null ? cfg.getUsuarioHacienda()
            : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getUsuarioHacienda() : null));
        configReq.setClaveHacienda(cfg != null && cfg.getClaveHacienda() != null ? cfg.getClaveHacienda()
            : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getClaveHacienda() : null));

        // Certificado/fecha/pin
        configReq.setUrlCertificadoKey(
            certificadoUrl != null ? certificadoUrl :
                (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getUrlCertificadoKey() : null)
        );
        if (cfg != null && cfg.getPinCertificado() != null) {
          configReq.setPinCertificado(cfg.getPinCertificado());
        } else if (empresa.getConfigHacienda() != null) {
          configReq.setPinCertificado(empresa.getConfigHacienda().getPinCertificado());
        }
        configReq.setFechaVencimientoCertificado(
            fechaVencimientoCert != null ? fechaVencimientoCert :
                (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getFechaVencimientoCertificado() : null)
        );

        // Notas y detalles de factura (merge)
        configReq.setNotaFactura(cfg != null ? cfg.getNotaFactura() : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getNotaFactura() : null));
        configReq.setNotaValidezProforma(cfg != null ? cfg.getNotaValidezProforma() : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getNotaValidezProforma() : null));
        configReq.setDetalleFactura1(cfg != null ? cfg.getDetalleFactura1() : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getDetalleFactura1() : null));
        configReq.setDetalleFactura2(cfg != null ? cfg.getDetalleFactura2() : (empresa.getConfigHacienda() != null ? empresa.getConfigHacienda().getDetalleFactura2() : null));

        // Persistir (el servicio interno decide create/update)
        configHaciendaService.crearOActualizar(configReq);

        // 6.1) Actividades económicas (si el request trae lista, reemplazamos ordenadamente)
        if (cfg != null && cfg.getActividades() != null) {
          log.info("6.1 Reemplazando actividades económicas...");
          if (cfg.getActividades().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos una actividad económica");
          }
          // Validar existencias
          for (ActividadEconomicaRequest act : request.getConfigHacienda().getActividades()) {
            if (act.getCodigo() == null || act.getCodigo().isBlank()) {
              throw new RuntimeException("Cada actividad debe tener un código válido");
            }
            if (act.getDescripcion() == null || act.getDescripcion().isBlank()) {
              throw new RuntimeException("Cada actividad debe tener una descripción válida");
            }
          }

          // Borrar actuales y crear nuevas (ajusta a tu repo: deleteByEmpresaId / deleteByEmpresa / etc.)
          try {
            empresaActividadRepository.deleteByEmpresaId(empresa.getId());
          } catch (Exception ignore) {
            // Si tu repo no tiene este método, usa el que corresponda en tu proyecto:
            // empresaActividadRepository.deleteByEmpresa(empresa);
            // o bien carga y deleteAll(...)
          }

          crearActividadesEconomicas(empresa, cfg.getActividades());
          log.info("   ✅ Actividades económicas actualizadas");
        }
      } else {
        // Si NO requiere Hacienda ahora, puedes decidir deshabilitar/limpiar config (opcional):
        log.info("6. Configuración Hacienda: empresa no requiere Hacienda. Se mantiene configuración existente sin uso (no se borra).");
        // Si prefieres limpiar, aquí podrías: empresa.setConfigHacienda(null);
        // y/o borrar actividades; depende de tu regla de negocio.
      }

      // 7) Guardar empresa actualizada
      empresa = empresaRepository.save(empresa);

      // 8) (Opcional) Envío de correo de confirmación de actualización
      try {
        String emailDestino = empresa.getEmailNotificacion() != null ? empresa.getEmailNotificacion() : empresa.getEmail();
        emailService.enviarConfirmacionEmpresaCreada(empresa, emailDestino); // puedes tener un método específico para "actualizada"
      } catch (Exception ex) {
        log.warn("No se pudo enviar correo de confirmación de actualización: {}", ex.getMessage());
      }

      // 9) Respuesta
      log.info("=== EMPRESA ACTUALIZADA EXITOSAMENTE [{}] ===", empresa.getId());
      CrearEmpresaCompletaResponse resp = CrearEmpresaCompletaResponse.builder()
          .empresaId(empresa.getId())
          .mensaje("Empresa actualizada exitosamente")
          .nombreComercial(empresa.getNombreComercial())
          .identificacion(empresa.getIdentificacion())
          .requiereHacienda(empresa.getRequiereHacienda())
          .build();

      if (empresa.getRequiereHacienda() && empresa.getConfigHacienda() != null) {
        resp.setConfigHacienda(empresaConfigHaciendaMapper.toDto(empresa.getConfigHacienda()));
      }
      return resp;

    } catch (RuntimeException e) {
      log.error("❌ Error de negocio al actualizar: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌ Error inesperado actualizando empresa", e);
      throw new RuntimeException("Error al actualizar la empresa: " + e.getMessage(), e);
    }
  }

  /* ======================= HELPERS DE ACTUALIZACIÓN ======================= */

  private void validarDatosEmpresaActualizacion(CrearEmpresaCompletaRequest request, Long empresaId) {
    // Uniqueness de identificación (si viene en el request y cambió)
    if (request.getIdentificacion() != null && !request.getIdentificacion().isBlank()) {
      boolean usadoPorOtro = empresaRepository.existsByIdentificacion(request.getIdentificacion())
          && empresaRepository.findByIdentificacion(request.getIdentificacion())
          .map(e -> !e.getId().equals(empresaId))
          .orElse(false);
      if (usadoPorOtro) {
        throw new RuntimeException("Ya existe una empresa con la identificación: " + request.getIdentificacion());
      }
    }

    // Uniqueness de email (si viene y cambió)
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      boolean usadoPorOtro = empresaRepository.existsByEmail(request.getEmail())
          && empresaRepository.findByEmail(request.getEmail())
          .map(e -> !e.getId().equals(empresaId))
          .orElse(false);
      if (usadoPorOtro) {
        throw new RuntimeException("Ya existe una empresa con el email: " + request.getEmail());
      }
    }

    // Si en el request se marca requiereHacienda=true, validar estructura mínima
    if (Boolean.TRUE.equals(request.getRequiereHacienda())) {
      if (request.getConfigHacienda() == null) {
        // Permitimos que sea null si la empresa YA tenía config; esto se resuelve más adelante
        log.info("Validación: requiere Hacienda pero no llegó ConfigHacienda en request; se intentará mantener la existente.");
      } else {
        // Si llega lista de actividades, debe tener al menos una y existir
        if (request.getConfigHacienda().getActividades() != null) {
          if (request.getConfigHacienda().getActividades().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos una actividad económica");
          }
          for (ActividadEconomicaRequest act : request.getConfigHacienda().getActividades()) {
            if (act.getCodigo() == null || act.getCodigo().isBlank()) {
              throw new RuntimeException("Cada actividad debe tener un código válido");
            }
            if (act.getDescripcion() == null || act.getDescripcion().isBlank()) {
              throw new RuntimeException("Cada actividad debe tener una descripción válida");
            }
          }
        }
      }
    }
  }

  /**
   * Aplica valores del request a la entidad, con merge seguro (no pisa con nulls salvo que explícitamente quieras limpiar).
   */
  private void actualizarDatosEmpresaDesdeRequest(Empresa empresa,
      CrearEmpresaCompletaRequest req,
      String nuevoLogoUrl) {
    // Datos básicos
    if (req.getNombreRazonSocial() != null) empresa.setNombreRazonSocial(req.getNombreRazonSocial());
    if (req.getNombreComercial() != null) empresa.setNombreComercial(req.getNombreComercial());
    if (req.getTipoIdentificacion() != null) empresa.setTipoIdentificacion(req.getTipoIdentificacion());
    if (req.getIdentificacion() != null) empresa.setIdentificacion(req.getIdentificacion());

    // Contacto
    if (req.getEmail() != null) empresa.setEmail(req.getEmail());
    if (req.getEmailNotificacion() != null) empresa.setEmailNotificacion(req.getEmailNotificacion());
    if (req.getTelefono() != null) empresa.setTelefono(req.getTelefono());
    if (req.getFax() != null) empresa.setFax(req.getFax());

    // Ubicación (si viene null explícito y quieres limpiar, déjalo así; si no, omite el set cuando venga null)
    if (req.getProvinciaId() != null) {
      empresa.setProvincia(ubicacionService.buscarProvinciaPorId(req.getProvinciaId()).orElse(null));
    }
    if (req.getCantonId() != null) {
      empresa.setCanton(ubicacionService.buscarCantonPorId(req.getCantonId()).orElse(null));
    }
    if (req.getDistritoId() != null) {
      empresa.setDistrito(ubicacionService.buscarDistritoPorId(req.getDistritoId()).orElse(null));
    }
    if (req.getBarrioId() != null) {
      empresa.setBarrio(ubicacionService.buscarBarrioPorId(req.getBarrioId()).orElse(null));
    }
    if (req.getOtrasSenas() != null) empresa.setOtrasSenas(req.getOtrasSenas());

    // Configuración general
    if (req.getActiva() != null) empresa.setActiva(req.getActiva());
    if (req.getRequiereHacienda() != null) empresa.setRequiereHacienda(req.getRequiereHacienda());
    if (req.getRegimenTributario() != null) empresa.setRegimenTributario(req.getRegimenTributario());

    // Logo
    if (nuevoLogoUrl != null) empresa.setLogoUrl(nuevoLogoUrl);
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
        if (act.getCodigo() == null || act.getCodigo().isBlank()) {
          throw new RuntimeException("Cada actividad debe tener un código válido");
        }
        if (act.getDescripcion() == null || act.getDescripcion().isBlank()) {
          throw new RuntimeException("Cada actividad debe tener una descripción válida");
        }
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
      ActividadEconomicaRequest actReq = actividadesRequest.get(i);

      // Crear relación empresa-actividad solo con strings
      EmpresaActividad ea = new EmpresaActividad();
      ea.setEmpresa(empresa);
      ea.setCodigoActividad(actReq.getCodigo());
      ea.setDescripcionActividad(actReq.getDescripcion());
      ea.setEsPrincipal(Boolean.TRUE.equals(actReq.getEsPrincipal()) || i == 0);
      ea.setOrden(i);

      // 🧠 Legacy: ya no seteamos 'actividad'
      // ea.setActividad(...); ← eliminar

      actividades.add(ea);
    }

    empresaActividadRepository.saveAll(actividades);
  }
}