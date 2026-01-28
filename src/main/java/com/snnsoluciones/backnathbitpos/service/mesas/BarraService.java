// service/mesas/BarraService.java
package com.snnsoluciones.backnathbitpos.service.mesas;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.entity.Barra;
import com.snnsoluciones.backnathbitpos.entity.SillaBarra;
import com.snnsoluciones.backnathbitpos.entity.ZonaMesa;
import com.snnsoluciones.backnathbitpos.enums.EstadoSillaBarra;
import com.snnsoluciones.backnathbitpos.repository.BarraRepository;
import com.snnsoluciones.backnathbitpos.repository.SillaBarraRepository;
import com.snnsoluciones.backnathbitpos.repository.ZonaMesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarraService {
    
    private final BarraRepository barraRepo;
    private final ZonaMesaRepository zonaRepo;
    private final SillaBarraRepository sillaRepo;

    @Transactional
    public BarraResponse crear(Long zonaId, CrearBarraRequest req) {
        ZonaMesa zona = zonaRepo.findById(zonaId)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));

        // Validar código único en zona
        barraRepo.findByZonaIdAndCodigo(zonaId, req.codigo().trim())
            .ifPresent(b -> {
                throw new IllegalArgumentException("Código de barra ya existe en la zona");
            });

        // Crear barra
        Barra barra = Barra.builder()
            .zona(zona)
            .sucursal(zona.getSucursal())
            .codigo(req.codigo().trim())
            .nombre(req.nombre() != null ? req.nombre() : req.codigo())
            .tipoForma(req.tipoForma())
            .cantidadSillas(req.cantidadSillas())
            .orden(req.orden() != null ? req.orden() : 0)
            .activo(true)
            .build();

        // Inicializar sillas automáticamente
        barra.inicializarSillas();

        barraRepo.save(barra);
        
        log.info("Barra creada: {} con {} sillas", barra.getCodigo(), barra.getCantidadSillas());
        
        return toDto(barra);
    }

    @Transactional(readOnly = true)
    public List<BarraResponse> listar(Long zonaId) {
        return barraRepo.findByZonaIdOrderByOrdenAsc(zonaId).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public BarraResponse obtener(Long barraId) {
        Barra barra = barraRepo.findById(barraId)
            .orElseThrow(() -> new EntityNotFoundException("Barra no encontrada"));
        return toDto(barra);
    }

    @Transactional
    public BarraResponse actualizar(Long barraId, ActualizarBarraRequest req) {
        Barra barra = barraRepo.findById(barraId)
            .orElseThrow(() -> new EntityNotFoundException("Barra no encontrada"));

        // Validar código único (si cambió)
        if (!barra.getCodigo().equals(req.codigo().trim())) {
            barraRepo.findByZonaIdAndCodigo(barra.getZona().getId(), req.codigo().trim())
                .ifPresent(b -> {
                    throw new IllegalArgumentException("Código ya existe en la zona");
                });
            barra.setCodigo(req.codigo().trim());
        }

        barra.setNombre(req.nombre() != null ? req.nombre() : req.codigo());
        
        if (req.tipoForma() != null) {
            barra.setTipoForma(req.tipoForma());
        }
        
        if (req.orden() != null) {
            barra.setOrden(req.orden());
        }
        
        if (req.activa() != null) {
            barra.setActivo(req.activa());
        }

        // Si cambió cantidad de sillas, reinicializar
        if (req.cantidadSillas() != null && !req.cantidadSillas().equals(barra.getCantidadSillas())) {
            // Solo permitir si no hay órdenes activas
            long sillasOcupadas = barra.contarSillasOcupadas();
            if (sillasOcupadas > 0) {
                throw new IllegalStateException(
                    "No se puede cambiar cantidad de sillas con sillas ocupadas"
                );
            }
            
            barra.setCantidadSillas(req.cantidadSillas());
            barra.inicializarSillas();
            
            log.info("Barra {} reinicializada con {} sillas", barra.getCodigo(), req.cantidadSillas());
        }

        return toDto(barra);
    }

    @Transactional
    public void eliminar(Long barraId) {
        Barra barra = barraRepo.findById(barraId)
            .orElseThrow(() -> new EntityNotFoundException("Barra no encontrada"));

        // Validar que no haya sillas ocupadas
        if (barra.contarSillasOcupadas() > 0) {
            throw new IllegalStateException("No se puede eliminar barra con sillas ocupadas");
        }

        barraRepo.delete(barra);
        log.info("Barra eliminada: {}", barra.getCodigo());
    }

    // ========== GESTIÓN DE SILLAS ==========

    @Transactional(readOnly = true)
    public List<SillaBarraResponse> listarSillas(Long barraId) {
        return sillaRepo.findByBarraIdOrderByNumeroAsc(barraId).stream()
            .map(this::toSillaDto)
            .toList();
    }

    @Transactional
    public SillaBarraResponse cambiarEstadoSilla(Long sillaId, CambiarEstadoSillaRequest req) {
        SillaBarra silla = sillaRepo.findById(sillaId)
            .orElseThrow(() -> new EntityNotFoundException("Silla no encontrada"));

        switch (req.nuevoEstado()) {
            case DISPONIBLE -> silla.liberar();
            case OCUPADA -> {
                if (req.ordenId() == null) {
                    throw new IllegalArgumentException("ordenId requerido para ocupar silla");
                }
                silla.ocupar(req.ordenId(), req.ordenPersonaId());
            }
            case RESERVADA -> silla.reservar();
        }

        log.info("Silla {}-{} cambió a {}", 
            silla.getBarra().getCodigo(), silla.getNumero(), req.nuevoEstado());

        return toSillaDto(silla);
    }

    @Transactional
    public SillaBarraResponse ocuparSilla(Long sillaId, Long ordenId, Long ordenPersonaId) {
        SillaBarra silla = sillaRepo.findById(sillaId)
            .orElseThrow(() -> new EntityNotFoundException("Silla no encontrada"));

        silla.ocupar(ordenId, ordenPersonaId);
        
        log.info("Silla {}-{} ocupada por orden {} persona {}", 
            silla.getBarra().getCodigo(), silla.getNumero(), ordenId, ordenPersonaId);

        return toSillaDto(silla);
    }

    @Transactional
    public SillaBarraResponse liberarSilla(Long sillaId) {
        SillaBarra silla = sillaRepo.findById(sillaId)
            .orElseThrow(() -> new EntityNotFoundException("Silla no encontrada"));

        silla.liberar();
        
        log.info("Silla {}-{} liberada", 
            silla.getBarra().getCodigo(), silla.getNumero());

        return toSillaDto(silla);
    }

    // ========== MAPPERS ==========

    private BarraResponse toDto(Barra barra) {
        return new BarraResponse(
            barra.getId(),
            barra.getCodigo(),
            barra.getNombre(),
            barra.getTipoForma(),
            barra.getCantidadSillas(),
            barra.getOrden(),
            barra.getActivo(),
            barra.getZona().getId(),
            barra.getSucursal().getId(),
            barra.contarSillasDisponibles(),
            barra.contarSillasOcupadas(),
            barra.getSillas().stream().map(this::toSillaDto).toList(),
            barra.getCreatedAt(),
            barra.getUpdatedAt()
        );
    }

    private SillaBarraResponse toSillaDto(SillaBarra silla) {
        return new SillaBarraResponse(
            silla.getId(),
            silla.getNumero(),
            silla.getEstado(),
            silla.getOrdenPersonaId(),
            silla.getOrdenId(),
            silla.getBarra().getId()
        );
    }
}