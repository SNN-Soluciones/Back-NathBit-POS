package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteUbicacionRepository;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServiceImpl implements ClienteService {
    
    private final ClienteRepository clienteRepository;
    private final ClienteUbicacionRepository ubicacionRepository;
    private final ClienteExoneracionRepository exoneracionRepository;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    private static final int MAX_EMAILS = 4;
    
    @Override
    public Cliente crear(Cliente cliente) {
        log.info("Creando nuevo cliente: {} - {}", 
            cliente.getTipoIdentificacion(), cliente.getNumeroIdentificacion());
        
        // Validaciones
        validarEmailsFormato(cliente.getEmails());
        if (cliente.getTelefonoNumero() != null || cliente.getTelefonoCodigoPais() != null) {
            validarTelefonos(cliente.getTelefonoCodigoPais(), cliente.getTelefonoNumero());
        }
        
        // Verificar si ya existe
        if (existeCliente(cliente.getSucursal().getId(), 
                         cliente.getNumeroIdentificacion(), 
                         cliente.getEmails())) {
            throw new IllegalArgumentException(
                "Ya existe un cliente con esa identificación y emails en esta sucursal"
            );
        }
        
        return clienteRepository.save(cliente);
    }
    
    @Override
    public Cliente actualizar(Long id, Cliente clienteActualizado) {
        log.info("Actualizando cliente ID: {}", id);
        
        Cliente clienteExistente = obtenerPorId(id);
        
        // Validaciones
        validarEmailsFormato(clienteActualizado.getEmails());
        if (clienteActualizado.getTelefonoNumero() != null || 
            clienteActualizado.getTelefonoCodigoPais() != null) {
            validarTelefonos(clienteActualizado.getTelefonoCodigoPais(), 
                           clienteActualizado.getTelefonoNumero());
        }
        
        // Verificar unicidad si cambió identificación o emails
        if (!clienteExistente.getNumeroIdentificacion().equals(clienteActualizado.getNumeroIdentificacion()) ||
            !clienteExistente.getEmails().equals(clienteActualizado.getEmails())) {
            
            if (existeCliente(clienteExistente.getSucursal().getId(),
                            clienteActualizado.getNumeroIdentificacion(),
                            clienteActualizado.getEmails())) {
                throw new IllegalArgumentException(
                    "Ya existe otro cliente con esa identificación y emails"
                );
            }
        }
        
        // Actualizar campos
        clienteExistente.setTipoIdentificacion(clienteActualizado.getTipoIdentificacion());
        clienteExistente.setNumeroIdentificacion(clienteActualizado.getNumeroIdentificacion());
        clienteExistente.setRazonSocial(clienteActualizado.getRazonSocial());
        clienteExistente.setEmails(clienteActualizado.getEmails());
        clienteExistente.setTelefonoCodigoPais(clienteActualizado.getTelefonoCodigoPais());
        clienteExistente.setTelefonoNumero(clienteActualizado.getTelefonoNumero());
        clienteExistente.setPermiteCredito(clienteActualizado.getPermiteCredito());
        clienteExistente.setObservaciones(clienteActualizado.getObservaciones());
        
        return clienteRepository.save(clienteExistente);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + id));
    }
    
    @Override
    public void eliminar(Long id) {
        log.info("Eliminando cliente ID: {}", id);
        Cliente cliente = obtenerPorId(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }
    
    @Override
    public void activarDesactivar(Long id, boolean activo) {
        log.info("Cambiando estado del cliente ID: {} a {}", id, activo ? "activo" : "inactivo");
        Cliente cliente = obtenerPorId(id);
        cliente.setActivo(activo);
        clienteRepository.save(cliente);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Cliente> buscarPorSucursal(Long sucursalId, String busqueda, Pageable pageable) {
        return clienteRepository.buscarPorSucursal(sucursalId, busqueda, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Cliente> buscarPorIdentificacion(Long sucursalId, String numeroIdentificacion) {
        return clienteRepository.findBySucursalIdAndNumeroIdentificacionAndActivoTrue(
            sucursalId, numeroIdentificacion
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public Cliente buscarPorIdentificacionYEmails(Long sucursalId, String numeroIdentificacion, String emails) {
        return clienteRepository.findBySucursalIdAndNumeroIdentificacionAndEmails(
            sucursalId, numeroIdentificacion, emails
        ).orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Cliente> obtenerClientesConExoneracion(Long sucursalId) {
        return clienteRepository.findClientesConExoneracionActiva(sucursalId);
    }
    
    // Gestión de ubicación
    @Override
    public ClienteUbicacion guardarUbicacion(Long clienteId, ClienteUbicacion ubicacion) {
        log.info("Guardando ubicación para cliente ID: {}", clienteId);

        Cliente cliente = obtenerPorId(clienteId);

        // Verificar si ya tiene ubicación
        ClienteUbicacion ubicacionExistente = ubicacionRepository.findByClienteId(clienteId)
            .orElse(null);

        if (ubicacionExistente != null) {
            // Actualizar existente
            ubicacionExistente.setProvincia(ubicacion.getProvincia());
            ubicacionExistente.setCanton(ubicacion.getCanton());
            ubicacionExistente.setDistrito(ubicacion.getDistrito());
            ubicacionExistente.setBarrio(ubicacion.getBarrio());
            ubicacionExistente.setOtrasSenas(ubicacion.getOtrasSenas());
            return ubicacionRepository.save(ubicacionExistente);
        } else {
            // Crear nueva
            ubicacion.setCliente(cliente);
            return ubicacionRepository.save(ubicacion);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteUbicacion obtenerUbicacion(Long clienteId) {
        return ubicacionRepository.findByClienteId(clienteId)
            .orElse(null);
    }

    @Override
    public void eliminarUbicacion(Long clienteId) {
        log.info("Eliminando ubicación del cliente ID: {}", clienteId);

        ClienteUbicacion ubicacion = ubicacionRepository.findByClienteId(clienteId)
            .orElseThrow(() -> new IllegalArgumentException(
                "El cliente no tiene ubicación registrada"
            ));

        ubicacionRepository.delete(ubicacion);
    }

    // Gestión de exoneraciones
    @Override
    public ClienteExoneracion agregarExoneracion(Long clienteId, ClienteExoneracion exoneracion) {
        log.info("Agregando exoneración para cliente ID: {}", clienteId);

        Cliente cliente = obtenerPorId(clienteId);

        // Verificar si el número de documento ya existe
        if (exoneracionRepository.existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
            exoneracion.getNumeroDocumento(), clienteId)) {
            throw new IllegalArgumentException(
                "Ya existe una exoneración activa con ese número de documento"
            );
        }

        exoneracion.setCliente(cliente);
        exoneracion.setActivo(true);

        // Actualizar flag en cliente
        cliente.setTieneExoneracion(true);
        clienteRepository.save(cliente);

        return exoneracionRepository.save(exoneracion);
    }

    @Override
    public ClienteExoneracion actualizarExoneracion(Long exoneracionId, ClienteExoneracion exoneracionActualizada) {
        log.info("Actualizando exoneración ID: {}", exoneracionId);

        ClienteExoneracion exoneracionExistente = exoneracionRepository.findById(exoneracionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Exoneración no encontrada: " + exoneracionId
            ));

        // Verificar número documento si cambió
        if (!exoneracionExistente.getNumeroDocumento().equals(exoneracionActualizada.getNumeroDocumento())) {
            if (exoneracionRepository.existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
                exoneracionActualizada.getNumeroDocumento(),
                exoneracionExistente.getCliente().getId())) {
                throw new IllegalArgumentException(
                    "Ya existe otra exoneración activa con ese número de documento"
                );
            }
        }

        // Actualizar campos
        exoneracionExistente.setTipoDocumento(exoneracionActualizada.getTipoDocumento());
        exoneracionExistente.setNumeroDocumento(exoneracionActualizada.getNumeroDocumento());
        exoneracionExistente.setNombreInstitucion(exoneracionActualizada.getNombreInstitucion());
        exoneracionExistente.setFechaEmision(exoneracionActualizada.getFechaEmision());
        exoneracionExistente.setFechaVencimiento(exoneracionActualizada.getFechaVencimiento());
        exoneracionExistente.setPorcentajeExoneracion(exoneracionActualizada.getPorcentajeExoneracion());
        exoneracionExistente.setCategoriaCompra(exoneracionActualizada.getCategoriaCompra());
        exoneracionExistente.setMontoMaximo(exoneracionActualizada.getMontoMaximo());
        exoneracionExistente.setObservaciones(exoneracionActualizada.getObservaciones());

        return exoneracionRepository.save(exoneracionExistente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteExoneracion> obtenerExoneracionesVigentes(Long clienteId) {
        return exoneracionRepository.findExoneracionesVigentes(clienteId, LocalDate.now());
    }

    @Override
    public void desactivarExoneracion(Long exoneracionId) {
        log.info("Desactivando exoneración ID: {}", exoneracionId);

        ClienteExoneracion exoneracion = exoneracionRepository.findById(exoneracionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Exoneración no encontrada: " + exoneracionId
            ));

        exoneracion.setActivo(false);
        exoneracionRepository.save(exoneracion);

        // Verificar si el cliente tiene más exoneraciones activas
        Cliente cliente = exoneracion.getCliente();
        List<ClienteExoneracion> exoneracionesActivas =
            exoneracionRepository.findByClienteIdAndActivoTrue(cliente.getId());

        if (exoneracionesActivas.isEmpty()) {
            cliente.setTieneExoneracion(false);
            clienteRepository.save(cliente);
        }
    }

    // Validaciones
    @Override
    @Transactional(readOnly = true)
    public boolean existeCliente(Long sucursalId, String numeroIdentificacion, String emails) {
        return clienteRepository.existsBySucursalIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
            sucursalId, numeroIdentificacion, emails
        );
    }

    @Override
    public void validarEmailsFormato(String emails) {
        if (emails == null || emails.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un email");
        }

        String[] emailArray = emails.split(",");

        if (emailArray.length > MAX_EMAILS) {
            throw new IllegalArgumentException(
                "No puede registrar más de " + MAX_EMAILS + " emails"
            );
        }

        for (String email : emailArray) {
            String emailTrimmed = email.trim();
            if (!EMAIL_PATTERN.matcher(emailTrimmed).matches()) {
                throw new IllegalArgumentException(
                    "Email inválido: " + emailTrimmed
                );
            }
        }
    }

    @Override
    public void validarTelefonos(String codigoPais, String numero) {
        if ((codigoPais == null && numero != null) ||
            (codigoPais != null && numero == null)) {
            throw new IllegalArgumentException(
                "Debe proporcionar tanto el código de país como el número de teléfono"
            );
        }

        if (codigoPais != null) {
            if (codigoPais.length() < 1 || codigoPais.length() > 3) {
                throw new IllegalArgumentException(
                    "El código de país debe tener entre 1 y 3 dígitos"
                );
            }

            if (!codigoPais.matches("\\d+")) {
                throw new IllegalArgumentException(
                    "El código de país debe contener solo números"
                );
            }
        }

        if (numero != null) {
            if (numero.length() < 8 || numero.length() > 20) {
                throw new IllegalArgumentException(
                    "El número de teléfono debe tener entre 8 y 20 dígitos"
                );
            }

            if (!numero.matches("\\d+")) {
                throw new IllegalArgumentException(
                    "El número de teléfono debe contener solo números"
                );
            }
        }
    }

    // Utilidades
    @Override
    @Transactional(readOnly = true)
    public long contarClientesPorSucursal(Long sucursalId) {
        return clienteRepository.countBySucursalIdAndActivoTrue(sucursalId);
    }

    @Override
    public void procesarExoneracionesVencidas() {
        log.info("Procesando exoneraciones vencidas");

        int actualizadas = exoneracionRepository.desactivarExoneracionesVencidas(LocalDate.now());

        if (actualizadas > 0) {
            log.info("Se desactivaron {} exoneraciones vencidas", actualizadas);

            // Actualizar flag en clientes afectados
            List<Cliente> clientesConExoneracion =
                clienteRepository.findAll().stream()
                    .filter(c -> c.getTieneExoneracion() &&
                        exoneracionRepository.findByClienteIdAndActivoTrue(c.getId()).isEmpty())
                    .toList();

            for (Cliente cliente : clientesConExoneracion) {
                cliente.setTieneExoneracion(false);
                clienteRepository.save(cliente);
            }
        }
    }
}