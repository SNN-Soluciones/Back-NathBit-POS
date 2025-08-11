package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClienteService {
    
    // Operaciones CRUD básicas
    Cliente crear(Cliente cliente);
    
    Cliente actualizar(Long id, Cliente cliente);
    
    Cliente obtenerPorId(Long id);
    
    void eliminar(Long id);
    
    void activarDesactivar(Long id, boolean activo);
    
    // Búsquedas
    Page<Cliente> buscarPorSucursal(Long sucursalId, String busqueda, Pageable pageable);
    
    List<Cliente> buscarPorIdentificacion(Long sucursalId, String numeroIdentificacion);
    
    Cliente buscarPorIdentificacionYEmails(Long sucursalId, String numeroIdentificacion, String emails);
    
    List<Cliente> obtenerClientesConExoneracion(Long sucursalId);
    
    // Gestión de ubicación
    ClienteUbicacion guardarUbicacion(Long clienteId, ClienteUbicacion ubicacion);
    
    ClienteUbicacion obtenerUbicacion(Long clienteId);
    
    void eliminarUbicacion(Long clienteId);
    
    // Gestión de exoneraciones
    ClienteExoneracion agregarExoneracion(Long clienteId, ClienteExoneracion exoneracion);
    
    ClienteExoneracion actualizarExoneracion(Long exoneracionId, ClienteExoneracion exoneracion);
    
    List<ClienteExoneracion> obtenerExoneracionesVigentes(Long clienteId);
    
    void desactivarExoneracion(Long exoneracionId);
    
    // Validaciones
    boolean existeCliente(Long sucursalId, String numeroIdentificacion, String emails);
    
    void validarEmailsFormato(String emails);
    
    void validarTelefonos(String codigoPais, String numero);
    
    // Utilidades
    long contarClientesPorSucursal(Long sucursalId);
    
    void procesarExoneracionesVencidas();
}