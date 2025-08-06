package com.snnsoluciones.backnathbitpos.dto.auth;

import com.snnsoluciones.backnathbitpos.dto.usuario.AccesoDTO;
import com.snnsoluciones.backnathbitpos.dto.usuario.UsuarioDTO;
import com.snnsoluciones.backnathbitpos.enums.TipoAcceso;
import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UsuarioDTO usuario;
    private TipoAcceso tipoAcceso;
    private AccesoDTO accesoDirecto;
    private List<AccesoDTO> accesosDisponibles;
    private boolean requiereSeleccion;
}
