package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Mesas;
import java.util.List;

public interface MesasService {

  Mesas obtenerMesaPorId(Long id);
  List<Mesas> obtenerTodas();
}
