package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoIdentificacion;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteUbicacionRepository;
import com.snnsoluciones.backnathbitpos.service.impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

  @Mock
  private ClienteRepository clienteRepository;

  @Mock
  private ClienteUbicacionRepository ubicacionRepository;

  @Mock
  private ClienteExoneracionRepository exoneracionRepository;

  @InjectMocks
  private ClienteServiceImpl clienteService;

  private Cliente cliente;
  private Sucursal sucursal;

  @BeforeEach
  void setUp() {
    sucursal = new Sucursal();
    sucursal.setId(1L);
    sucursal.setNombre("Sucursal Test");

    cliente = Cliente.builder()
        .id(1L)
        .sucursal(sucursal)
        .tipoIdentificacion(TipoIdentificacion.CEDULA_FISICA)
        .numeroIdentificacion("123456789")
        .razonSocial("Juan Pérez")
        .emails("juan@test.com,juan.perez@test.com")
        .telefonoCodigoPais("506")
        .telefonoNumero("88888888")
        .permiteCredito(false)
        .tieneExoneracion(false)
        .activo(true)
        .build();
  }

  @Test
  @DisplayName("Debe crear un cliente exitosamente")
  void testCrearCliente() {
    // Given
    when(clienteRepository.existsBySucursalIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
        anyLong(), anyString(), anyString())).thenReturn(false);
    when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

    // When
    Cliente resultado = clienteService.crear(cliente);

    // Then
    assertNotNull(resultado);
    assertEquals("123456789", resultado.getNumeroIdentificacion());
    assertEquals("Juan Pérez", resultado.getRazonSocial());
    verify(clienteRepository).save(cliente);
  }

  @Test
  @DisplayName("Debe lanzar excepción cuando el cliente ya existe")
  void testCrearClienteDuplicado() {
    // Given
    when(clienteRepository.existsBySucursalIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
        anyLong(), anyString(), anyString())).thenReturn(true);

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.crear(cliente);
    });
    verify(clienteRepository, never()).save(any());
  }

  @Test
  @DisplayName("Debe validar formato de emails correctamente")
  void testValidarEmailsFormato() {
    // Caso válido
    assertDoesNotThrow(() -> {
      clienteService.validarEmailsFormato("email1@test.com,email2@test.com");
    });

    // Casos inválidos
    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarEmailsFormato("");
    });

    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarEmailsFormato("email-invalido");
    });

    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarEmailsFormato("a@test.com,b@test.com,c@test.com,d@test.com,e@test.com");
    });
  }

  @Test
  @DisplayName("Debe validar teléfonos correctamente")
  void testValidarTelefonos() {
    // Caso válido
    assertDoesNotThrow(() -> {
      clienteService.validarTelefonos("506", "88888888");
    });

    // Casos inválidos
    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarTelefonos("506", null);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarTelefonos(null, "88888888");
    });

    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarTelefonos("abcd", "88888888");
    });

    assertThrows(IllegalArgumentException.class, () -> {
      clienteService.validarTelefonos("506", "123");
    });
  }

  @Test
  @DisplayName("Debe buscar clientes por identificación")
  void testBuscarPorIdentificacion() {
    // Given
    List<Cliente> clientes = Arrays.asList(cliente);
    when(clienteRepository.findBySucursalIdAndNumeroIdentificacionAndActivoTrue(1L, "123456789"))
        .thenReturn(clientes);

    // When
    List<Cliente> resultado = clienteService.buscarPorIdentificacion(1L, "123456789");

    // Then
    assertNotNull(resultado);
    assertEquals(1, resultado.size());
    assertEquals("123456789", resultado.get(0).getNumeroIdentificacion());
  }

  @Test
  @DisplayName("Debe actualizar cliente exitosamente")
  void testActualizarCliente() {
    // Given
    Cliente clienteActualizado = Cliente.builder()
        .tipoIdentificacion(TipoIdentificacion.CEDULA_FISICA)
        .numeroIdentificacion("123456789")
        .razonSocial("Juan Pérez Actualizado")
        .emails("nuevo@email.com")
        .build();

    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    when(clienteRepository.existsBySucursalIdAndNumeroIdentificacionAndEmailsAndActivoTrue(
        anyLong(), anyString(), anyString())).thenReturn(false);
    when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

    // When
    Cliente resultado = clienteService.actualizar(1L, clienteActualizado);

    // Then
    assertNotNull(resultado);
    verify(clienteRepository).save(any(Cliente.class));
  }

  @Test
  @DisplayName("Debe eliminar (desactivar) cliente")
  void testEliminarCliente() {
    // Given
    when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
    when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

    // When
    clienteService.eliminar(1L);

    // Then
    assertFalse(cliente.getActivo());
    verify(clienteRepository).save(cliente);
  }

  @Test
  @DisplayName("Debe buscar clientes con paginación")
  void testBuscarPorSucursal() {
    // Given
    Pageable pageable = PageRequest.of(0, 20);
    Page<Cliente> page = new PageImpl<>(Arrays.asList(cliente));
    when(clienteRepository.buscarPorSucursal(1L, "test", pageable)).thenReturn(page);

    // When
    Page<Cliente> resultado = clienteService.buscarPorSucursal(1L, "test", pageable);

    // Then
    assertNotNull(resultado);
    assertEquals(1, resultado.getTotalElements());
  }
}