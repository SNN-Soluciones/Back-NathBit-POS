// MesaService.java
package com.snnsoluciones.backnathbitpos.service.mesas;

import com.snnsoluciones.backnathbitpos.dto.mesas.*;
import com.snnsoluciones.backnathbitpos.entity.Mesa;
import com.snnsoluciones.backnathbitpos.entity.MesaEstadoHist;
import com.snnsoluciones.backnathbitpos.entity.ZonaMesa;
import com.snnsoluciones.backnathbitpos.enums.EstadoMesa;
import com.snnsoluciones.backnathbitpos.repository.MesaEstadoHistRepository;
import com.snnsoluciones.backnathbitpos.repository.MesaRepository;
import com.snnsoluciones.backnathbitpos.repository.ZonaMesaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class MesaService {
  private final MesaRepository mesaRepo;
  private final ZonaMesaRepository zonaRepo;
  private final MesaEstadoHistRepository histRepo;

  @Transactional
  public MesaResponse crear(Long zonaId, CrearMesaRequest req) {
    ZonaMesa zona = zonaRepo.findById(zonaId)
        .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada"));
    mesaRepo.findByZonaIdAndCodigo(zonaId, req.codigo().trim())
        .ifPresent(m -> { throw new IllegalArgumentException("Código de mesa ya existe en la zona"); });

    Mesa m = Mesa.builder()
        .zona(zona).codigo(req.codigo().trim()).nombre(req.nombre())
        .capacidad(req.capacidad() == null ? 2 : req.capacidad())
        .orden(req.orden() == null ? 0 : req.orden())
        .estado(EstadoMesa.LIBRE).activo(true).build();

    mesaRepo.save(m);
    registrarHistorial(m, EstadoMesa.LIBRE, "CREADA", null);
    return toDto(m);
  }

  @Transactional(readOnly = true)
  public List<MesaResponse> listar(Long zonaId) {
    return mesaRepo.findByZonaIdOrderByOrdenAsc(zonaId).stream().map(this::toDto).toList();
  }

  @Transactional
  public MesaResponse actualizar(Long mesaId, ActualizarMesaRequest req) {
    Mesa m = mesaRepo.findById(mesaId)
        .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada"));
    // validar colisión de código en la misma zona
    mesaRepo.findByZonaIdAndCodigo(m.getZona().getId(), req.codigo().trim())
        .filter(x -> !x.getId().equals(mesaId))
        .ifPresent(x -> { throw new IllegalArgumentException("Código ya existe en la zona"); });

    m.setCodigo(req.codigo().trim());
    m.setNombre(req.nombre());
    if (req.capacidad() != null) m.setCapacidad(req.capacidad());
    if (req.orden() != null) m.setOrden(req.orden());
    if (req.activa() != null) m.setActivo(req.activa());

    return toDto(m);
  }

  @Transactional
  public MesaResponse cambiarEstado(Long mesaId, CambiarEstadoMesaRequest req) {
    Mesa m = mesaRepo.findById(mesaId)
        .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada"));

    // reglas rápidas (ejemplos):
    if (m.getEstado() == EstadoMesa.BLOQUEADA && req.nuevoEstado() == EstadoMesa.OCUPADA) {
      throw new IllegalStateException("No se puede ocupar una mesa bloqueada");
    }
    if (req.nuevoEstado() == EstadoMesa.RESERVADA && m.getEstado() != EstadoMesa.LIBRE) {
      throw new IllegalStateException("Solo se puede reservar una mesa libre");
    }

    m.setEstado(req.nuevoEstado());
    registrarHistorial(m, req.nuevoEstado(), req.motivo(), req.usuarioId());
    return toDto(m);
  }

  @Transactional
  public MesaResponse moverDeZona(Long mesaId, Long nuevaZonaId) {
    Mesa m = mesaRepo.findById(mesaId)
        .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada"));
    ZonaMesa nueva = zonaRepo.findById(nuevaZonaId)
        .orElseThrow(() -> new EntityNotFoundException("Zona destino no encontrada"));
    mesaRepo.findByZonaIdAndCodigo(nuevaZonaId, m.getCodigo())
        .ifPresent(x -> { throw new IllegalArgumentException("Código ya usado en la zona destino"); });

    m.setZona(nueva);
    return toDto(m);
  }

  @Transactional
  public List<MesaResponse> unirMesas(List<Long> mesaIds, Long unionGroupId) {
    List<Mesa> mesas = mesaRepo.findAllById(mesaIds);
    mesas.forEach(m -> m.setUnionGroupId(unionGroupId));
    return mesas.stream().map(this::toDto).toList();
  }

  @Transactional
  public MesaResponse desunirMesa(Long mesaId) {
    Mesa m = mesaRepo.findById(mesaId)
        .orElseThrow(() -> new EntityNotFoundException("Mesa no encontrada"));
    m.setUnionGroupId(null);
    return toDto(m);
  }

  private void registrarHistorial(Mesa m, EstadoMesa estado, String motivo, Long usuarioId) {
    MesaEstadoHist h = MesaEstadoHist.builder()
        .mesa(m).estado(estado).motivo(motivo).usuarioId(usuarioId)
        .fechaCambio(OffsetDateTime.now())
        .build();
    histRepo.save(h);
  }

  private MesaResponse toDto(Mesa m) {
    return new MesaResponse(m.getId(), m.getCodigo(), m.getNombre(), m.getCapacidad(),
        m.getOrden(), m.getEstado(), m.getActivo(), m.getZona().getId(), m.getUnionGroupId());
  }
}