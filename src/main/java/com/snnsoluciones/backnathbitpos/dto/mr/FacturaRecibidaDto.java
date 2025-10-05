// ✅ CORRECTO - Usar record (Java 14+)
package com.snnsoluciones.backnathbitpos.dto.mr;

import com.snnsoluciones.backnathbitpos.dto.compra.FacturaXmlDto.DetalleDto;
import com.snnsoluciones.backnathbitpos.dto.factura.FacturaResponse.DetalleFacturaDto;
import com.snnsoluciones.backnathbitpos.dto.proveedor.ProveedorDto;
import lombok.Builder;
import java.util.List;

@Builder
public record FacturaRecibidaDto(
    String claveHacienda,
    String consecutivo,
    String fechaEmision,
    ProveedorDto proveedor,
    boolean proveedorExiste,
    List<DetalleDto> detalles,
    ResumenTotalesDto totales,
    String estadoHacienda,
    boolean puedeAceptar,
    String condicionVenta,
    Integer plazoCredito,
    String medioPago
) {}