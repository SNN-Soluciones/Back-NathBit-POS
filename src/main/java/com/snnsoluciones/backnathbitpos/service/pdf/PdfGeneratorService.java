package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PdfGeneratorService {

    // Cache para guardar reportes compilados
    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void precompilarReportes() {
        log.info("========= PRECOMPILANDO REPORTES JASPER =========");

        // Lista actualizada de reportes a precompilar
        String[] reportes = {
            "factura_electronica",
            "detalle_factura",
            "otros_cargos",
            "subreport_exoneraciones",

            "factura_electronica_80mm",
            "detalle_factura_80mm",
            "otros_cargos_80mm",
            "exoneraciones_80mm"
        };

        for (String reporte : reportes) {
            try {
                compilarYCachear(reporte);
                log.info("✅ {} - Compilado exitosamente", reporte);
            } catch (Exception e) {
                log.error("❌ {} - Error al compilar: {}", reporte, e.getMessage());
            }
        }

        log.info("========= COMPILACIÓN COMPLETADA =========");
        log.info("📊 Reportes en cache: {}", reportCache.size());
    }


    private void compilarYCachear(String nombreReporte) throws JRException {
        String rutaJrxml = "/jasper/" + nombreReporte + ".jrxml";
        InputStream jrxmlStream = getClass().getResourceAsStream(rutaJrxml);

        if (jrxmlStream == null) {
            throw new JRException("No se encontró el archivo: " + rutaJrxml);
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);
        reportCache.put(nombreReporte, jasperReport);
    }

    public byte[] generarPdf(String plantilla, Map<String, Object> parametros, List<?> datos) {
        try {
            // Remover extensión si viene con ella
            String nombreReporte = plantilla.replace(".jrxml", "").replace("/jasper/", "");

            // Obtener reporte del cache
            JasperReport jasperReport = reportCache.get(nombreReporte);

            if (jasperReport == null) {
                log.warn("Reporte {} no encontrado en cache, compilando ahora...", nombreReporte);
                compilarYCachear(nombreReporte);
                jasperReport = reportCache.get(nombreReporte);
            }

            // Crear datasource
            JRDataSource dataSource;
            if (datos != null && !datos.isEmpty()) {
                dataSource = new JRBeanCollectionDataSource(datos);
            } else {
                dataSource = new JREmptyDataSource();
            }

            // Llenar reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parametros,
                dataSource
            );

            // Exportar a PDF
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage());
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    /**
     * @param nombreReporte Nombre del reporte sin extensión
     * @return JasperReport compilado o null si no existe
     */
    public JasperReport getCompiledReport(String nombreReporte) {
        JasperReport report = reportCache.get(nombreReporte);

        if (report == null) {
            log.warn("Reporte {} no encontrado en cache, intentando compilar...", nombreReporte);
            try {
                compilarYCachear(nombreReporte);
                report = reportCache.get(nombreReporte);
            } catch (Exception e) {
                log.error("Error compilando reporte {}: {}", nombreReporte, e.getMessage());
            }
        }

        return report;
    }

    // Método para verificar el estado del cache
    public void estadoCache() {
        log.info("=== ESTADO DEL CACHE DE REPORTES ===");
        reportCache.forEach((nombre, reporte) -> {
            log.info("📄 {} - Compilado", nombre);
        });
        log.info("Total: {} reportes en cache", reportCache.size());
    }
}