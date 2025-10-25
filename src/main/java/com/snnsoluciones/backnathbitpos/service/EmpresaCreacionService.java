package com.snnsoluciones.backnathbitpos.service;

import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaRequest;
import com.snnsoluciones.backnathbitpos.dto.empresa.CrearEmpresaCompletaResponse;
import org.springframework.web.multipart.MultipartFile;

public interface EmpresaCreacionService {
    CrearEmpresaCompletaResponse crearEmpresaCompleta(
        CrearEmpresaCompletaRequest request,
        MultipartFile logo,
        MultipartFile certificado
    );

    CrearEmpresaCompletaResponse actualizarEmpresaCompleta(
        Long empresaId,
        CrearEmpresaCompletaRequest request,
        MultipartFile logo,
        MultipartFile certificado,
        String usuarioEmail);
}