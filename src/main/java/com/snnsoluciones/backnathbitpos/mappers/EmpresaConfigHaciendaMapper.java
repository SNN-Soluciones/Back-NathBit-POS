package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.confighacienda.ConfigHaciendaResponse;
import com.snnsoluciones.backnathbitpos.entity.EmpresaConfigHacienda;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = ComponentModel.SPRING)
public interface EmpresaConfigHaciendaMapper {

  ConfigHaciendaResponse toDto(EmpresaConfigHacienda empresaConfigHacienda);
}