package com.snnsoluciones.backnathbitpos.util;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.facturacion.TipoArchivoFactura;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumento;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Generador de rutas estandarizadas para almacenamiento en S3
 *
 * IMPORTANTE: Este es el ÚNICO lugar donde se debe normalizar nombres de empresa
 * para garantizar consistencia en toda la aplicación.
 */
@Component
public class S3PathBuilder {

  private static final Locale LOCALE_ES = new Locale("es", "CR");
  private static final String S3_PATH_PREFIX = "NathBit-POS";

  /**
   * Genera la ruta completa para un archivo XML en S3
   */
  public String buildXmlPath(Factura factura, TipoArchivoFactura tipoArchivo, String empresaNombre) {
    if (empresaNombre == null || empresaNombre.trim().isEmpty()) {
      empresaNombre = factura.getSucursal().getEmpresa().getNombreComercial();
      if (empresaNombre == null || empresaNombre.trim().isEmpty()) {
        empresaNombre = factura.getSucursal().getEmpresa().getNombreRazonSocial();
      }
    }

    String empresaNormalizada = normalizeCompanyName(empresaNombre);
    String tipoFactura = normalizeTipoDocumento(factura.getTipoDocumento());

    LocalDateTime ahora = LocalDateTime.now();
    String year = String.valueOf(ahora.getYear());
    String month = ahora.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES).toUpperCase();

    String filename = String.format("%s-%s.xml",
        factura.getClave(),
        tipoArchivo.name()
    );

