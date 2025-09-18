package com.snnsoluciones.backnathbitpos.dto.pago;

import com.snnsoluciones.backnathbitpos.dto.factura.MedioPagoRequest;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FacturaDesdeMontoRequest {
    private BigDecimal montoTotal;
    private List<MedioPagoRequest> pagos;
}

