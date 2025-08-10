package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.dto.documento.GenerarClaveRequest;
import com.snnsoluciones.backnathbitpos.dto.documento.GenerarClaveResponse;
import com.snnsoluciones.backnathbitpos.util.GeneradorClaveUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Generación de claves y consecutivos")
public class DocumentoController {

    @Operation(summary = "Generar clave de 50 dígitos para documento electrónico")
    @PostMapping("/generar-clave")
    public ResponseEntity<ApiResponse<GenerarClaveResponse>> generarClave(
        @Valid @RequestBody GenerarClaveRequest request) {

        try {
            // Generar la clave
            String clave = GeneradorClaveUtil.generarClave(
                request.getFechaEmision(),
                request.getTipoIdentificacion(),
                request.getIdentificacionEmisor(),
                request.getConsecutivo(),
                request.getSituacion(),
                request.getDocumentoId()
            );

            // Generar código de seguridad para respuesta
            String codigoSeguridad = GeneradorClaveUtil.generarCodigoSeguridad(request.getDocumentoId());

            // Descomponer para debugging
            var descompuesta = GeneradorClaveUtil.descomponerClave(clave);

            GenerarClaveResponse response = GenerarClaveResponse.builder()
                .clave(clave)
                .codigoSeguridad(codigoSeguridad)
                .pais(descompuesta.getPais())
                .fecha(descompuesta.getFecha())
                .identificacion(descompuesta.getIdentificacion())
                .consecutivo(descompuesta.getConsecutivo())
                .situacion(descompuesta.getSituacion())
                .fechaGeneracion(LocalDateTime.now())
                .mensaje("Clave generada exitosamente")
                .build();

            return ResponseEntity.ok(ApiResponse.ok(response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al generar clave: " + e.getMessage()));
        }
    }

    @Operation(summary = "Validar formato de clave")
    @GetMapping("/validar-clave/{clave}")
    public ResponseEntity<ApiResponse<GeneradorClaveUtil.ClaveDescompuesta>> validarClave(
        @PathVariable String clave) {

        try {
            if (clave.length() != 50) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La clave debe tener exactamente 50 dígitos"));
            }

            var descompuesta = GeneradorClaveUtil.descomponerClave(clave);
            return ResponseEntity.ok(ApiResponse.ok("Clave válida", descompuesta));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Clave inválida: " + e.getMessage()));
        }
    }
}