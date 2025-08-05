package com.snnsoluciones.backnathbitpos.service.cash;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.config.tenant.TenantContext;
import com.snnsoluciones.backnathbitpos.dto.cash.AperturaCajaRequest;
import com.snnsoluciones.backnathbitpos.dto.cash.AperturaCajaResponse;
import com.snnsoluciones.backnathbitpos.dto.cash.CierreCajaRequest;
import com.snnsoluciones.backnathbitpos.dto.cash.CierreCajaResponse;
import com.snnsoluciones.backnathbitpos.entity.global.UsuarioGlobal;
import com.snnsoluciones.backnathbitpos.exception.BusinessException;
import com.snnsoluciones.backnathbitpos.repository.global.UsuarioGlobalRepository;
import com.snnsoluciones.backnathbitpos.util.ContextUtils;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CajaService {
    
    private final JdbcTemplate jdbcTemplate;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final ObjectMapper objectMapper;
    
    public AperturaCajaResponse abrirCaja(AperturaCajaRequest request)
        throws JsonProcessingException {
        UsuarioGlobal usuario = ContextUtils.getCurrentUser();
        String tenantId = TenantContext.getCurrentTenant();
        
        // Verificar que no hay una caja abierta
        String sqlCheck = """
            SELECT COUNT(*) FROM apertura_cierre_caja 
            WHERE caja_id = ? AND tipo = 'APERTURA' AND estado = 'ACTIVO'
        """;
        
        Integer count = jdbcTemplate.queryForObject(sqlCheck, Integer.class, request.getCajaId());
        if (count > 0) {
            throw new BusinessException("La caja ya está abierta");
        }
        
        // Insertar apertura
        String sql = """
            INSERT INTO apertura_cierre_caja 
            (caja_id, usuario_id, usuario_nombre, tipo, monto_inicial, denominaciones, observaciones)
            VALUES (?, ?, ?, 'APERTURA', ?, ?::jsonb, ?)
            RETURNING id, fecha_hora
        """;
        
        Map<String, Object> result = jdbcTemplate.queryForMap(
            sql,
            request.getCajaId(),
            usuario.getId(),
            usuario.getNombreCompleto(),
            request.getMontoInicial(),
            objectMapper.writeValueAsString(request.getDenominaciones()),
            request.getObservaciones()
        );
        
        return AperturaCajaResponse.builder()
            .id((UUID) result.get("id"))
            .fechaHora((LocalDateTime) result.get("fecha_hora"))
            .usuario(usuario.getNombreCompleto())
            .montoInicial(request.getMontoInicial())
            .mensaje("Caja abierta exitosamente")
            .build();
    }
    
    public CierreCajaResponse cerrarCaja(CierreCajaRequest request) throws JsonProcessingException {
        UsuarioGlobal usuario = ContextUtils.getCurrentUser();
        
        // Obtener apertura
        String sqlApertura = """
            SELECT caja_id, monto_inicial, fecha_hora 
            FROM apertura_cierre_caja 
            WHERE id = ? AND tipo = 'APERTURA' AND estado = 'ACTIVO'
        """;
        
        Map<String, Object> apertura = jdbcTemplate.queryForMap(sqlApertura, request.getAperturaId());
        
        // Calcular ventas del período
        BigDecimal montoVentas = calcularVentasPeriodo(
            (UUID) apertura.get("caja_id"),
            (LocalDateTime) apertura.get("fecha_hora"),
            LocalDateTime.now()
        );
        
        // Calcular diferencia
        BigDecimal montoEsperado = ((BigDecimal) apertura.get("monto_inicial")).add(montoVentas);
        BigDecimal diferencia = request.getMontoFinal().subtract(montoEsperado);
        
        // Actualizar apertura a cerrada
        jdbcTemplate.update(
            "UPDATE apertura_cierre_caja SET estado = 'CERRADO' WHERE id = ?",
            request.getAperturaId()
        );
        
        // Insertar cierre
        String sqlCierre = """
            INSERT INTO apertura_cierre_caja 
            (caja_id, usuario_id, usuario_nombre, tipo, monto_inicial, monto_final, 
             monto_ventas, diferencia, denominaciones, observaciones, estado)
            VALUES (?, ?, ?, 'CIERRE', ?, ?, ?, ?, ?::jsonb, ?, 'CERRADO')
            RETURNING id, fecha_hora
        """;
        
        Map<String, Object> result = jdbcTemplate.queryForMap(
            sqlCierre,
            apertura.get("caja_id"),
            usuario.getId(),
            usuario.getNombreCompleto(),
            apertura.get("monto_inicial"),
            request.getMontoFinal(),
            montoVentas,
            diferencia,
            objectMapper.writeValueAsString(request.getDenominaciones()),
            request.getObservaciones()
        );
        
        return CierreCajaResponse.builder()
            .id((UUID) result.get("id"))
            .fechaCierre((LocalDateTime) result.get("fecha_hora"))
            .montoInicial((BigDecimal) apertura.get("monto_inicial"))
            .montoVentas(montoVentas)
            .montoFinal(request.getMontoFinal())
            .montoEsperado(montoEsperado)
            .diferencia(diferencia)
            .estado(diferencia.compareTo(BigDecimal.ZERO) == 0 ? "CUADRADO" : 
                    diferencia.compareTo(BigDecimal.ZERO) > 0 ? "SOBRANTE" : "FALTANTE")
            .build();
    }
    
    private BigDecimal calcularVentasPeriodo(UUID cajaId, LocalDateTime desde, LocalDateTime hasta) {
        String sql = """
            SELECT COALESCE(SUM(o.total), 0) as total
            FROM ordenes o
            WHERE o.caja_id = ? 
            AND o.fecha_creacion BETWEEN ? AND ?
            AND o.estado IN ('PAGADA', 'CERRADA')
        """;
        
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, cajaId, desde, hasta);
    }
}