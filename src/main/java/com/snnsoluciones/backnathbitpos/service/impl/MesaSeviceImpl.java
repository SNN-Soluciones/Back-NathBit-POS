package com.snnsoluciones.backnathbitpos.service.impl;

import com.snnsoluciones.backnathbitpos.entity.Mesas;
import com.snnsoluciones.backnathbitpos.repository.MesasRepository;
import com.snnsoluciones.backnathbitpos.service.MesasService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MesaSeviceImpl implements MesasService {

  @Autowired
  private MesasRepository mesasRepository;

  @Override
  public Mesas obtenerMesaPorId(Long id) {
    return mesasRepository.findById(id).orElse(null);
  }

  @Override
  public List<Mesas> obtenerTodas() {
    return mesasRepository.findAll();
  }
}
