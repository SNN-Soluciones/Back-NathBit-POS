package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClienteService {
    
    // Operaciones CRUD básicas
    Cliente crear(ClientePOSDto cliente, Long empresaId);
    
    Cliente actualizar(Long id, ClientePOSDto cliente);
    
    Cliente obtenerPorId(Long id);
    
    void eliminar(Long id);
    
    void activarDesactivar(Long id, boolean activo);

    Page<Cliente> buscarPorEmpresaActivos(Long empresaId, Pageable pageable);
    Page<ClientePOSDto> buscarPorEmpresaActivosDTO(Long empresaId, Pageable pageable);

    // Búsquedas
    Page<Cliente> buscarPorEmpresa(Long empresaId, String busqueda, Pageable pageable);
    Page<ClientePOSDto> buscarPorEmpresaDto(Long empresaId, String busqueda, Pageable pageable);

    List<Cliente> buscarPorIdentificacion(Long empresaId, String numeroIdentificacion);
    
    Cliente buscarPorIdentificacionYEmails(Long empresaId, String numeroIdentificacion, String emails);
    
    List<Cliente> obtenerClientesConExoneracion(Long empresaId);
    
    // Gestión de ubicación
    ClienteUbicacion guardarUbicacion(Long clienteId, ClienteUbicacion ubicacion);
    
    ClienteUbicacion obtenerUbicacion(Long clienteId);
    
    void eliminarUbicacion(Long clienteId);
    
    // Gestión de exoneraciones
    ClienteExoneracion agregarExoneracion(Long clienteId, ClienteExoneracion exoneracion);
    
    ClienteExoneracion actualizarExoneracion(Long exoneracionId, ClienteExoneracion exoneracion);

    ClienteExoneracion agregarExoneracion(Long clienteId, ClienteExoneracion exoneracion, List<String> codigosCabys);

    ClienteExoneracion actualizarExoneracion(Long exoneracionId, ClienteExoneracion exoneracion, List<String> codigosCabys);


    List<ClienteExoneracion> obtenerExoneracionesVigentes(Long clienteId);
    
    void desactivarExoneracion(Long exoneracionId);
    
    // Validaciones
    boolean existeCliente(Long empresaId, String numeroIdentificacion, String emails);
    
    void validarEmailsFormato(String emails);
    
    void validarTelefonos(String codigoPais, String numero);
    
    // Utilidades
    long contarClientesPorEmpresa(Long empresaId);
    
    void procesarExoneracionesVencidas();

    ClienteExoneracion obtenerExoneracionPorId(Long exoneracionId);

    Cliente agregarEmail(Long clienteId, String email);
    Cliente quitarEmail(Long clienteId, String email);
    List<String> obtenerEmails(Long clienteId);
    String obtenerEmailSugerido(Long clienteId);
    void registrarUsoEmail(Long clienteId, String email);
}