package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Moneda;
import com.snnsoluciones.backnathbitpos.entity.TipoCambio;
import com.snnsoluciones.backnathbitpos.repository.MonedaRepository;
import com.snnsoluciones.backnathbitpos.repository.TipoCambioRepository;
import com.snnsoluciones.backnathbitpos.service.TipoCambioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TipoCambioServiceImpl implements TipoCambioService {
    
    private final TipoCambioRepository tipoCambioRepository;
    private final MonedaRepository monedaRepository;
    
    @Override
    @CacheEvict(value = "tiposCambio", allEntries = true)
    public TipoCambio crear(TipoCambio tipoCambio) {
        // Verificar si ya existe para esa fecha y moneda
        Optional<TipoCambio> existente = tipoCambioRepository.findByMonedaIdAndFecha(
            tipoCambio.getMoneda().getId(), 
            tipoCambio.getFecha()
        );
        
        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe tipo de cambio para esta moneda y fecha");
        }
        
        return tipoCambioRepository.save(tipoCambio);
    }
    
    @Override
    @CacheEvict(value = "tiposCambio", allEntries = true)
    public TipoCambio actualizar(Long id, TipoCambio tipoCambio) {
        TipoCambio existente = tipoCambioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tipo de cambio no encontrado"));
        
        existente.setTipoCambioCompra(tipoCambio.getTipoCambioCompra());
        existente.setTipoCambioVenta(tipoCambio.getTipoCambioVenta());
        existente.setTipoCambioReferencia(tipoCambio.getTipoCambioReferencia());
        existente.setFuente(tipoCambio.getFuente());
        
        return tipoCambioRepository.save(existente);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<TipoCambio> buscarPorId(Long id) {
        return tipoCambioRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "tiposCambio", key = "#codigoMoneda + '-' + #fecha")
    public Optional<TipoCambio> buscarPorMonedaYFecha(String codigoMoneda, LocalDate fecha) {
        return tipoCambioRepository.findByCodigoMonedaAndFecha(codigoMoneda, fecha);
    }
    
    @Override
    @Transactional(readOnly = true)
    public TipoCambio obtenerTipoCambioActual(String codigoMoneda) {
        LocalDate hoy = LocalDate.now();
        
        // Buscar tipo de cambio de hoy
        Optional<TipoCambio> tipoCambioHoy = buscarPorMonedaYFecha(codigoMoneda, hoy);
        if (tipoCambioHoy.isPresent()) {
            return tipoCambioHoy.get();
        }
        
        // Si no hay de hoy, buscar el más reciente
        Moneda moneda = monedaRepository.findByCodigo(codigoMoneda)
            .orElseThrow(() -> new RuntimeException("Moneda no encontrada: " + codigoMoneda));
        
        List<TipoCambio> ultimos = tipoCambioRepository.findUltimosByMonedaId(moneda.getId(), 1);
        if (ultimos.isEmpty()) {
            throw new RuntimeException("No hay tipos de cambio registrados para " + codigoMoneda);
        }
        
        return ultimos.get(0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TipoCambio> listarPorFechaRango(LocalDate fechaInicio, LocalDate fechaFin) {
        return tipoCambioRepository.findByFechaRango(fechaInicio, fechaFin);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TipoCambio> listarUltimosPorMoneda(String codigoMoneda, int cantidad) {
        Moneda moneda = monedaRepository.findByCodigo(codigoMoneda)
            .orElseThrow(() -> new RuntimeException("Moneda no encontrada: " + codigoMoneda));
        
        return tipoCambioRepository.findUltimosByMonedaId(moneda.getId(), cantidad);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal convertir(BigDecimal monto, String monedaOrigen, String monedaDestino, LocalDate fecha) {
        if (monedaOrigen.equals(monedaDestino)) {
            return monto;
        }
        
        // Si una de las monedas es CRC, conversión directa
        if ("CRC".equals(monedaOrigen)) {
            return convertirDeColones(monto, monedaDestino);
        } else if ("CRC".equals(monedaDestino)) {
            return convertirAColones(monto, monedaOrigen);
        }
        
        // Si ninguna es CRC, convertir a través de colones
        BigDecimal montoEnColones = convertirAColones(monto, monedaOrigen);
        return convertirDeColones(montoEnColones, monedaDestino);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal convertirAColones(BigDecimal monto, String monedaOrigen) {
        if ("CRC".equals(monedaOrigen)) {
            return monto;
        }
        
        TipoCambio tipoCambio = obtenerTipoCambioActual(monedaOrigen);
        return monto.multiply(tipoCambio.getTipoCambioVenta())
                    .setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal convertirDeColones(BigDecimal monto, String monedaDestino) {
        if ("CRC".equals(monedaDestino)) {
            return monto;
        }
        
        TipoCambio tipoCambio = obtenerTipoCambioActual(monedaDestino);
        return monto.divide(tipoCambio.getTipoCambioCompra(), 2, RoundingMode.HALF_UP);
    }
    
    @Override
    @CacheEvict(value = "tiposCambio", allEntries = true)
    public void actualizarTiposCambioDiarios() {
        // TODO: Implementar llamada a API BCCR
        log.info("Actualizando tipos de cambio desde BCCR...");
        
        // Por ahora, ejemplo manual
        LocalDate hoy = LocalDate.now();
        
        // USD
        monedaRepository.findByCodigo("USD").ifPresent(moneda -> {
            if (tipoCambioRepository.findByMonedaIdAndFecha(moneda.getId(), hoy).isEmpty()) {
                TipoCambio tc = new TipoCambio();
                tc.setMoneda(moneda);
                tc.setFecha(hoy);
                tc.setTipoCambioCompra(new BigDecimal("625.50"));
                tc.setTipoCambioVenta(new BigDecimal("632.00"));
                tc.setFuente("BCCR");
                tipoCambioRepository.save(tc);
            }
        });
        
        // EUR
        monedaRepository.findByCodigo("EUR").ifPresent(moneda -> {
            if (tipoCambioRepository.findByMonedaIdAndFecha(moneda.getId(), hoy).isEmpty()) {
                TipoCambio tc = new TipoCambio();
                tc.setMoneda(moneda);
                tc.setFecha(hoy);
                tc.setTipoCambioCompra(new BigDecimal("680.00"));
                tc.setTipoCambioVenta(new BigDecimal("688.50"));
                tc.setFuente("BCCR");
                tipoCambioRepository.save(tc);
            }
        });
        
        log.info("Actualización de tipos de cambio completada");
    }
}