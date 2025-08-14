package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.sucursal.CrearSucursalCompletaRequest;
import com.snnsoluciones.backnathbitpos.dto.sucursal.CrearSucursalCompletaResponse;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalRequest;
import com.snnsoluciones.backnathbitpos.dto.terminal.TerminalResponse;
import com.snnsoluciones.backnathbitpos.entity.Empresa;
import com.snnsoluciones.backnathbitpos.entity.Sucursal;
import com.snnsoluciones.backnathbitpos.entity.Terminal;
import com.snnsoluciones.backnathbitpos.enums.TipoImpresion;
import com.snnsoluciones.backnathbitpos.repository.EmpresaRepository;
import com.snnsoluciones.backnathbitpos.repository.SucursalRepository;
import com.snnsoluciones.backnathbitpos.repository.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SucursalCreacionService {

    private final SucursalRepository sucursalRepository;
    private final TerminalRepository terminalRepository;
    private final EmpresaRepository empresaRepository;
    private final UbicacionService ubicacionService;

    @Transactional
    public CrearSucursalCompletaResponse crearSucursalCompleta(CrearSucursalCompletaRequest request) {
        log.info("=== INICIANDO PROCESO DE CREACIÓN DE SUCURSAL ===");

        try {
            // 1. Validar datos
            log.info("1. Validando datos de entrada...");
            validarDatosSucursal(request);
            log.info("   ✅ Datos validados correctamente");

            // 2. Buscar empresa
            log.info("2. Buscando empresa ID: {}", request.getEmpresaId());
            Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
            log.info("   ✅ Empresa encontrada: {}", empresa.getNombreComercial());

            // 3. Crear sucursal
            log.info("3. Creando sucursal...");
            Sucursal sucursal = crearSucursal(request, empresa);
            log.info("   ✅ Sucursal creada con ID: {}", sucursal.getId());

            // 4. Crear terminales si existen
            List<Terminal> terminalesCreadas = new ArrayList<>();
            if (request.getTerminales() != null && !request.getTerminales().isEmpty()) {
                log.info("4. Creando {} terminales...", request.getTerminales().size());
                terminalesCreadas = crearTerminales(sucursal, request.getTerminales());
                log.info("   ✅ {} terminales creadas exitosamente", terminalesCreadas.size());
            } else {
                log.info("4. No hay terminales para crear");
            }

            // 5. Construir respuesta
            CrearSucursalCompletaResponse response = construirRespuesta(sucursal, empresa, terminalesCreadas);

            log.info("=== SUCURSAL CREADA EXITOSAMENTE ===");
            return response;

        } catch (RuntimeException e) {
            log.error("❌ Error de negocio: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado creando sucursal", e);
            throw new RuntimeException("Error al crear la sucursal: " + e.getMessage(), e);
        }
    }

    private void validarDatosSucursal(CrearSucursalCompletaRequest request) {
        // Validar que no exista el número de sucursal en la empresa
        if (request.getNumeroSucursal() != null && !request.getNumeroSucursal().isEmpty()) {
            if (sucursalRepository.existsByNumeroSucursalAndEmpresaId(
                    request.getNumeroSucursal(), request.getEmpresaId())) {
                throw new RuntimeException(
                    "Ya existe una sucursal con el número: " + request.getNumeroSucursal());
            }
        }

        // Validar ubicación completa o nada
        boolean tieneUbicacion = request.getProvinciaId() != null || 
                                request.getCantonId() != null ||
                                request.getDistritoId() != null || 
                                request.getBarrioId() != null;

        if (tieneUbicacion) {
            if (request.getProvinciaId() == null || request.getCantonId() == null ||
                request.getDistritoId() == null || request.getBarrioId() == null) {
                throw new RuntimeException(
                    "Si especifica ubicación, debe incluir provincia, cantón, distrito y barrio");
            }
        }

        // Validar terminales
        if (request.getTerminales() != null && request.getTerminales().size() > 2) {
            throw new RuntimeException("Máximo 2 terminales permitidas por sucursal");
        }
    }

    private Sucursal crearSucursal(CrearSucursalCompletaRequest request, Empresa empresa) {
        Sucursal sucursal = new Sucursal();
        
        // Datos básicos
        sucursal.setNombre(request.getNombre());
        sucursal.setTelefono(request.getTelefono());
        sucursal.setEmail(request.getEmail());
        sucursal.setEmpresa(empresa);
        sucursal.setModoFacturacion(request.getModoFacturacion());
        sucursal.setActiva(request.getActiva());

        // Generar número de sucursal si no se proporciona
        if (request.getNumeroSucursal() == null || request.getNumeroSucursal().isEmpty()) {
            Integer maxNumero = sucursalRepository.findMaxNumeroSucursalByEmpresaId(empresa.getId());
            int siguiente = (maxNumero != null ? maxNumero : 0) + 1;
            sucursal.setNumeroSucursal(String.format("%03d", siguiente));
        } else {
            sucursal.setNumeroSucursal(request.getNumeroSucursal());
        }

        // Ubicación si existe
        if (request.getProvinciaId() != null) {
            sucursal.setProvincia(
                ubicacionService.buscarProvinciaPorId(request.getProvinciaId())
                    .orElseThrow(() -> new RuntimeException("Provincia no encontrada"))
            );
            sucursal.setCanton(
                ubicacionService.buscarCantonPorId(request.getCantonId())
                    .orElseThrow(() -> new RuntimeException("Cantón no encontrado"))
            );
            sucursal.setDistrito(
                ubicacionService.buscarDistritoPorId(request.getDistritoId())
                    .orElseThrow(() -> new RuntimeException("Distrito no encontrado"))
            );
            sucursal.setBarrio(
                ubicacionService.buscarBarrioPorId(request.getBarrioId())
                    .orElseThrow(() -> new RuntimeException("Barrio no encontrado"))
            );
            sucursal.setOtrasSenas(request.getOtrasSenas());
        }

        return sucursalRepository.save(sucursal);
    }

    private List<Terminal> crearTerminales(Sucursal sucursal, List<TerminalRequest> terminalesRequest) {
        List<Terminal> terminales = new ArrayList<>();

        for (TerminalRequest terminalReq : terminalesRequest) {
            Terminal terminal = new Terminal();
            
            // Generar número de terminal si no se proporciona
            if (terminalReq.getNumeroTerminal() == null || terminalReq.getNumeroTerminal().isEmpty()) {
                Integer maxNumero = terminalRepository.findMaxNumeroTerminalBySucursalId(sucursal.getId());
                int siguiente = (maxNumero != null ? maxNumero : 0) + 1;
                terminal.setNumeroTerminal(String.format("%05d", siguiente));
            } else {
                terminal.setNumeroTerminal(terminalReq.getNumeroTerminal());
            }

            terminal.setNombre(terminalReq.getNombre());
            terminal.setDescripcion(terminalReq.getDescripcion());
            terminal.setActiva(terminalReq.getActiva() != null ? terminalReq.getActiva() : true);
            terminal.setImprimirAutomatico(terminalReq.getImprimirAutomatico() != null ? 
                terminalReq.getImprimirAutomatico() : false);
            terminal.setTipoImpresion(terminalReq.getTipoImpresion() != null ? 
                terminalReq.getTipoImpresion() : TipoImpresion.CARTA);
            terminal.setSucursal(sucursal); // IMPORTANTE: Asignar la sucursal

            // Establecer consecutivos
            terminal.setConsecutivoOrdenPedido(terminalReq.getConsecutivoOrdenPedido() != null ? 
                terminalReq.getConsecutivoOrdenPedido() : 0);
            terminal.setConsecutivoFacturaElectronica(terminalReq.getConsecutivoFacturaElectronica() != null ? 
                terminalReq.getConsecutivoFacturaElectronica() : 1);
            terminal.setConsecutivoTiqueteElectronico(terminalReq.getConsecutivoTiqueteElectronico() != null ? 
                terminalReq.getConsecutivoTiqueteElectronico() : 1);
            terminal.setConsecutivoNotaCredito(terminalReq.getConsecutivoNotaCredito() != null ? 
                terminalReq.getConsecutivoNotaCredito() : 1);
            terminal.setConsecutivoNotaDebito(terminalReq.getConsecutivoNotaDebito() != null ? 
                terminalReq.getConsecutivoNotaDebito() : 1);
            terminal.setConsecutivoFacturaCompra(terminalReq.getConsecutivoFacturaCompra() != null ? 
                terminalReq.getConsecutivoFacturaCompra() : 1);
            terminal.setConsecutivoFacturaExportacion(terminalReq.getConsecutivoFacturaExportacion() != null ? 
                terminalReq.getConsecutivoFacturaExportacion() : 1);
            terminal.setConsecutivoReciboPago(terminalReq.getConsecutivoReciboPago() != null ? 
                terminalReq.getConsecutivoReciboPago() : 1);
            terminal.setConsecutivoTiqueteInterno(terminalReq.getConsecutivoTiqueteInterno() != null ? 
                terminalReq.getConsecutivoTiqueteInterno() : 0);
            terminal.setConsecutivoFacturaInterna(terminalReq.getConsecutivoFacturaInterna() != null ? 
                terminalReq.getConsecutivoFacturaInterna() : 0);
            terminal.setConsecutivoProforma(terminalReq.getConsecutivoProforma() != null ? 
                terminalReq.getConsecutivoProforma() : 0);

            Terminal terminalGuardada = terminalRepository.save(terminal);
            terminales.add(terminalGuardada);
            
            log.info("   Terminal '{}' creada con número: {}", 
                terminalGuardada.getNombre(), terminalGuardada.getNumeroTerminal());
        }

        return terminales;
    }

    private CrearSucursalCompletaResponse construirRespuesta(Sucursal sucursal, Empresa empresa, 
                                                              List<Terminal> terminales) {
        List<TerminalResponse> terminalesResponse = new ArrayList<>();
        
        for (Terminal terminal : terminales) {
            TerminalResponse terminalResp = new TerminalResponse();
            terminalResp.setId(terminal.getId());
            terminalResp.setNumeroTerminal(terminal.getNumeroTerminal());
            terminalResp.setNombre(terminal.getNombre());
            terminalResp.setActiva(terminal.getActiva());
            terminalResp.setSucursalId(sucursal.getId());
            terminalesResponse.add(terminalResp);
        }

        return CrearSucursalCompletaResponse.builder()
            .sucursalId(sucursal.getId())
            .nombre(sucursal.getNombre())
            .numeroSucursal(sucursal.getNumeroSucursal())
            .empresaId(empresa.getId())
            .empresaNombre(empresa.getNombreComercial())
            .modoFacturacion(sucursal.getModoFacturacion())
            .activa(sucursal.getActiva())
            .terminalesCreadas(terminales.size())
            .terminales(terminalesResponse)
            .mensaje("Sucursal creada exitosamente con " + terminales.size() + " terminales")
            .createdAt(sucursal.getCreatedAt())
            .build();
    }
}