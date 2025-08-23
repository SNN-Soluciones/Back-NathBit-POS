package com.snnsoluciones.backnathbitpos.controller;

import com.snnsoluciones.backnathbitpos.dto.common.ApiResponse;
import com.snnsoluciones.backnathbitpos.entity.Mesas;
import com.snnsoluciones.backnathbitpos.service.MesasService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

  @Autowired
  private MesasService mesasService;

  @GetMapping
  ResponseEntity<ApiResponse<List<Mesas>>> obtenerTodas(){
    return ResponseEntity.ok(ApiResponse.ok(mesasService.obtenerTodas()));
  }

}
