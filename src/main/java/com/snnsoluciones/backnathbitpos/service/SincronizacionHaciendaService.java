package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.FacturaRecepcion;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.HaciendaClient;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.ConsultaEstadoResponse;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaAuthParams;
import com.snnsoluciones.backnathbitpos.integrations.hacienda.dto.HaciendaTokenResponse;
import com.snnsoluciones.backnathbitpos.repository.EmpresaConfigHaciendaRepository;
import com.snnsoluciones.backnathbitpos.repository.FacturaRecepcionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para sincronizar estados de comprobantes con Hacienda
 *
 * Funcionalidades:
 * - Consulta el estado REAL en Hacienda
 * - Actualiza la BD con el estado confirmado
 * - Genera reportes de verificación
 *
 * @author NathBit POS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SincronizacionHaciendaService {

    private final HaciendaClient haciendaClient;
    private final FacturaRecepcionRepository facturaRecepcionRepository;
    private final EmpresaConfigHaciendaRepository empresaConfigRepository;

    /**
     * Verifica el estado de UNA factura en Hacienda
     *
     * @param facturaId ID de la factura a verificar
     * @return DTO con el resultado de la verificación
     */
    @Transactional
    public VerificacionHaciendaDTO verificarFactura(Long facturaId) {
        log.info("🔍 Verificando factura ID: {} en Hacienda", facturaId);

        FacturaRecepcion factura = facturaRecepcionRepository.findById(facturaId)
            .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + facturaId));

        try {
            // 1. Obtener token de Hacienda
            HaciendaTokenResponse tokenResponse = obtenerToken(factura.getEmpresa().getId());
            String accessToken = tokenResponse.getAccessToken();

            // 2. Consultar estado en Hacienda
            ConsultaEstadoResponse estadoHacienda = haciendaClient.getEstado(
                accessToken,
                false, // producción
                factura.getClave()
            );

            // 3. Actualizar BD con el estado real
            String estadoAnterior = factura.getEstadoHacienda();
            factura.setEstadoHacienda(estadoHacienda.getIndEstado());
            factura.setMensajeHacienda(estadoHacienda.getRespuestaXmlBase64()); // ✅ Base64
            facturaRecepcionRepository.save(factura);

            log.info("✅ Factura {} verificada - Estado Hacienda: {}",
                facturaId, estadoHacienda.getIndEstado());

            return VerificacionHaciendaDTO.builder()
                .facturaId(facturaId)
                .clave(factura.getClave())
                .numeroConsecutivo(factura.getNumeroConsecutivo())
                .proveedorNombre(factura.getProveedorNombre())
                .totalComprobante(factura.getTotalComprobante())
                .estadoInternoAnterior(estadoAnterior)
                .estadoHaciendaActual(estadoHacienda.getIndEstado())
                .fechaConsulta(LocalDateTime.now())
                .sincronizado(true)
                .mensaje("Verificado exitosamente")
                .build();

        } catch (Exception e) {
            log.error("❌ Error verificando factura {}: {}", facturaId, e.getMessage(), e);

            return VerificacionHaciendaDTO.builder()
                .facturaId(facturaId)
                .clave(factura.getClave())
                .numeroConsecutivo(factura.getNumeroConsecutivo())
                .proveedorNombre(factura.getProveedorNombre())
                .totalComprobante(factura.getTotalComprobante())
                .estadoInternoAnterior(factura.getEstadoHacienda())
                .sincronizado(false)
                .mensaje("Error: " + e.getMessage())
                .fechaConsulta(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Verifica TODAS las facturas de una empresa en un rango de fechas
     *
     * @param empresaId ID de la empresa
     * @param fechaDesde Fecha inicio
     * @param fechaHasta Fecha fin
     * @return Lista de resultados de verificación
     */
    @Transactional
    public List<VerificacionHaciendaDTO> verificarFacturasMasivo(
        Long empresaId,
        LocalDateTime fechaDesde,
        LocalDateTime fechaHasta) {

        log.info("🔍 Verificación masiva - Empresa: {}, Rango: {} a {}",
            empresaId, fechaDesde, fechaHasta);

        // 1. Buscar facturas en el rango
        List<FacturaRecepcion> facturas = facturaRecepcionRepository
            .findByEmpresaIdAndFechaEmisionBetween(empresaId, fechaDesde, fechaHasta);

        log.info("📋 Encontradas {} facturas para verificar", facturas.size());

        // 2. Obtener token UNA SOLA VEZ (reutilizable por 5 minutos)
        HaciendaTokenResponse tokenResponse = obtenerToken(empresaId);
        String accessToken = tokenResponse.getAccessToken();

        // 3. Verificar cada factura
        List<VerificacionHaciendaDTO> resultados = new ArrayList<>();
        int exitosos = 0;
        int fallidos = 0;

        for (FacturaRecepcion factura : facturas) {
            try {
                // Consultar Hacienda
                ConsultaEstadoResponse estadoHacienda = haciendaClient.getEstado(
                    accessToken,
                    false,
                    factura.getClave()
                );

                // Actualizar BD
                String estadoAnterior = factura.getEstadoHacienda();
                factura.setEstadoHacienda(estadoHacienda.getIndEstado());
                factura.setMensajeHacienda(estadoHacienda.getRespuestaXmlBase64()); // ✅ Base64
                facturaRecepcionRepository.save(factura);

                resultados.add(VerificacionHaciendaDTO.builder()
                    .facturaId(factura.getId())
                    .clave(factura.getClave())
                    .numeroConsecutivo(factura.getNumeroConsecutivo())
                    .proveedorNombre(factura.getProveedorNombre())
                    .totalComprobante(factura.getTotalComprobante())
                    .estadoInternoAnterior(estadoAnterior)
                    .estadoHaciendaActual(estadoHacienda.getIndEstado())
                    .fechaConsulta(LocalDateTime.now())
                    .sincronizado(true)
                    .mensaje("✅ Verificado")
                    .build());

                exitosos++;

                // Delay para no sobrecargar la API de Hacienda
                Thread.sleep(500); // 500ms entre requests

            } catch (Exception e) {
                log.error("Error verificando factura {}: {}", factura.getId(), e.getMessage());

                resultados.add(VerificacionHaciendaDTO.builder()
                    .facturaId(factura.getId())
                    .clave(factura.getClave())
                    .numeroConsecutivo(factura.getNumeroConsecutivo())
                    .proveedorNombre(factura.getProveedorNombre())
                    .totalComprobante(factura.getTotalComprobante())
                    .estadoInternoAnterior(factura.getEstadoHacienda())
                    .sincronizado(false)
                    .mensaje("❌ Error: " + e.getMessage())
                    .fechaConsulta(LocalDateTime.now())
                    .build());

                fallidos++;
            }
        }

        log.info("📊 Verificación completada - Exitosos: {}, Fallidos: {}", exitosos, fallidos);

        return resultados;
    }

    /**
     * Obtiene token de autenticación de Hacienda
     */
    private HaciendaTokenResponse obtenerToken(Long empresaId) {
        var config = empresaConfigRepository.findByEmpresaId(empresaId)
            .orElseThrow(() -> new RuntimeException("No hay configuración de Hacienda para empresa: " + empresaId));

        HaciendaAuthParams authParams = HaciendaAuthParams.builder()
            .username(config.getUsuarioHacienda())
            .password(config.getClaveHacienda())
            .clientId("api-prod")
            .sandbox(false)
            .build();

        return haciendaClient.getToken(authParams);
    }

    /**
     * DTO para resultados de verificación
     */
    @lombok.Data
    @lombok.Builder
    public static class VerificacionHaciendaDTO {
        private Long facturaId;
        private String clave;
        private String numeroConsecutivo;
        private String proveedorNombre;
        private java.math.BigDecimal totalComprobante;
        private String estadoInternoAnterior;
        private String estadoHaciendaActual;
        private LocalDateTime fechaConsulta;
        private boolean sincronizado;
        private String mensaje;
    }
}