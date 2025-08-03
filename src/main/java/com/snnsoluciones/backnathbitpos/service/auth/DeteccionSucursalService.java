package com.snnsoluciones.backnathbitpos.service.auth;

import com.snnsoluciones.backnathbitpos.entity.global.ConfiguracionAcceso;
import com.snnsoluciones.backnathbitpos.entity.global.EmpresaSucursal;
import com.snnsoluciones.backnathbitpos.enums.TipoDeteccion;
import com.snnsoluciones.backnathbitpos.repository.global.ConfiguracionAccesoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Servicio para detectar automáticamente la sucursal basándose en diferentes criterios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeteccionSucursalService {

    private final ConfiguracionAccesoRepository configuracionAccesoRepository;

    /**
     * Detecta la sucursal basándose en la IP del cliente
     */
    @Cacheable(value = "sucursal-por-ip", key = "#ipAddress")
    public EmpresaSucursal detectarPorIP(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return null;
        }

        log.debug("Intentando detectar sucursal para IP: {}", ipAddress);

        // Obtener todas las configuraciones activas de tipo IP_RANGE
        List<ConfiguracionAcceso> configuracionesIP = configuracionAccesoRepository
            .findByTipoDeteccionAndActivoOrderByPrioridadDesc(
                TipoDeteccion.IP_RANGE, true
            );

        for (ConfiguracionAcceso config : configuracionesIP) {
            if (ipEnRango(ipAddress, config)) {
                log.info("Sucursal detectada por IP {}: {}", 
                    ipAddress, config.getSucursal().getNombreSucursal());
                return config.getSucursal();
            }
        }

        log.debug("No se encontró sucursal para IP: {}", ipAddress);
        return null;
    }

    /**
     * Detecta la sucursal basándose en el ID del terminal
     */
    @Cacheable(value = "sucursal-por-terminal", key = "#terminalId")
    public EmpresaSucursal detectarPorTerminal(String terminalId) {
        if (terminalId == null || terminalId.isEmpty()) {
            return null;
        }

        log.debug("Intentando detectar sucursal para terminal: {}", terminalId);

        List<ConfiguracionAcceso> configuracionesTerminal = configuracionAccesoRepository
            .findByTipoDeteccionAndActivoOrderByPrioridadDesc(
                TipoDeteccion.TERMINAL_ID, true
            );

        for (ConfiguracionAcceso config : configuracionesTerminal) {
            String configTerminalId = config.getTerminalId();
            if (terminalId.equals(configTerminalId)) {
                log.info("Sucursal detectada por terminal {}: {}", 
                    terminalId, config.getSucursal().getNombreSucursal());
                return config.getSucursal();
            }
        }

        return null;
    }

    /**
     * Detecta la sucursal usando múltiples métodos en orden de prioridad
     */
    public EmpresaSucursal detectarAutomaticamente(String ipAddress, String terminalId, String macAddress) {
        // 1. Intentar por terminal (mayor prioridad)
        if (terminalId != null) {
            EmpresaSucursal sucursal = detectarPorTerminal(terminalId);
            if (sucursal != null) {
                return sucursal;
            }
        }

        // 2. Intentar por IP
        if (ipAddress != null) {
            EmpresaSucursal sucursal = detectarPorIP(ipAddress);
            if (sucursal != null) {
                return sucursal;
            }
        }

        // 3. Intentar por MAC address si está disponible
        if (macAddress != null) {
            EmpresaSucursal sucursal = detectarPorMacAddress(macAddress);
            if (sucursal != null) {
                return sucursal;
            }
        }

        return null;
    }

    /**
     * Verifica si una IP está dentro del rango configurado
     */
    private boolean ipEnRango(String ipAddress, ConfiguracionAcceso config) {
        try {
            String ipInicio = config.getIpInicio();
            String ipFin = config.getIpFin();

            if (ipInicio == null || ipFin == null) {
                return false;
            }

            long ipLong = ipToLong(InetAddress.getByName(ipAddress));
            long inicioLong = ipToLong(InetAddress.getByName(ipInicio));
            long finLong = ipToLong(InetAddress.getByName(ipFin));

            return ipLong >= inicioLong && ipLong <= finLong;

        } catch (UnknownHostException e) {
            log.error("Error al procesar IP: {}", ipAddress, e);
            return false;
        }
    }

    /**
     * Convierte una IP a su representación numérica para comparación
     */
    private long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    /**
     * Detecta por MAC address (para terminales específicos)
     */
    private EmpresaSucursal detectarPorMacAddress(String macAddress) {
        List<ConfiguracionAcceso> configuraciones = configuracionAccesoRepository
            .findByTipoDeteccionAndActivoOrderByPrioridadDesc(
                TipoDeteccion.TERMINAL_ID, true
            );

        for (ConfiguracionAcceso config : configuraciones) {
            String configMac = config.getMacAddress();
            if (macAddress.equalsIgnoreCase(configMac)) {
                log.info("Sucursal detectada por MAC {}: {}", 
                    macAddress, config.getSucursal().getNombreSucursal());
                return config.getSucursal();
            }
        }

        return null;
    }

    /**
     * Limpia la caché de detección
     */
    public void limpiarCache() {
        // Este método será llamado cuando se actualicen las configuraciones
        log.info("Limpiando caché de detección de sucursales");
    }
}