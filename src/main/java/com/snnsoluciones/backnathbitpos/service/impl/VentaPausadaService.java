package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.dto.factura.CrearVentaPausadaRequest;
import com.snnsoluciones.backnathbitpos.dto.factura.VentaPausadaDetalleDTO;
import com.snnsoluciones.backnathbitpos.dto.factura.VentaPausadaListDTO;
import com.snnsoluciones.backnathbitpos.entity.VentaPausada;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.VentaPausadaRepository;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VentaPausadaService {

    private final VentaPausadaRepository repository;
    private final SecurityContextService securityContext;

    private static final int MAX_VENTAS_PAUSADAS_POR_USUARIO = 10;

    public VentaPausadaListDTO crear(CrearVentaPausadaRequest request, Long sucursalId, Long terminalId) {
        Long usuarioId = securityContext.getCurrentUserId();

        // Validar límite de ventas pausadas
        long ventasActuales = repository.countByUsuarioIdAndSucursalIdAndFechaExpiracionAfter(
            usuarioId, sucursalId, LocalDateTime.now()
        );

        if (ventasActuales >= MAX_VENTAS_PAUSADAS_POR_USUARIO) {
            throw new BusinessException("Has alcanzado el límite máximo de " +
                MAX_VENTAS_PAUSADAS_POR_USUARIO + " ventas pausadas");
        }

        // Crear la venta pausada
        VentaPausada ventaPausada = new VentaPausada();
        ventaPausada.setUsuarioId(usuarioId);
        ventaPausada.setSucursalId(sucursalId); // Usar el parámetro recibido
        ventaPausada.setTerminalId(terminalId); // Usar el parámetro recibido
        ventaPausada.setDatosFactura(request.getDatosFactura());

        // Generar descripción automática si no viene
        if (request.getDescripcion() == null || request.getDescripcion().isEmpty()) {
            ventaPausada.setDescripcion(generarDescripcion(request.getDatosFactura()));
        } else {
            ventaPausada.setDescripcion(request.getDescripcion());
        }

        VentaPausada saved = repository.save(ventaPausada);
        return mapToListDTO(saved);
    }

    public List<VentaPausadaListDTO> listarActivas(Long sucursalId) {
        Long usuarioId = securityContext.getCurrentUserId();

        List<VentaPausada> ventas = repository
            .findByUsuarioIdAndSucursalIdAndFechaExpiracionAfterOrderByFechaCreacionDesc(
                usuarioId, sucursalId, LocalDateTime.now()
            );

        return ventas.stream()
            .map(this::mapToListDTO)
            .collect(Collectors.toList());
    }

    public VentaPausadaDetalleDTO obtenerDetalle(Long id, Long sucursalId) {
        Long usuarioId = securityContext.getCurrentUserId();

        VentaPausada venta = repository
            .findByIdAndUsuarioIdAndSucursalId(id, usuarioId, sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Venta pausada no encontrada"));

        VentaPausadaDetalleDTO dto = new VentaPausadaDetalleDTO();
        dto.setId(venta.getId());
        dto.setDatosFactura(venta.getDatosFactura());
        dto.setDescripcion(venta.getDescripcion());
        dto.setFechaCreacion(venta.getFechaCreacion());

        return dto;
    }

    public void eliminar(Long id, Long sucursalId) {
        Long usuarioId = securityContext.getCurrentUserId();

        VentaPausada venta = repository
            .findByIdAndUsuarioIdAndSucursalId(id, usuarioId, sucursalId)
            .orElseThrow(() -> new ResourceNotFoundException("Venta pausada no encontrada"));

        repository.delete(venta);
    }

    public long contarActivas(Long sucursalId) {
        Long usuarioId = securityContext.getCurrentUserId();

        return repository.countByUsuarioIdAndSucursalIdAndFechaExpiracionAfter(
            usuarioId, sucursalId, LocalDateTime.now()
        );
    }

    // Método para supervisores - ver todas las de la sucursal
    public List<VentaPausadaListDTO> listarTodasSucursal(Long sucursalId) {
        List<VentaPausada> ventas = repository
            .findAllBySucursalActivas(
                sucursalId, LocalDateTime.now()
            );

        return ventas.stream()
            .map(this::mapToListDTO)
            .collect(Collectors.toList());
    }


    // Helpers privados
    private VentaPausadaListDTO mapToListDTO(VentaPausada venta) {
        VentaPausadaListDTO dto = new VentaPausadaListDTO();
        dto.setId(venta.getId());
        dto.setDescripcion(venta.getDescripcion());
        dto.setFechaCreacion(venta.getFechaCreacion());
        dto.setFechaExpiracion(venta.getFechaExpiracion());

        // Extraer datos del JSON
        Map<String, Object> datos = venta.getDatosFactura();

        // Cliente
        if (datos.containsKey("cliente")) {
            Map<String, Object> cliente = (Map<String, Object>) datos.get("cliente");
            if (cliente != null && cliente.containsKey("nombre")) {
                dto.setClienteNombre(cliente.get("nombre").toString());
            } else {
                dto.setClienteNombre("Cliente Genérico");
            }
        }

        // Cantidad de items
        if (datos.containsKey("detalles")) {
            List<Map<String, Object>> detalles = (List<Map<String, Object>>) datos.get("detalles");
            int totalItems = detalles.stream()
                .mapToInt(d -> ((Number) d.getOrDefault("cantidad", 0)).intValue())
                .sum();
            dto.setCantidadItems(totalItems);
        }

        // Monto total
        if (datos.containsKey("resumen") && datos.get("resumen") != null) {
            Map<String, Object> resumen = (Map<String, Object>) datos.get("resumen");
            dto.setMontoTotal(((Number) resumen.getOrDefault("totalComprobante", 0)).doubleValue());
        }

        // Calcular tiempos
        dto.setTiempoTranscurrido(calcularTiempoTranscurrido(venta.getFechaCreacion()));
        dto.setTiempoRestante(calcularTiempoRestante(venta.getFechaExpiracion()));

        return dto;
    }

    private String generarDescripcion(Map<String, Object> datosFactura) {
        if (datosFactura == null) {
            return "Venta sin título";
        }

        StringBuilder desc = new StringBuilder();

        Map<String, Object> receptor = (Map<String, Object>)
            datosFactura.getOrDefault("receptor", new HashMap<>());

        String nombre = (String) receptor.get("nombre");
        if (nombre != null && !nombre.isEmpty()) {
            desc.append(nombre);
        }

        // Intentar obtener el nombre del cliente
        if (datosFactura.containsKey("cliente")) {
            Map<String, Object> cliente = (Map<String, Object>) datosFactura.get("cliente");
            if (cliente != null && cliente.containsKey("nombre")) {
                desc.append(cliente.get("nombre").toString());
            }
        }

        // Si no hay cliente, usar cantidad de items
        if (desc.isEmpty() && datosFactura.containsKey("detalles")) {
            List<Map<String, Object>> detalles = (List<Map<String, Object>>) datosFactura.get("detalles");
            desc.append("Venta con ").append(detalles.size()).append(" productos");
        }

        return !desc.isEmpty() ? desc.toString() : "Venta sin título";
    }

    private String calcularTiempoTranscurrido(LocalDateTime fecha) {
        Duration duration = Duration.between(fecha, LocalDateTime.now());

        if (duration.toMinutes() < 1) {
            return "Hace menos de 1 minuto";
        } else if (duration.toMinutes() < 60) {
            return "Hace " + duration.toMinutes() + " minutos";
        } else if (duration.toHours() < 24) {
            return "Hace " + duration.toHours() + " horas";
        } else {
            return "Hace " + duration.toDays() + " días";
        }
    }

    private String calcularTiempoRestante(LocalDateTime fechaExpiracion) {
        Duration duration = Duration.between(LocalDateTime.now(), fechaExpiracion);

        if (duration.isNegative()) {
            return "Expirado";
        } else if (duration.toHours() < 1) {
            return "Expira en " + duration.toMinutes() + " minutos";
        } else if (duration.toHours() < 24) {
            return "Expira en " + duration.toHours() + " horas";
        } else {
            return "Expira en " + duration.toDays() + " días";
        }
    }
}