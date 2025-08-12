package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.enums.mh.TipoDocumentoExoneracion;
import com.snnsoluciones.backnathbitpos.repository.ClienteExoneracionRepository;
import com.snnsoluciones.backnathbitpos.repository.ClienteRepository;
import com.snnsoluciones.backnathbitpos.service.impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteExoneracionServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteExoneracionRepository exoneracionRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private Cliente cliente;
    private ClienteExoneracion exoneracion;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .id(1L)
                .razonSocial("Cliente Test")
                .tieneExoneracion(false)
                .build();

        exoneracion = ClienteExoneracion.builder()
                .id(1L)
                .cliente(cliente)
                .tipoDocumento(TipoDocumentoExoneracion.EXONERACION)
                .numeroDocumento("EXO-2024-001")
                .nombreInstitucion("MEP")
                .fechaEmision(LocalDate.now().minusDays(30))
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .porcentajeExoneracion(new BigDecimal("13.00"))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Debe agregar exoneración exitosamente")
    void testAgregarExoneracion() {
        // Given
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(exoneracionRepository.existsByNumeroDocumentoAndClienteIdNotAndActivoTrue(
                anyString(), anyLong())).thenReturn(false);
        when(exoneracionRepository.save(any(ClienteExoneracion.class))).thenReturn(exoneracion);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        // When
        ClienteExoneracion resultado = clienteService.agregarExoneracion(1L, exoneracion);

        // Then
        assertNotNull(resultado);
        assertEquals("EXO-2024-001", resultado.getNumeroDocumento());
        assertTrue(cliente.getTieneExoneracion());
        verify(clienteRepository).save(cliente);
        verify(exoneracionRepository).save(exoneracion);
    }

    @Test
    @DisplayName("Debe obtener exoneraciones vigentes")
    void testObtenerExoneracionesVigentes() {
        // Given
        List<ClienteExoneracion> exoneraciones = Arrays.asList(exoneracion);
        when(exoneracionRepository.findExoneracionesVigentes(1L, LocalDate.now()))
                .thenReturn(exoneraciones);

        // When
        List<ClienteExoneracion> resultado = clienteService.obtenerExoneracionesVigentes(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).estaVigente());
    }

    @Test
    @DisplayName("Debe verificar vigencia de exoneración")
    void testExoneracionEstaVigente() {
        // Caso: vigente
        assertTrue(exoneracion.estaVigente());

        // Caso: vencida
        exoneracion.setFechaVencimiento(LocalDate.now().minusDays(1));
        assertFalse(exoneracion.estaVigente());

        // Caso: inactiva
        exoneracion.setFechaVencimiento(LocalDate.now().plusDays(30));
        exoneracion.setActivo(false);
        assertFalse(exoneracion.estaVigente());
    }

    @Test
    @DisplayName("Debe procesar exoneraciones vencidas")
    void testProcesarExoneracionesVencidas() {
        // Given
        cliente.setTieneExoneracion(true); // Importante: el cliente debe tener exoneración

        when(exoneracionRepository.desactivarExoneracionesVencidas(any(LocalDate.class)))
            .thenReturn(3);
        when(clienteRepository.findAll()).thenReturn(Arrays.asList(cliente));
        when(exoneracionRepository.findByClienteIdAndActivoTrue(1L))
            .thenReturn(List.of()); // Retorna lista vacía para simular que no hay exoneraciones activas

        // When
        clienteService.procesarExoneracionesVencidas();

        // Then
        verify(exoneracionRepository).desactivarExoneracionesVencidas(any(LocalDate.class));
        verify(clienteRepository).findAll();
        verify(exoneracionRepository).findByClienteIdAndActivoTrue(1L);

        // Verificar que se actualiza el cliente
        assertFalse(cliente.getTieneExoneracion());
        verify(clienteRepository).save(cliente);
    }
}