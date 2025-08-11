package com.snnsoluciones.backnathbitpos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snnsoluciones.backnathbitpos.controller.ClienteController;
import com.snnsoluciones.backnathbitpos.dto.cliente.*;
import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.TipoIdentificacion;
import com.snnsoluciones.backnathbitpos.mappers.ClienteMapper;
import com.snnsoluciones.backnathbitpos.service.ClienteService;
import com.snnsoluciones.backnathbitpos.service.SucursalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClienteControllerUnitTest {

    @Mock
    private ClienteService clienteService;

    @Mock
    private SucursalService sucursalService;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteController clienteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc con PageableHandlerMethodArgumentResolver
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCrearCliente() throws Exception {
        // Given
        Long sucursalId = 1L;
        ClienteCreateDTO dto = ClienteCreateDTO.builder()
            .tipoIdentificacion(TipoIdentificacion.CEDULA_FISICA)
            .numeroIdentificacion("123456789")
            .razonSocial("Juan Pérez")
            .emails("juan@test.com")
            .build();

        Sucursal sucursal = new Sucursal();
        sucursal.setId(sucursalId);
        sucursal.setNombre("Sucursal Test");

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNumeroIdentificacion("123456789");
        cliente.setRazonSocial("Juan Pérez");

        ClienteDTO clienteDTO = ClienteDTO.builder()
            .id(1L)
            .numeroIdentificacion("123456789")
            .razonSocial("Juan Pérez")
            .build();

        // When
        when(sucursalService.buscarPorId(sucursalId)).thenReturn(Optional.of(sucursal));
        when(clienteMapper.toEntity(any(ClienteCreateDTO.class))).thenReturn(cliente);
        when(clienteService.crear(any(Cliente.class))).thenReturn(cliente);
        when(clienteMapper.toDTO(any(Cliente.class))).thenReturn(clienteDTO);

        // Then
        mockMvc.perform(post("/api/clientes")
                .param("sucursalId", sucursalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .principal(new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CAJERO")))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Cliente creado exitosamente"))
            .andExpect(jsonPath("$.data.numeroIdentificacion").value("123456789"));
    }

    @Test
    void testCrearClienteSucursalNoExiste() throws Exception {
        // Given
        Long sucursalId = 999L;
        ClienteCreateDTO dto = ClienteCreateDTO.builder()
            .tipoIdentificacion(TipoIdentificacion.CEDULA_FISICA)
            .numeroIdentificacion("123456789")
            .razonSocial("Juan Pérez")
            .emails("juan@test.com")
            .build();

        // When
        when(sucursalService.buscarPorId(sucursalId)).thenReturn(Optional.empty());

        // Then
        mockMvc.perform(post("/api/clientes")
                .param("sucursalId", sucursalId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .principal(new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CAJERO")))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Sucursal no encontrada"));
    }

    @Test
    void testBuscarPorIdentificacion() throws Exception {
        // Given
        Long sucursalId = 1L;
        String numeroIdentificacion = "123456789";

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNumeroIdentificacion(numeroIdentificacion);
        cliente.setRazonSocial("Juan Pérez");

        ClienteBusquedaDTO.ClienteOpcionDTO opcion = ClienteBusquedaDTO.ClienteOpcionDTO.builder()
            .id(1L)
            .razonSocial("Juan Pérez")
            .emails(Arrays.asList("juan@test.com"))
            .build();

        // When
        when(sucursalService.finById(sucursalId)).thenReturn(Optional.of(new Sucursal()));
        when(clienteService.buscarPorIdentificacion(sucursalId, numeroIdentificacion))
            .thenReturn(Arrays.asList(cliente));
        when(clienteMapper.toOpcionDTO(cliente)).thenReturn(opcion);

        // Then
        mockMvc.perform(get("/api/clientes/buscar-identificacion/{numeroIdentificacion}", numeroIdentificacion)
                .param("sucursalId", sucursalId.toString())
                .principal(new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CAJERO")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.numeroIdentificacion").value(numeroIdentificacion))
            .andExpect(jsonPath("$.data.opciones[0].razonSocial").value("Juan Pérez"));
    }

    @Test
    void testObtenerClientePorId() throws Exception {
        // Given
        Long clienteId = 1L;

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setRazonSocial("Juan Pérez");

        ClienteDTO clienteDTO = ClienteDTO.builder()
            .id(clienteId)
            .razonSocial("Juan Pérez")
            .build();

        // When
        when(clienteService.obtenerPorId(clienteId)).thenReturn(cliente);
        when(clienteMapper.toDTO(cliente)).thenReturn(clienteDTO);

        // Then
        mockMvc.perform(get("/api/clientes/{id}", clienteId)
                .principal(new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_CAJERO")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(clienteId))
            .andExpect(jsonPath("$.data.razonSocial").value("Juan Pérez"));
    }

    @Test
    void testEliminarCliente() throws Exception {
        // Given
        Long clienteId = 1L;
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);

        // When
        when(clienteService.obtenerPorId(clienteId)).thenReturn(cliente);

        // Then
        mockMvc.perform(delete("/api/clientes/{id}", clienteId)
                .principal(new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Cliente eliminado exitosamente"));
    }

    @Test
    void testObtenerResumen() throws Exception {
        // Given
        Long sucursalId = 1L;
        long totalClientes = 100L;
        List<Cliente> clientesConExoneracion = Arrays.asList(new Cliente(), new Cliente());

        // When
        when(clienteService.contarClientesPorSucursal(sucursalId)).thenReturn(totalClientes);
        when(clienteService.obtenerClientesConExoneracion(sucursalId)).thenReturn(clientesConExoneracion);

        // Then
        mockMvc.perform(get("/api/clientes/resumen")
                .param("sucursalId", sucursalId.toString())
                .principal(new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalClientes").value(100))
            .andExpect(jsonPath("$.data.clientesConExoneracion").value(2));
    }
}