    return String.format("%s/empresas/%s/facturas/%s/%s/%s/%s",
        S3_PATH_PREFIX,
        empresaNormalizada,
        tipoFactura,
        year,
        month,
        filename
    );
  }

  /**
   * Genera la ruta completa para un archivo XML de Mensaje Receptor en S3
   * Formato:
   * NathBit-POS/empresas/{empresa}/facturas/mensaje-receptor/{YYYY}/{MMMM}/{clave}-{tag}.xml
   */
  public String buildXmlPathMR(String claveComprobante, String empresaNombre, String tagArchivo) {
    if (empresaNombre == null || empresaNombre.trim().isEmpty()) {
      throw new IllegalArgumentException("El nombre de la empresa no puede estar vacío");
    }

    String empresaNormalizada = normalizeCompanyName(empresaNombre);

    LocalDateTime ahora = LocalDateTime.now();
    String year = String.valueOf(ahora.getYear());
    String month = ahora.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES).toUpperCase();

    String tag = (tagArchivo == null || tagArchivo.trim().isEmpty()) ? "mr" : tagArchivo.trim();
    String filename = String.format("%s-%s.xml", claveComprobante, tag);

    return String.format("%s/empresas/%s/facturas/mensaje-receptor/%s/%s/%s",
        S3_PATH_PREFIX,
        empresaNormalizada,
        year,
        month,
        filename
    );
  }

  /**
   * Overload conveniente cuando ya tienes la Empresa.
   */
  public String buildXmlPathMR(String claveComprobante, Empresa empresa, String tagArchivo) {
    String empresaNombre = obtenerNombreEmpresa(empresa);
    return buildXmlPathMR(claveComprobante, empresaNombre, tagArchivo);
  }

  /**
   * Genera la ruta para un PDF
   */
  public String buildPdfPath(Factura factura) {
    String empresaNombre = obtenerNombreEmpresa(factura.getSucursal().getEmpresa());
    String empresaNormalizada = normalizeCompanyName(empresaNombre);
    String tipoFactura = normalizeTipoDocumento(factura.getTipoDocumento());

    LocalDateTime ahora = LocalDateTime.now();
    String year = String.valueOf(ahora.getYear());
    String month = ahora.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES).toUpperCase();

    String filename = String.format("%s.pdf", factura.getClave());

    return String.format("%s/empresas/%s/facturas/%s/%s/%s/PDF/%s",
        S3_PATH_PREFIX,
        empresaNormalizada,
        tipoFactura,
        year,
        month,
        filename
    );
  }

  /**
   * Normaliza el tipo de documento
   */
  public String normalizeTipoDocumento(TipoDocumento tipo) {
    return switch (tipo) {
      case FACTURA_ELECTRONICA -> "factura-electronica";
      case TIQUETE_ELECTRONICO -> "tiquete-electronico";
      case NOTA_CREDITO -> "nota-credito";
      case NOTA_DEBITO -> "nota-debito";
      case FACTURA_COMPRA -> "factura-compra";
      case FACTURA_EXPORTACION -> "factura-exportacion";
      default -> tipo.name().toLowerCase().replace("_", "-");
    };
  }

  /**
   * Genera la ruta para el certificado digital de la empresa
   * Formato: NathBit-POS/empresas/{empresa}/certificados/{filename}
   */
  public String buildCertificadoPath(String nombreEmpresa, String filename) {
    String empresaNormalizada = normalizeCompanyName(nombreEmpresa);
    return String.format("%s/empresas/%s/certificados/%s",
        S3_PATH_PREFIX,
        empresaNormalizada,
        filename);
  }

  /**
   * Genera la ruta para el logo de la empresa
   * Formato: NathBit-POS/empresas/{empresa}/logos/logo.{extension}
   */
  public String buildLogoPath(String nombreEmpresa, String extension) {
    String empresaNormalizada = normalizeCompanyName(nombreEmpresa);
    String ext = extension.startsWith(".") ? extension.substring(1) : extension;
    String filename = String.format("logo.%s", ext.toLowerCase());
    return String.format("%s/empresas/%s/logos/%s",
        S3_PATH_PREFIX,
        empresaNormalizada,
        filename);
  }

  /**
   * Genera la ruta para otros archivos de la empresa
   * Formato: NathBit-POS/empresas/{empresa}/archivos/{filename}
   */
  public String buildArchivoPath(String nombreEmpresa, String filename) {
    String empresaNormalizada = normalizeCompanyName(nombreEmpresa);
    return String.format("%s/empresas/%s/archivos/%s",
        S3_PATH_PREFIX,
        empresaNormalizada,
        filename);
  }

  /**
   * MÉTODO CENTRALIZADO DE NORMALIZACIÓN
   * Este es el ÚNICO método que debe usarse en toda la aplicación
   * para normalizar nombres de empresa.
   *
   * Reglas:
   * 1. Convertir a minúsculas (más estándar en S3)
   * 2. Eliminar acentos y caracteres especiales
   * 3. Reemplazar espacios por guiones bajos
   * 4. Eliminar caracteres no ASCII
   * 5. Limitar longitud a 50 caracteres
   */
  public String normalizeCompanyName(String nombre) {
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException("El nombre de la empresa no puede estar vacío");
    }

    String normalizado = nombre
        .toLowerCase()                          // Minúsculas para consistencia
        .replaceAll("[áàäâ]", "a")             // Reemplazar acentos
        .replaceAll("[éèëê]", "e")
        .replaceAll("[íìïî]", "i")
        .replaceAll("[óòöô]", "o")
        .replaceAll("[úùüû]", "u")
        .replaceAll("ñ", "n")
        .replaceAll("[^a-z0-9\\s]", "")        // Solo letras, números y espacios
        .trim()
        .replaceAll("\\s+", "_");              // Espacios por guiones bajos

    // Limitar longitud
    if (normalizado.length() > 50) {
      normalizado = normalizado.substring(0, 50);
    }

    // Validar que no quede vacío después de la normalización
    if (normalizado.isEmpty()) {
      throw new IllegalArgumentException("El nombre normalizado no puede quedar vacío");
    }

    return normalizado;
  }

  /**
   * Método auxiliar para obtener el nombre de la empresa
   * Prioriza nombre comercial sobre razón social
   */
  private String obtenerNombreEmpresa(Empresa empresa) {
    if (empresa.getNombreComercial() != null && !empresa.getNombreComercial().trim().isEmpty()) {
      return empresa.getNombreComercial();
    }
    return empresa.getNombreRazonSocial();
  }
}