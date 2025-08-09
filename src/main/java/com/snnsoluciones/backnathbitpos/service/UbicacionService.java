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
    Optional<Provincia> buscarProvinciaPorCodigo(Integer codigo);
    
    // Cantones
    List<Canton> listarCantonesPorProvincia(Integer codigoProvincia);
    Optional<Canton> buscarCantonPorId(Integer id);
    Optional<Canton> buscarCantonPorCodigos(Integer codigoProvincia, Integer codigoCanton);
    
    // Distritos
    List<Distrito> listarDistritosPorCanton(Integer codigoProvincia, Integer codigoCanton);
    Optional<Distrito> buscarDistritoPorId(Integer id);
    Optional<Distrito> buscarDistritoPorCodigos(Integer codigoProvincia, Integer codigoCanton, Integer codigoDistrito);
    
    // Barrios
    List<Barrio> listarBarriosPorDistrito(Integer codigoProvincia, Integer codigoCanton, Integer codigoDistrito);
    Optional<Barrio> buscarBarrioPorId(Integer id);
    Optional<Barrio> buscarBarrioPorCodigos(Integer codigoProvincia, Integer codigoCanton, 
                                            Integer codigoDistrito, Integer codigoBarrio);
}