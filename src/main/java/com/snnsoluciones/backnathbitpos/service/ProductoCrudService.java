package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.*;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

@Deprecated(since = "2.0", forRemoval = true)
public interface ProductoCrudService {

    // CRUD básico con soporte para imágenes
    ProductoDto crear(Long empresaId, ProductoCreateDto dto, MultipartFile imagen);
    ProductoDto actualizar(Long empresaId, Long productoId, ProductoUpdateDto dto, MultipartFile imagen);
    ProductoDto obtenerPorId(Long empresaId, Long productoId);
    Producto obtenerEntidadPorId(Long productoId);
    void eliminar(Long empresaId, Long productoId);
    void activarDesactivar(Long empresaId, Long productoId, boolean activo);

    // Generación de código
    String generarCodigoInterno(Long empresaId);
    ProductoDto actualizarPrecio(Long empresaId, Long productoId, ActualizarPrecioDto dto);
    Optional<Producto> buscarPorCodigoBarras(Long empresaId, String codigoBarras);
    void save(Producto producto);
    Optional<Producto> findByEmpresaIdAndCodigoCabys(Long empresaId, String codigoCabysId);
    ProductoDto crearProductoSimplificado(Long empresaId, Long sucursalId, ProductoCreateDto dto, MultipartFile imagen);
    Map<String, Object> obtenerModoFacturacion(Long sucursalId);
}