package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Generador de rutas estandarizadas para almacenamiento en S3
 * <p>
 * Formato: EMPRESA-NOMBRE-COMERCIAL/TIPO-FACTURA/2025/AGOSTO/{clave}-tipo.xml
 */
@Component
public class S3PathBuilder {

  private static final Locale LOCALE_ES = new Locale("es", "CR");
  private static final String S3_PATH_PREFIX = "NathBit-POS";

  /**
   * Genera la ruta completa para un archivo XML en S3
   *
   * @param factura     La factura
   * @param tipoArchivo Tipo de archivo (sin-firma, firmado, respuesta)
   * @return Ruta completa en S3
   */
  public String buildXmlPath(Factura factura, TipoArchivoS3 tipoArchivo) {
    String empresaNombre = normalizeEmpresaName(factura);
    String tipoFactura = normalizeTipoDocumento(factura.getTipoDocumento());

    LocalDateTime ahora = LocalDateTime.now();
    String year = String.valueOf(ahora.getYear());
    String month = ahora.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES).toUpperCase();

    String filename = String.format("%s-%s.xml",
        factura.getClave(),
        tipoArchivo.getSuffix()
    );

    return String.format("%s/%s/%s/%s/%s/%s",
        S3_PATH_PREFIX,
        empresaNombre,
        tipoFactura,
        year,
        month,
        filename
    );
  }

  /**
   * Genera la ruta para un PDF
   */
  public String buildPdfPath(Factura factura) {
    String empresaNombre = normalizeEmpresaName(factura);
    String tipoFactura = normalizeTipoDocumento(factura.getTipoDocumento());

    LocalDateTime ahora = LocalDateTime.now();
    String year = String.valueOf(ahora.getYear());
    String month = ahora.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES).toUpperCase();

    String filename = String.format("%s.pdf", factura.getClave());

    return String.format("%s/%s/%s/%s/%s/PDF/%s",
        S3_PATH_PREFIX,
        empresaNombre,
        tipoFactura,
        year,
        month,
        filename
    );
  }

  /**
   * Normaliza el nombre de la empresa para usar en rutas - Convierte a mayúsculas - Reemplaza
   * espacios por guiones - Elimina caracteres especiales
   */
  private String normalizeEmpresaName(Factura factura) {
    String nombre = factura.getSucursal().getEmpresa().getNombreComercial();

    // Si no hay nombre comercial, usar razón social
    if (nombre == null || nombre.trim().isEmpty()) {
      nombre = factura.getSucursal().getEmpresa().getNombreRazonSocial();
    }

    return nombre
        .toUpperCase()
        .replaceAll("[^A-Z0-9\\s]", "") // Eliminar caracteres especiales
        .trim()
        .replaceAll("\\s+", "_"); // Espacios a guiones
  }

  /**
   * Normaliza el tipo de documento
   */
  private String normalizeTipoDocumento(TipoDocumento tipo) {
    return switch (tipo) {
      case FACTURA_ELECTRONICA -> "FACTURA-ELECTRONICA";
      case TIQUETE_ELECTRONICO -> "TIQUETE-ELECTRONICO";
      case NOTA_CREDITO -> "NOTA-CREDITO";
      case NOTA_DEBITO -> "NOTA-DEBITO";
      case FACTURA_COMPRA -> "FACTURA-COMPRA";
      case FACTURA_EXPORTACION -> "FACTURA-EXPORTACION";
      default -> tipo.name().replace("_", "-");
    };
  }

  /**
   * Genera la ruta para el certificado digital de la empresa
   * Formato: NathBit-POS/EMPRESA-NOMBRE/ARCHIVOS/certificado.p12
   */
  public String buildCertificadoPath(String nombreEmpresa) {
    String empresaNormalizada = normalizeCompanyName(nombreEmpresa);
    String filename = "Certificado".concat(".p12");
    return String.format("%s/%s/ARCHIVOS/%s", S3_PATH_PREFIX, empresaNormalizada, filename);
  }

  /**
   * Genera la ruta para el logo de la empresa
   * Formato: NathBit-POS/EMPRESA-NOMBRE/ARCHIVOS/logo.{extension}
   */
  public String buildLogoPath(String nombreEmpresa, String extension) {
    String empresaNormalizada = normalizeCompanyName(nombreEmpresa);
    String ext = extension.startsWith(".") ? extension.substring(1) : extension;
    String filename = String.format("logo.%s", ext);
    return String.format("%s/%s/ARCHIVOS/%s", S3_PATH_PREFIX, empresaNormalizada, filename);
  }


  /**
   * Genera la ruta para otros archivos de la empresa
   * Formato: NathBit-POS/EMPRESA-NOMBRE/ARCHIVOS/{filename}
   */
  public String buildArchivoPath(String nombreEmpresa, String filename) {
    String empresaNormalizada = normalizeCompanyName(nombreEmpresa);
    return String.format("%s/%s/ARCHIVOS/%s", S3_PATH_PREFIX, empresaNormalizada, filename);
  }

  /**
   * Normaliza el nombre de empresa (método auxiliar para uso directo)
   */
  public String normalizeCompanyName(String nombre) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException("El nombre de la empresa no puede estar vacío");
    }

    return nombre
        .toUpperCase()
        .replaceAll("[^A-Z0-9\\s]", "") // Eliminar caracteres especiales
        .trim()
        .replaceAll("\\s+", "_"); // CAMBIO: Espacios a guiones bajos en vez de guiones
  }

  /**
   * Enum para tipos de archivo
   */
  public enum TipoArchivoS3 {
    SIN_FIRMA("sin-firma"),
    FIRMADO("firmado"),
    RESPUESTA("respuesta");

    private final String suffix;

    TipoArchivoS3(String suffix) {
      this.suffix = suffix;
    }

    public String getSuffix() {
      return suffix;
    }
  }
}