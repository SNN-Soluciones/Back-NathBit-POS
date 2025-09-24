package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Controlador simple para testing de impresión
 * Sin dependencias de ESC/POS para verificar el flujo
 */
@Slf4j
@RestController
@RequestMapping("/api/test/impresion")
@Tag(name = "Test Impresión", description = "Endpoints de prueba para impresión térmica")
public class TestImpresionController {

    @Operation(summary = "Test básico de impresión",
        description = "Genera un ticket de prueba simple sin comandos ESC/POS")
    @GetMapping("/basico")
    public ResponseEntity<?> testBasico() {
        try {
            // Crear un ticket de prueba simple
            StringBuilder ticket = new StringBuilder();

            // Encabezado
            ticket.append("================================\n");
            ticket.append("      NATHBIT POS - TEST\n");
            ticket.append("================================\n");
            ticket.append("\n");

            // Información
            ticket.append("Fecha: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
            ticket.append("Terminal: TEST-001\n");
            ticket.append("Cajero: Usuario Prueba\n");
            ticket.append("\n");

            // Líneas de prueba
            ticket.append("--------------------------------\n");
            ticket.append("PRUEBA DE IMPRESION\n");
            ticket.append("--------------------------------\n");
            ticket.append("1234567890123456789012345678901234567890\n");
            ticket.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ\n");
            ticket.append("abcdefghijklmnopqrstuvwxyz\n");
            ticket.append("Caracteres especiales: ñ á é í ó ú\n");
            ticket.append("Simbolos: $ % & @ # * + - = \n");
            ticket.append("\n");

            // Pie
            ticket.append("================================\n");
            ticket.append("Si puede leer esto correctamente\n");
            ticket.append("la impresora funciona bien!\n");
            ticket.append("================================\n");
            ticket.append("\n\n\n\n\n"); // Feed para corte

            // Convertir a bytes UTF-8
            byte[] ticketBytes = ticket.toString().getBytes(StandardCharsets.UTF_8);

            // Codificar en Base64
            String base64 = Base64.getEncoder().encodeToString(ticketBytes);

            // Respuesta
            Map<String, Object> response = Map.of(
                "success", true,
                "mensaje", "Test generado correctamente",
                "comandos", base64,
                "formato", "TEXT/PLAIN",
                "encoding", "UTF-8",
                "longitudBytes", ticketBytes.length,
                "preview", ticket.toString()
            );

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Test básico generado", response));

        } catch (Exception e) {
            log.error("Error en test básico: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    @Operation(summary = "Test con formato simple",
        description = "Genera un ticket con formato básico pero estructurado")
    @GetMapping("/formato-simple")
    public ResponseEntity<?> testFormatoSimple() {
        try {
            StringBuilder ticket = new StringBuilder();

            // Simulamos un tiquete real pero sin ESC/POS
            ticket.append(centrar("EMPRESA DE PRUEBA S.A.", 40)).append("\n");
            ticket.append(centrar("Ced Juridica: 3-101-123456", 40)).append("\n");
            ticket.append(centrar("Tel: 2222-3333", 40)).append("\n");
            ticket.append(centrar("email@ejemplo.com", 40)).append("\n");
            ticket.append(repetir("=", 40)).append("\n");
            ticket.append("\n");

            ticket.append("TIQUETE INTERNO\n");
            ticket.append("No: 001-00001-TI-0000000001\n");
            ticket.append("Fecha: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            ticket.append(repetir("-", 40)).append("\n");

            // Detalles
            ticket.append("CANT  DESCRIPCION         P.UNIT  TOTAL\n");
            ticket.append(repetir("-", 40)).append("\n");

            // Producto ejemplo
            ticket.append(formatearLinea("2", "Cafe Americano", "2500", "5000")).append("\n");
            ticket.append(formatearLinea("1", "Sandwich Pollo", "3500", "3500")).append("\n");
            ticket.append(formatearLinea("3", "Refresco Natural", "1800", "5400")).append("\n");

            ticket.append(repetir("-", 40)).append("\n");

            // Totales
            ticket.append(formatearTotal("Subtotal:", "13,900")).append("\n");
            ticket.append(formatearTotal("IVA 13%:", "1,807")).append("\n");
            ticket.append(repetir("=", 40)).append("\n");
            ticket.append(formatearTotal("TOTAL:", "15,707")).append("\n");
            ticket.append(repetir("=", 40)).append("\n");

            // Pago
            ticket.append("\n");
            ticket.append("Forma de Pago: EFECTIVO\n");
            ticket.append("Recibido: 20,000\n");
            ticket.append("Vuelto: 4,293\n");
            ticket.append("\n");

            // Pie
            ticket.append(repetir("-", 40)).append("\n");
            ticket.append(centrar("GRACIAS POR SU COMPRA", 40)).append("\n");
            ticket.append(centrar("NathBit POS v1.0", 40)).append("\n");
            ticket.append("\n\n\n\n\n");

            // Convertir a Base64
            byte[] bytes = ticket.toString().getBytes(StandardCharsets.UTF_8);
            String base64 = Base64.getEncoder().encodeToString(bytes);

            Map<String, Object> response = Map.of(
                "success", true,
                "mensaje", "Tiquete de prueba generado",
                "comandos", base64,
                "formato", "TEXT/PLAIN",
                "encoding", "UTF-8",
                "preview", ticket.toString()
            );

            return ResponseEntity.ok()
                .body(ApiResponse.success("Test con formato generado", response));

        } catch (Exception e) {
            log.error("Error en test con formato: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    // Métodos auxiliares para formateo
    private String centrar(String texto, int ancho) {
        if (texto.length() >= ancho) return texto;
        int espacios = (ancho - texto.length()) / 2;
        return " ".repeat(espacios) + texto;
    }

    private String repetir(String caracter, int veces) {
        return caracter.repeat(veces);
    }

    private String formatearLinea(String cant, String desc, String pUnit, String total) {
        // Formato: "CANT  DESCRIPCION         P.UNIT  TOTAL"
        //          "2     Cafe Americano      2500    5000"
        return String.format("%-5s %-19s %6s %7s",
            cant,
            desc.length() > 19 ? desc.substring(0, 19) : desc,
            pUnit,
            total
        );
    }

    private String formatearTotal(String etiqueta, String valor) {
        int espacios = 40 - etiqueta.length() - valor.length();
        return etiqueta + " ".repeat(Math.max(espacios, 1)) + valor;
    }
}