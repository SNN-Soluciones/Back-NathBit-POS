package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoImportDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoImportResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductoImportService {
    ProductoImportResultDto importarProductos(Long empresaId, List<ProductoImportDto> productos);
    List<ProductoImportDto> procesarArchivoExcel(MultipartFile archivo) throws Exception;
    ProductoImportResultDto importarDesdeExcel(Long empresaId, MultipartFile archivo) throws Exception;
}