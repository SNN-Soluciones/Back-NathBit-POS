package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    
    Usuario crear(Usuario usuario);
    
    Usuario actualizar(Long id, Usuario usuario);
    
    Optional<Usuario> buscarPorId(Long id);
    
    Optional<Usuario> buscarPorEmail(String email);
    
    List<Usuario> listarTodos();
    
    void eliminar(Long id);
    
    boolean existeEmail(String email);

    Optional<Usuario> buscarPorUsername(String username);

    List<Usuario> listarPorEmpresa(Long empresaId);
}