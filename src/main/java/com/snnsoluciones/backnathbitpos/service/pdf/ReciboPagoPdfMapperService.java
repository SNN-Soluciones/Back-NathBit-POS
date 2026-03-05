package com.snnsoluciones.backnathbitpos.service.pdf;

import com.snnsoluciones.backnathbitpos.entity.CuentaPorCobrar;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Pago;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.exception.ResourceNotFoundException;
import com.snnsoluciones.backnathbitpos.repository.PagoRepository;
import com.snnsoluciones.backnathbitpos.service.EmpresaService;
import com.snnsoluciones.backnathbitpos.service.StorageService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReciboPagoPdfMapperService {

    private final PagoRepository pagoRepository;
    private final EmpresaService empresaService;
    private final SucursalService sucursalService;
    private final StorageService storageService;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter FECHA_FORMATO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Transactional(readOnly = true)
    public Map<String, Object> mapearPagoAParametros(Long pagoId) {
        log.info("Mapeando pago ID: {} para recibo", pagoId);

        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + pagoId));

        CuentaPorCobrar cuenta = pago.getCuentaPorCobrar();
        Map<String, Object> p = new HashMap<>();

        // ===== EMPRESA =====
        Empresa empresa = empresaService.buscarPorId(
            cuenta.getEmpresa().getId()
        );

        String nombreComercial = empresa.getNombreComercial() != null
            ? empresa.getNombreComercial()
            : empresa.getNombreRazonSocial();
        p.put("empresa_nombre", nombreComercial);
        p.put("empresa_razon_social", empresa.getNombreRazonSocial());
        p.put("empresa_identificacion", empresa.getIdentificacion());
        p.put("empresa_telefono",
            empresa.getTelefono() != null ? empresa.getTelefono() : "");
        p.put("empresa_email",
            empresa.getEmail() != null ? empresa.getEmail() : "");

        // Logo
        cargarLogo(p, empresa);

        // ===== RECIBO =====
        p.put("numero_recibo", pago.getNumeroRecibo());
        p.put("fecha_pago", pago.getFechaPago().format(FECHA_FORMATO));
        p.put("cajero_nombre", pago.getCajero().getNombre()
            + " " + pago.getCajero().getApellidos());

        // ===== CLIENTE =====
        p.put("cliente_nombre", pago.getCliente().getRazonSocial());
        p.put("cliente_cedula", pago.getCliente().getNumeroIdentificacion());

        // ===== FACTURA REFERENCIADA =====
        String numeroDocumento = "";
        if (cuenta.getFactura() != null) {
            numeroDocumento = cuenta.getFactura().getConsecutivo();
        } else if (cuenta.getFacturaInterna() != null) {
            numeroDocumento = cuenta.getFacturaInterna().getNumero();
        }
        p.put("numero_documento_origen", numeroDocumento);
        p.put("tipo_origen", cuenta.getTipoOrigen()); // ELECTRONICA o INTERNA

        // ===== MONTOS =====
        BigDecimal saldoAnterior = cuenta.getSaldo().add(pago.getMonto());
        p.put("monto_original",
            "₡ " + DECIMAL_FORMAT.format(cuenta.getMontoOriginal()));
        p.put("saldo_anterior",
            "₡ " + DECIMAL_FORMAT.format(saldoAnterior));
        p.put("monto_abono",
            "₡ " + DECIMAL_FORMAT.format(pago.getMonto()));
        p.put("saldo_actual",
            "₡ " + DECIMAL_FORMAT.format(cuenta.getSaldo()));

        // ===== MEDIO DE PAGO =====
        p.put("medio_pago", traducirMedioPago(pago.getMedioPago().name()));

        // ===== REFERENCIA (SINPE/transferencia) =====
        p.put("referencia",
            pago.getReferencia() != null ? pago.getReferencia() : "");

        // ===== OBSERVACIONES =====
        p.put("observaciones",
            pago.getObservaciones() != null ? pago.getObservaciones() : "");

        // ===== ESTADO CUENTA =====
        p.put("estado_cuenta", traducirEstado(cuenta.getEstado().name()));
        p.put("saldo_pendiente",
            cuenta.getSaldo().compareTo(BigDecimal.ZERO) == 0
                ? "CANCELADA TOTALMENTE"
                : "Saldo pendiente: ₡ " + DECIMAL_FORMAT.format(cuenta.getSaldo()));

        p.put("mensaje_cortesia", "¡Gracias por su pago!");

        return p;
    }

    private void cargarLogo(Map<String, Object> p, Empresa empresa) {
        try {
            if (empresa.getLogoUrl() != null && !empresa.getLogoUrl().isBlank()) {
                byte[] logoBytes = storageService.downloadFileAsBytes(empresa.getLogoUrl());
                p.put("logo_empresa", new ByteArrayInputStream(logoBytes));
                p.put("tiene_logo", true);
                return;
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar logo empresa {}: {}", empresa.getId(), e.getMessage());
        }
        p.put("tiene_logo", false);
    }

    private String traducirMedioPago(String codigo) {
        return switch (codigo) {
            case "EFECTIVO"           -> "Efectivo";
            case "TARJETA"            -> "Tarjeta";
            case "TRANSFERENCIA"      -> "Transferencia";
            case "CHEQUE"             -> "Cheque";
            case "SINPE_MOVIL"        -> "SINPE Móvil";
            case "RECAUDADO_TERCEROS" -> "Recaudado por terceros";
            default                   -> codigo;
        };
    }

    private String traducirEstado(String estado) {
        return switch (estado) {
            case "VIGENTE"  -> "Vigente";
            case "PARCIAL"  -> "Parcial";
            case "VENCIDA"  -> "Vencida";
            case "PAGADA"   -> "Pagada";
            default         -> estado;
        };
    }
}