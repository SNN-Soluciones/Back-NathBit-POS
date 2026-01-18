package com.snnsoluciones.backnathbitpos.service.logo;

import com.snnsoluciones.backnathbitpos.dto.logo.LogoResponseDTO;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoService {

    private final SucursalRepository sucursalRepository;
    private final StorageService storageService;

    // URL del logo por defecto
    private static final String LOGO_DEFAULT_URL =
        "https://snn-soluciones.nyc3.cdn.digitaloceanspaces.com/LOGO_DEFAULT/Nathbit.png";

    /**
     * Obtiene el logo para una sucursal específica
     * Prioridad: Logo de Sucursal → Logo de Empresa → Logo Default
     *
     * @param sucursalId ID de la sucursal
     * @return DTO con información del logo
     */
    @Transactional(readOnly = true)
    public LogoResponseDTO obtenerLogo(Long sucursalId) {
        log.info("🔍 Buscando logo para sucursal ID: {}", sucursalId);

        // Buscar sucursal con empresa
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
            .orElseThrow(() -> new RuntimeException("Sucursal no encontrada: " + sucursalId));

        Empresa empresa = sucursal.getEmpresa();

        String logoPath = null;
        String logoUrl = null;
        String tipoLogo;
        String nombre;

        // ========================================
        // PRIORIDAD 1: Logo de Sucursal
        // ========================================
        if (sucursal.getLogoSucursalPath() != null && !sucursal.getLogoSucursalPath().trim().isEmpty()) {
            logoPath = sucursal.getLogoSucursalPath();
            tipoLogo = "SUCURSAL";
            nombre = sucursal.getNombre();
            log.info("✅ Logo encontrado en SUCURSAL: {}", logoPath);
        }
        // ========================================
        // PRIORIDAD 2: Logo de Empresa
        // ========================================
        else if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().trim().isEmpty()) {
            logoPath = empresa.getLogoUrl();
            tipoLogo = "EMPRESA";
            nombre = empresa.getNombreComercial() != null
                ? empresa.getNombreComercial()
                : empresa.getNombreRazonSocial();
            log.info("✅ Logo encontrado en EMPRESA: {}", logoPath);
        }
        // ========================================
        // PRIORIDAD 3: Logo Default
        // ========================================
        else {
            logoUrl = LOGO_DEFAULT_URL;
            tipoLogo = "DEFAULT";
            nombre = empresa.getNombreComercial() != null
                ? empresa.getNombreComercial()
                : empresa.getNombreRazonSocial();
            log.warn("⚠️ No se encontró logo personalizado, usando logo default");
        }

        // Si encontramos un logo en S3, generar URL firmada
        if (logoPath != null) {
            try {
                // URL válida por 60 minutos
                logoUrl = storageService.generarUrlPreFirmada(logoPath, Duration.ofMinutes(60));
                log.info("🔗 URL firmada generada (válida 60 min)");
            } catch (Exception e) {
                log.error("❌ Error generando URL firmada para: {}, usando logo default", logoPath, e);
                // Si falla la URL firmada, usar logo default
                logoUrl = LOGO_DEFAULT_URL;
                tipoLogo = "DEFAULT";
            }
        }

        // Generar iniciales
        String iniciales = generarIniciales(nombre);

        return LogoResponseDTO.builder()
            .logoUrl(logoUrl)
            .nombre(nombre)
            .tipoLogo(tipoLogo)
            .empresaId(empresa.getId())
            .sucursalId(sucursal.getId())
            .iniciales(iniciales)
            .build();
    }

    /**
     * Genera iniciales a partir de un nombre
     * Ejemplos:
     * - "Bar La Cinco31" → "BC"
     * - "NathBit" → "N"
     * - "Super Mercado Central" → "SC"
     */
    private String generarIniciales(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "NB"; // Default NathBit
        }

        String[] palabras = nombre.trim().split("\\s+");

        if (palabras.length == 1) {
            // Una sola palabra: primera letra
            return palabras[0].substring(0, 1).toUpperCase();
        } else {
            // Múltiples palabras: primera letra de las primeras 2 palabras
            String primera = palabras[0].substring(0, 1).toUpperCase();
            String segunda = palabras[1].substring(0, 1).toUpperCase();
            return primera + segunda;
        }
    }
}