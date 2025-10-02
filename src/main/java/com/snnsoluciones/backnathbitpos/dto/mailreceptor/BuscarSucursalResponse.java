package com.snnsoluciones.backnathbitpos.dto.mailreceptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscarSucursalResponse {
    private boolean success;
    private String message;
    private Long sucursalId;
    private Long empresaId;
    private String nombreSucursal;
    private String nombreEmpresa;
    private String razonSocial;
    
    public static BuscarSucursalResponse notFound() {
        return BuscarSucursalResponse.builder()
                .success(false)
                .message("No se encontró sucursal con la cédula y email proporcionados")
                .build();
    }
    
    public static BuscarSucursalResponse found(Long sucursalId, Long empresaId, 
                                               String nombreSucursal, String nombreEmpresa,
                                               String razonSocial) {
        return BuscarSucursalResponse.builder()
                .success(true)
                .message("Sucursal encontrada")
                .sucursalId(sucursalId)
                .empresaId(empresaId)
                .nombreSucursal(nombreSucursal)
                .nombreEmpresa(nombreEmpresa)
                .razonSocial(razonSocial)
                .build();
    }
}