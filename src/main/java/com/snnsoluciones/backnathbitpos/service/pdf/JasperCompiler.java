package com.snnsoluciones.backnathbitpos.service.pdf;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.FileOutputStream;

@Slf4j
@Component
public class JasperCompiler {

    @PostConstruct
    public void compilarReportes() {
        compilarReporte("factura_electronica");
        compilarReporte("factura_electronica_80mm");
        // Agrega más reportes aquí si tienes
        // compilarReporte("tiquete_electronico");
        // compilarReporte("nota_credito");
    }

    private void compilarReporte(String nombreReporte) {
        try {
            // Opción 1: Compilar y guardar en target/classes (recomendado)
            InputStream jrxmlStream = getClass().getResourceAsStream("/jasper/" + nombreReporte + ".jrxml");

            if (jrxmlStream == null) {
                log.error("❌ No se encontró el archivo: /jasper/{}.jrxml", nombreReporte);
                return;
            }

            // Compilar a JasperReport
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            // Guardar el .jasper compilado
            String outputPath = "target/classes/jasper/" + nombreReporte + ".jasper";
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                JasperCompileManager.writeReportToXmlStream(jasperReport, fos);
            }

            log.info("✅ Reporte {} compilado exitosamente!", nombreReporte);

        } catch (Exception e) {
            log.error("❌ Error compilando reporte {}: ", nombreReporte, e);
        }
    }
}