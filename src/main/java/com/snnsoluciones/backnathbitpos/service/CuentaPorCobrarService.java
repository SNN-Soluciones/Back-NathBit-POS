package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.EstadoCuenta;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.repository.CuentaPorCobrarRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CuentaPorCobrarService {
    
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final ClienteRepository clienteRepository;
    
    /**
     * Crear cuenta por cobrar desde una factura a crédito
     */
    public CuentaPorCobrar crearDesdeFactura(Factura factura) {
        // Validar que sea venta a crédito
        if (factura.getCondicionVenta() != CondicionVenta.CREDITO) {
            log.info("Factura {} no es a crédito, no se crea cuenta por cobrar", 
                factura.getConsecutivo());
            return null;
        }
        
        // Validar que no exista ya
        if (cuentaPorCobrarRepository.findByFacturaId(factura.getId()).isPresent()) {
            log.warn("Ya existe cuenta por cobrar para factura {}", factura.getId());
            return null;
        }
        
        // Crear la cuenta
        CuentaPorCobrar cuenta = new CuentaPorCobrar();
        cuenta.setFactura(factura);
        cuenta.setCliente(factura.getCliente());
        cuenta.setEmpresa(factura.getSucursal().getEmpresa());
        cuenta.setFechaEmision(LocalDate.now());
        
        // Calcular vencimiento
        Integer diasCredito = factura.getPlazoCredito() != null ? 
            factura.getPlazoCredito() : 
            factura.getCliente().getDiasCredito();
            
        cuenta.setFechaVencimiento(cuenta.getFechaEmision().plusDays(diasCredito));
        cuenta.setMontoOriginal(factura.getTotalComprobante());
        cuenta.setSaldo(factura.getTotalComprobante());
        cuenta.setEstado(EstadoCuenta.VIGENTE);
        cuenta.setDiasMora(0);
        
        cuenta = cuentaPorCobrarRepository.save(cuenta);
        
        // Actualizar saldo del cliente
        actualizarSaldoCliente(factura.getCliente().getId());
        
        log.info("Cuenta por cobrar creada para factura {} por monto {}", 
            factura.getConsecutivo(), 
            cuenta.getMontoOriginal());
            
        return cuenta;
    }

    @Transactional
    public void actualizarEstadosVencidos() {
        log.info("Iniciando actualización automática de estados de cuentas");

        LocalDate hoy = LocalDate.now();

        // Buscar solo cuentas que necesitan actualización
        List<CuentaPorCobrar> cuentasPorActualizar = cuentaPorCobrarRepository
            .findVencidas(null, hoy); // Modificar query para aceptar null y buscar todas

        int actualizadas = 0;
        int clientesBloqueados = 0;

        for (CuentaPorCobrar cuenta : cuentasPorActualizar) {
            long diasMora = ChronoUnit.DAYS.between(cuenta.getFechaVencimiento(), hoy);
            cuenta.setDiasMora((int) diasMora);

            // Cambiar estado
            if (cuenta.getEstado() == EstadoCuenta.VIGENTE) {
                cuenta.setEstado(EstadoCuenta.VENCIDA);
            }

            // Verificar bloqueo por mora (más de 30 días)
            if (diasMora > 30) {
                Cliente cliente = cuenta.getCliente();
                if (!Boolean.TRUE.equals(cliente.getBloqueadoPorMora())) {
                    cliente.setBloqueadoPorMora(true);
                    cliente.setEstadoCredito("BLOQUEADO_MORA");
                    clientesBloqueados++;

                    log.warn("Cliente {} bloqueado por mora de {} días en factura {}",
                        cliente.getRazonSocial(),
                        diasMora,
                        cuenta.getFactura().getConsecutivo()
                    );
                }
            }

            actualizadas++;
        }

        // Guardar cambios en batch
        if (!cuentasPorActualizar.isEmpty()) {
            cuentaPorCobrarRepository.saveAll(cuentasPorActualizar);
        }

        log.info("Actualización completada: {} cuentas actualizadas, {} clientes bloqueados",
            actualizadas, clientesBloqueados);
    }
    
    /**
     * Actualizar saldo actual del cliente
     */
    private void actualizarSaldoCliente(Long clienteId) {
        var saldoTotal = cuentaPorCobrarRepository.sumSaldoByClienteId(clienteId);
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.setSaldoActual(saldoTotal);
            clienteRepository.save(cliente);
        });
    }
}