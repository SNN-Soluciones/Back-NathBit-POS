package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.entity.Provincia;
import com.snnsoluciones.backnathbitpos.entity.Canton;
import com.snnsoluciones.backnathbitpos.entity.Distrito;
import com.snnsoluciones.backnathbitpos.entity.Barrio;

import java.util.List;
import java.util.Optional;

public interface UbicacionService {
    
    // Provincias
    List<Provincia> listarProvincias();
    Optional<Provincia> buscarProvinciaPorId(Integer id);

    // Cantones
    List<Canton> listarCantonesPorProvincia(Integer codigoProvincia);
    Optional<Canton> buscarCantonPorId(Integer id);

    // Distritos
    List<Distrito> listarDistritosPorCanton(Integer codigoCanton);
    Optional<Distrito> buscarDistritoPorId(Integer id);

    // Barrios
    List<Barrio> listarBarriosPorDistrito(Integer codigoDistrito);
    Optional<Barrio> buscarBarrioPorId(Integer id);
}