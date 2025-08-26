package com.snnsoluciones.backnathbitpos.service.pdf;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PdfGeneratorService {

    // Cache para guardar reportes compilados
    private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

    public byte[] generarPdf(String plantilla, Map<String, Object> parametros, List<?> datos) {
        try {
            // Obtener reporte (lo compila si no existe)
            JasperReport jasperReport = obtenerReporteCompilado(plantilla);

            // Crear datasource con los datos
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(datos);

            // Llenar el reporte
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                parametros,
                dataSource
            );

            // Exportar a PDF
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    private JasperReport obtenerReporteCompilado(String nombreReporte) throws JRException {
        // Si ya está en cache, devolverlo
        if (reportCache.containsKey(nombreReporte)) {
            log.debug("Usando reporte desde cache: {}", nombreReporte);
            return reportCache.get(nombreReporte);
        }

        // Si no, compilarlo
        String rutaJrxml = "/jasper/" + nombreReporte + ".jrxml";
        InputStream jrxmlStream = getClass().getResourceAsStream(rutaJrxml);

        if (jrxmlStream == null) {
            throw new JRException("No se encontró el archivo: " + rutaJrxml);
        }

        log.info("Compilando reporte por primera vez: {}", nombreReporte);
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

        // Guardarlo en cache para próximas veces
        reportCache.put(nombreReporte, jasperReport);
        log.info("Reporte compilado y guardado en cache: {}", nombreReporte);

        return jasperReport;
    }

    // Método opcional para limpiar cache si necesitas
    public void limpiarCache() {
        reportCache.clear();
        log.info("Cache de reportes limpiado");
    }
}