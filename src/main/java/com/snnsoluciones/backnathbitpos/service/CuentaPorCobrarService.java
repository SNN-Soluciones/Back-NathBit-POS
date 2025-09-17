package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.cxc.CuentaPorCobrarDTO;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.entity.Factura;
import com.snnsoluciones.backnathbitpos.enums.EstadoCuenta;
import com.snnsoluciones.backnathbitpos.enums.mh.CondicionVenta;
import com.snnsoluciones.backnathbitpos.repository.CuentaPorCobrarRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import jakarta.persistence.criteria.Join;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    public Page<CuentaPorCobrarDTO> listarPorEmpresa(
        Long empresaId,
        String busqueda,
        String estado,
        boolean soloVencidas,
        Long clienteId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        Pageable pageable) {

        // Crear especificación dinámica para los filtros
        Specification<CuentaPorCobrar> spec = (root, query, cb) -> cb.conjunction();

        // Filtro por empresa (a través del cliente)
        spec = spec.and((root, query, cb) -> {
            Join<CuentaPorCobrar, Cliente> clienteJoin = root.join("cliente");
            return cb.equal(clienteJoin.get("empresa").get("id"), empresaId);
        });

        // Filtro por empresa (a través del cliente)
        spec = spec.and((root, query, cb) -> {
            Join<CuentaPorCobrar, Cliente> clienteJoin = root.join("cliente");
            return cb.equal(clienteJoin.get("empresa").get("id"), empresaId);
        });

        // Filtro por búsqueda (cliente o factura)
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                Join<CuentaPorCobrar, Cliente> clienteJoin = root.join("cliente");
                Join<CuentaPorCobrar, Factura> facturaJoin = root.join("factura");

                String searchPattern = "%" + busqueda.toLowerCase() + "%";

                return cb.or(
                    cb.like(cb.lower(clienteJoin.get("razonSocial")), searchPattern),
                    cb.like(cb.lower(clienteJoin.get("nombreComercial")), searchPattern),
                    cb.like(cb.lower(clienteJoin.get("numeroIdentificacion")), searchPattern),
                    cb.like(cb.lower(facturaJoin.get("numeroFactura")), searchPattern)
                );
            });
        }

        // Filtro por estado
        if (estado != null && !estado.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("estado"), EstadoCuenta.valueOf(estado))
            );
        }

        // Filtro solo vencidas
        if (soloVencidas) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("estado"), EstadoCuenta.VENCIDA)
            );
        }

        // Filtro por cliente específico
        if (clienteId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("cliente").get("id"), clienteId)
            );
        }

        // Filtro por rango de fechas
        if (fechaDesde != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaDesde)
            );
        }

        if (fechaHasta != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaHasta)
            );
        }

        // Ejecutar consulta paginada
        Page<CuentaPorCobrar> cuentasPage = cuentaPorCobrarRepository.findAll(spec, pageable);

        // Convertir a DTO
        return cuentasPage.map(this::convertirADto);
    }

    private CuentaPorCobrarDTO convertirADto(CuentaPorCobrar cuenta) {
        CuentaPorCobrarDTO dto = new CuentaPorCobrarDTO();
        dto.setId(cuenta.getId());
        dto.setFacturaId(cuenta.getFactura().getId());
        dto.setNumeroFactura(cuenta.getFactura().getConsecutivo());
        dto.setClienteId(cuenta.getCliente().getId());
        dto.setClienteNombre(cuenta.getCliente().getRazonSocial());
        dto.setClienteIdentificacion(cuenta.getCliente().getNumeroIdentificacion());
        dto.setFechaEmision(cuenta.getFechaEmision());
        dto.setFechaVencimiento(cuenta.getFechaVencimiento());
        dto.setMontoOriginal(cuenta.getMontoOriginal());
        dto.setSaldo(cuenta.getSaldo());
        dto.setEstado(cuenta.getEstado().toString());
        dto.setDiasMora(cuenta.getDiasMora());

        // Calcular monto abonado
        BigDecimal montoAbonado = cuenta.getMontoOriginal().subtract(cuenta.getSaldo());
        dto.setMontoAbonado(montoAbonado);

        return dto;
    }
}