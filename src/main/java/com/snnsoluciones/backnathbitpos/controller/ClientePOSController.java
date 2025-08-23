package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.CrearClienteRapidoRequest;
import com.snnsoluciones.backnathbitpos.dto.cliente.ExoneracionClienteDto;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.CodigoCAByS;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.EmpresaCAByS;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionCabysRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.ProductoCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/pos/clientes")
@RequiredArgsConstructor
@Tag(name = "POS - Clientes", description = "Búsqueda rápida de clientes para punto de venta")
@PreAuthorize("hasAnyRole('CAJERO', 'MESERO', 'JEFE_CAJAS', 'ADMIN', 'SUPER_ADMIN')")
public class ClientePOSController {
    
    private final ClienteService clienteService;
    private final ClienteExoneracionCabysRepository exoneracionCabysRepository;
    private final ClienteExoneracionRepository exoneracionRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoCrudService productoCrudService;
    private final ModelMapper modelMapper;
    
    @Operation(summary = "Buscar cliente por identificación exacta")
    @GetMapping("/buscar-identificacion")
    public ResponseEntity<ApiResponse<List<ClientePOSDto>>> buscarPorIdentificacion(
            @RequestParam Long empresaId,
            @RequestParam String identificacion) {
        
        try {
            List<Cliente> clientes = clienteService.buscarPorIdentificacion(empresaId, identificacion);
            
            if (clientes.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok(
                    "No se encontró cliente con identificación: " + identificacion,
                    List.of()
                ));
            }
            
            List<ClientePOSDto> clientesDto = clientes.stream()
                .map(c -> modelMapper.map(c, ClientePOSDto.class))
                .collect(Collectors.toList());
            
            String mensaje = clientes.size() == 1 
                ? "Cliente encontrado" 
                : "Se encontraron " + clientes.size() + " clientes";
                
            return ResponseEntity.ok(ApiResponse.ok(mensaje, clientesDto));
            
        } catch (Exception e) {
            log.error("Error buscando cliente por identificación: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al buscar cliente: " + e.getMessage()));
        }
    }

    @GetMapping("/listar-pos")
    public ResponseEntity<ApiResponse<Page<ClientePOSDto>>> listarPorPos(
        @RequestParam Long empresaId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

        try {
            Page<Cliente> clientes = clienteService.buscarPorEmpresaActivos(
                empresaId,
                PageRequest.of(page, size)
            );

            String mensaje = clientes.isEmpty()
                ? "No se encontraron clientes"
                : "Se encontraron " + clientes.getTotalElements() + " clientes";

            return ResponseEntity.ok(ApiResponse.ok(mensaje, clientes.map(
                this::convertirAClientePOSDto)));
        } catch (Exception e) {
            log.error("Error en búsqueda rápida de clientes: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al buscar clientes: " + e.getMessage()));
        }
    }

    @Operation(summary = "Búsqueda rápida de clientes (cédula, nombre, email)")
    @GetMapping("/busqueda-rapida")
    public ResponseEntity<ApiResponse<Page<ClientePOSDto>>> busquedaRapida(
            @RequestParam Long empresaId,
            @RequestParam String termino,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            if (termino == null || termino.trim().length() < 3) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El término de búsqueda debe tener al menos 3 caracteres"));
            }

            Page<Cliente> clientes = clienteService.buscarPorEmpresa(
                empresaId,
                termino.trim(),
                PageRequest.of(page, size)
            );

            String mensaje = clientes.isEmpty()
                ? "No se encontraron clientes"
                : "Se encontraron " + clientes.getTotalElements() + " clientes";


            return ResponseEntity.ok(ApiResponse.ok(mensaje, clientes.map(
                this::convertirAClientePOSDto)));

        } catch (Exception e) {
            log.error("Error en búsqueda rápida de clientes: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al buscar clientes: " + e.getMessage()));
        }
    }

    @GetMapping("/validar-exoneracion/cliente")
    public ResponseEntity<ApiResponse<Boolean>> validarExoneracionCliente(
        @RequestParam Long clienteId,
        @RequestParam Long productoId
    ) {
        String cabys = null;
        try {
            // 1) Producto + CAByS
            var producto = productoCrudService.obtenerEntidadPorId(productoId);
            if (producto == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Producto no encontrado: " + productoId));
            }

            cabys = Optional.ofNullable(producto.getEmpresaCabys())
                .map(EmpresaCAByS::getCodigoCabys)
                .map(CodigoCAByS::getCodigo)
                .orElse(null);

            if (cabys == null || !cabys.matches("\\d{13}")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Producto sin CAByS válido (13 dígitos): " + cabys));
            }

            // 2) Cliente
            var clienteOpt = clienteRepository.findById(clienteId);
            if (clienteOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cliente no encontrado: " + clienteId));
            }

            // 3) Exoneración vigente (toma la última activa)
            var exOpt = exoneracionRepository
                .findByClienteIdAndActivoTrue(clienteId); // <-- agrega este método en el repo

            if (exOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.ok("Cliente sin exoneración activa", false));
            }

            var ex = exOpt.get(0);
            if (!ex.estaVigente()) {
                return ResponseEntity.ok(ApiResponse.ok("Exoneración no vigente", false));
            }

            // 4) ¿Aplica para el CAByS?
            boolean aplica = ex.aplicaParaCabys(
                cabys,
                c -> exoneracionCabysRepository.existsByExoneracionIdAndCabys_Codigo(ex.getId(), c)
            );

            String msg = aplica
                ? String.format("Exoneración vigente (%s%%) aplica al CAByS %s",
                ex.getPorcentajeExoneracion(), cabys)
                : (Boolean.TRUE.equals(ex.getPoseeCabys())
                    ? "CAByS no autorizado en la lista de la exoneración"
                    : "La exoneración no aplica a este CAByS");

            return ResponseEntity.ok(ApiResponse.ok(msg, aplica));

        } catch (Exception e) {
            log.error("Error validando exoneración (clienteId={}, cabys={}): {}", clienteId, cabys, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al validar exoneración: " + e.getMessage()));
        }
    }

    private ClientePOSDto convertirAClientePOSDto(Cliente cliente) {
        var exoOpt = cliente.getExoneracionVigente();

        ExoneracionClienteDto exoDto = exoOpt.map(e ->
            ExoneracionClienteDto.builder()
                .vigente(e.estaVigente())
                .tipoDocumento(e.getTipoDocumento() != null ? e.getTipoDocumento().name() : null)
                .numeroDocumento(e.getNumeroDocumento())
                .fechaEmision(e.getFechaEmision())
                .fechaVencimiento(e.getFechaVencimiento())
                .nombreInstitucion(e.getNombreInstitucion())
                .porcentajeExoneracion(e.getPorcentajeExoneracion())
                .codigoAutorizacion(e.getCodigoAutorizacion()) // si tu entidad lo tiene
                .poseeCabys(Boolean.TRUE.equals(e.getPoseeCabys()))
                .categoriaCompra(e.getCategoriaCompra())
                .montoMaximo(e.getMontoMaximo())
                .numeroAutorizacion(e.getNumeroAutorizacion())
                .totalCabysAutorizados(e.getCabysAutorizados().size())
                .build()
        ).orElse(null);

        return ClientePOSDto.builder()
            .id(cliente.getId())
            .tipoIdentificacion(cliente.getTipoIdentificacion())
            .numeroIdentificacion(cliente.getNumeroIdentificacion())
            .razonSocial(cliente.getRazonSocial())
            .emails(cliente.getEmails())
            .telefonoNumero(cliente.getTelefonoNumero())
            .activo(cliente.getActivo())
            .exonerado(exoDto != null)
            .inscritoHacienda(cliente.getInscritoHacienda())
            .tieneExoneracion(cliente.getTieneExoneracion())
            .permiteCredito(cliente.getPermiteCredito())
            .exoneracion(exoDto)
            .build();
    }
    
    @Operation(summary = "Crear cliente rápido desde POS")
    @PostMapping("/crear-rapido")
    public ResponseEntity<ApiResponse<ClienteDTO>> crearClienteRapido(
            @RequestBody CrearClienteRapidoRequest request) {
        
        try {
            // Validaciones básicas
            if (request.getNumeroIdentificacion() == null || request.getNumeroIdentificacion().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La identificación es requerida"));
            }
            
            if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El nombre es requerido"));
            }
            
            // Crear cliente con datos mínimos
            Cliente cliente = new Cliente();
            cliente.setEmpresa(new Empresa());
            cliente.getEmpresa().setId(request.getEmpresaId());
            cliente.setTipoIdentificacion(request.getTipoIdentificacion());
            cliente.setNumeroIdentificacion(request.getNumeroIdentificacion());
            cliente.setRazonSocial(request.getNombre());
            cliente.setEmails(request.getEmail() != null ? request.getEmail() : "sincorreo@pos.com");
            cliente.setTelefonoCodigoPais("506");
            cliente.setTelefonoNumero(request.getTelefono() != null ? request.getTelefono() : "00000000");
            cliente.setPermiteCredito(false);
            cliente.setTieneExoneracion(false);
            
            Cliente clienteCreado = clienteService.crear(cliente);
            ClienteDTO clienteDto = modelMapper.map(clienteCreado, ClienteDTO.class);
            
            return ResponseEntity.ok(ApiResponse.ok("Cliente creado exitosamente", clienteDto));
            
        } catch (Exception e) {
            log.error("Error creando cliente rápido: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al crear cliente: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Obtener clientes frecuentes")
    @GetMapping("/frecuentes")
    public ResponseEntity<ApiResponse<List<ClientePOSDto>>> clientesFrecuentes(
            @RequestParam Long empresaId,
            @RequestParam(defaultValue = "10") int limite) {
        
        // TODO: Implementar cuando tengamos el histórico de ventas
        return ResponseEntity.ok(ApiResponse.ok(
            "Funcionalidad pendiente de implementar", 
            List.of()
        ));
    }
}