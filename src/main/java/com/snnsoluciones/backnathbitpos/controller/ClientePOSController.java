package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteListDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClientePOSDto;
import com.snnsoluciones.backnathbitpos.dto.cliente.CrearClienteRapidoRequest;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final ModelMapper modelMapper;
    
    @Operation(summary = "Buscar cliente por identificación exacta")
    @GetMapping("/buscar-identificacion")
    public ResponseEntity<ApiResponse<List<ClienteListDTO>>> buscarPorIdentificacion(
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
            
            List<ClienteListDTO> clientesDto = clientes.stream()
                .map(c -> modelMapper.map(c, ClienteListDTO.class))
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

            Page<ClientePOSDto> clientesDto = clientes.map(this::convertirAClientePOSDto);

            return ResponseEntity.ok(ApiResponse.ok(mensaje, clientesDto));

        } catch (Exception e) {
            log.error("Error en búsqueda rápida de clientes: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Error al buscar clientes: " + e.getMessage()));
        }
    }

    private ClientePOSDto convertirAClientePOSDto(Cliente cliente) {
        return ClientePOSDto.builder()
            .id(cliente.getId())
            .tipoIdentificacion(cliente.getTipoIdentificacion().name())
            .numeroIdentificacion(cliente.getNumeroIdentificacion())
            .razonSocial(cliente.getRazonSocial())
            .primerEmail(cliente.getEmails())
            .telefonoNumero(cliente.getTelefonoNumero())
            .activo(cliente.getActivo())
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
    public ResponseEntity<ApiResponse<List<ClienteListDTO>>> clientesFrecuentes(
            @RequestParam Long empresaId,
            @RequestParam(defaultValue = "10") int limite) {
        
        // TODO: Implementar cuando tengamos el histórico de ventas
        return ResponseEntity.ok(ApiResponse.ok(
            "Funcionalidad pendiente de implementar", 
            List.of()
        ));
    }
}