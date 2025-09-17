package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.pago.RegistrarPagoDTO;
import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.entity.Pago;
import com.snnsoluciones.backnathbitpos.entity.SesionCaja;
import com.snnsoluciones.backnathbitpos.entity.Usuario;
import com.snnsoluciones.backnathbitpos.enums.EstadoCuenta;
import com.snnsoluciones.backnathbitpos.enums.EstadoPago;
import com.snnsoluciones.backnathbitpos.enums.mh.MedioPago;
import com.snnsoluciones.backnathbitpos.exception.BadRequestException;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.CuentaPorCobrarRepository;
import com.snnsoluciones.backnathbitpos.repository.PagoRepository;
import com.snnsoluciones.backnathbitpos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PagoService {
    
    private final PagoRepository pagoRepository;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final UsuarioRepository usuarioRepository;
    private final SesionCajaService sesionCajaService;
    private final ClienteService clienteService;
    
    /**
     * Registrar un pago a una cuenta por cobrar
     */
    public Pago registrarPago(RegistrarPagoDTO dto) {
        // Validar cuenta
        CuentaPorCobrar cuenta = cuentaPorCobrarRepository.findById(dto.getCuentaPorCobrarId())
            .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada"));
            
        // Validar que tenga saldo pendiente
        if (cuenta.getSaldo().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("La cuenta no tiene saldo pendiente");
        }
        
        // Validar monto
        if (dto.getMonto().compareTo(cuenta.getSaldo()) > 0) {
            throw new BadRequestException("El monto excede el saldo pendiente");
        }
        
        // Obtener cajero actual
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario cajero = usuarioRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            
        // Obtener sesión de caja activa
        SesionCaja sesion = sesionCajaService.buscarSesionActiva(cajero.getId())
            .orElseThrow(() -> new BadRequestException("No hay sesión de caja activa"));
        
        // Crear el pago
        Pago pago = new Pago();
        pago.setCuentaPorCobrar(cuenta);
        pago.setCliente(cuenta.getCliente());
        pago.setCajero(cajero);
        pago.setSesionCaja(sesion);
        pago.setMedioPago(MedioPago.valueOf(dto.getMedioPago()));
        pago.setMonto(dto.getMonto());
        pago.setReferencia(dto.getReferencia());
        pago.setObservaciones(dto.getObservaciones());
        pago.setFechaPago(LocalDateTime.now());
        pago.setEstado(EstadoPago.APLICADO);
        
        // Generar número de recibo
        pago.setNumeroRecibo(generarNumeroRecibo(sesion.getTerminal().getSucursal().getId()));
        
        // Guardar pago
        pago = pagoRepository.save(pago);
        
        // Actualizar saldo de la cuenta
        BigDecimal nuevoSaldo = cuenta.getSaldo().subtract(dto.getMonto());
        cuenta.setSaldo(nuevoSaldo);
        cuenta.setUltimoPago(LocalDateTime.now());
        
        // Actualizar estado si se pagó completo
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            cuenta.setEstado(EstadoCuenta.PAGADA);
        } else {
            cuenta.setEstado(EstadoCuenta.PARCIAL);
        }
        
        cuentaPorCobrarRepository.save(cuenta);
        
        // Actualizar saldo del cliente
        actualizarSaldoCliente(cuenta.getCliente().getId());
        
        log.info("Pago registrado: {} por monto {}", pago.getNumeroRecibo(), pago.getMonto());
        
        return pago;
    }
    
    /**
     * Generar número de recibo consecutivo
     */
    private String generarNumeroRecibo(Long sucursalId) {
        String ultimo = pagoRepository.findUltimoNumeroRecibo(sucursalId);
        if (ultimo == null) {
            return "REC-000001";
        }
        
        // Extraer número y sumar 1
        String numero = ultimo.substring(4);
        int siguiente = Integer.parseInt(numero) + 1;
        return String.format("REC-%06d", siguiente);
    }
    
    /**
     * Actualizar saldo del cliente
     */
    /**
     * Actualizar saldo del cliente
     */
    private void actualizarSaldoCliente(Long clienteId) {
        BigDecimal saldoTotal = cuentaPorCobrarRepository.sumSaldoByClienteId(clienteId);

        // Buscar el cliente por ID
        clienteService.findById(clienteId).ifPresent(cliente -> {
            cliente.setSaldoActual(saldoTotal);
            cliente.setFechaUltimoPago(LocalDateTime.now());
            clienteService.save(cliente);
        });
    }
}