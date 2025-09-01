package com.snnsoluciones.backnathbitpos.dto.cliente;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteBusquedaDTO {
    private String numeroIdentificacion;
    private List<ClienteOpcionDTO> opciones; // Todos los clientes con esa identificación
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClienteOpcionDTO {
        private Long id;
        private String razonSocial;
        private List<String> emails;
        private String telefonoCompleto;
        private Boolean tieneExoneracion;
        private String ubicacionResumen; // "San José, Central" o similar
        private Set<ClienteEmailDTO> clienteEmails;
    }
}