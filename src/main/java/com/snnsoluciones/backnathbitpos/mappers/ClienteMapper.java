package com.snnsoluciones.backnathbitpos.mappers;

import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteBusquedaDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteExoneracionCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteExoneracionDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUbicacionCreateDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUbicacionDTO;
import com.snnsoluciones.backnathbitpos.dto.cliente.ClienteUpdateDTO;
import com.snnsoluciones.backnathbitpos.entity.Cliente;
import com.snnsoluciones.backnathbitpos.entity.ClienteExoneracion;
import com.snnsoluciones.backnathbitpos.entity.ClienteUbicacion;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Component
public interface ClienteMapper {

    // Mapeos principales Cliente
    @Mapping(target = "empresaId", source = "empresa.id")
    @Mapping(target = "empresaNombre", source = "empresa.nombreRazonSocial")
    @Mapping(target = "emails", source = "emails", qualifiedByName = "stringToList")
    @Mapping(target = "telefonoCompleto", expression = "java(formatearTelefono(cliente))")
    @Mapping(target = "exoneracionesActivas", expression = "java(contarExoneracionesActivas(cliente))")
    @Mapping(target = "tieneExoneracionVigente", expression = "java(tieneExoneracionVigente(cliente))")
    ClienteDTO toDTO(Cliente cliente);

    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ubicacion", ignore = true)
    @Mapping(target = "exoneraciones", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "inscritoHacienda", source = "inscritoHacienda")
    Cliente toEntity(ClienteCreateDTO dto);

    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ubicacion", ignore = true)
    @Mapping(target = "exoneraciones", ignore = true)
    @Mapping(target = "tieneExoneracion", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Cliente toEntity(ClienteUpdateDTO dto);

    // Mapeo para búsquedas
    @Mapping(target = "emails", source = "emails", qualifiedByName = "stringToList")
    @Mapping(target = "telefonoCompleto", expression = "java(formatearTelefono(cliente))")
    @Mapping(target = "ubicacionResumen", expression = "java(obtenerUbicacionResumen(cliente))")
    ClienteBusquedaDTO.ClienteOpcionDTO toOpcionDTO(Cliente cliente);

    // Mapeos de Ubicación
    @Mapping(target = "provinciaId", source = "provincia.id")
    @Mapping(target = "provinciaNombre", source = "provincia.provincia")
    @Mapping(target = "cantonId", source = "canton.id")
    @Mapping(target = "cantonNombre", source = "canton.canton")
    @Mapping(target = "distritoId", source = "distrito.id")
    @Mapping(target = "distritoNombre", source = "distrito.distrito")
    @Mapping(target = "barrioId", source = "barrio.id")
    @Mapping(target = "barrioNombre", source = "barrio.barrio")
    @Mapping(target = "direccionCompleta", expression = "java(formatearDireccionCompleta(ubicacion))")
    ClienteUbicacionDTO toDTO(ClienteUbicacion ubicacion);

    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "provincia", ignore = true)
    @Mapping(target = "canton", ignore = true)
    @Mapping(target = "distrito", ignore = true)
    @Mapping(target = "barrio", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClienteUbicacion toEntity(ClienteUbicacionCreateDTO dto);

    // Mapeos de Exoneración
    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNombre", source = "cliente.razonSocial")
    @Mapping(target = "vigente", expression = "java(exoneracion.estaVigente())")
    @Mapping(target = "diasParaVencer", expression = "java(calcularDiasParaVencer(exoneracion))")
    ClienteExoneracionDTO toDTO(ClienteExoneracion exoneracion);

    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ClienteExoneracion toEntity(ClienteExoneracionCreateDTO dto);

    // Métodos auxiliares
    @Named("stringToList")
    default List<String> stringToList(String emails) {
        if (emails == null || emails.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(emails.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    }

    default String formatearTelefono(Cliente cliente) {
        if (cliente.getTelefonoCodigoPais() == null || cliente.getTelefonoNumero() == null) {
            return null;
        }
        return "+" + cliente.getTelefonoCodigoPais() + " " + cliente.getTelefonoNumero();
    }

    default String obtenerPrimerEmail(String emails) {
        if (emails == null || emails.trim().isEmpty()) {
            return null;
        }
        String[] emailArray = emails.split(",");
        return emailArray[0].trim();
    }

    default Integer contarExoneracionesActivas(Cliente cliente) {
        if (cliente.getExoneraciones() == null) {
            return 0;
        }
        return (int) cliente.getExoneraciones().stream()
            .filter(ClienteExoneracion::getActivo)
            .count();
    }

    default Boolean tieneExoneracionVigente(Cliente cliente) {
        if (cliente.getExoneraciones() == null || cliente.getExoneraciones().isEmpty()) {
            return false;
        }
        return cliente.getExoneraciones().stream()
            .anyMatch(ClienteExoneracion::estaVigente);
    }

    default String obtenerUbicacionResumen(Cliente cliente) {
        if (cliente.getUbicacion() == null) {
            return null;
        }
        ClienteUbicacion ub = cliente.getUbicacion();
        return ub.getProvincia().getProvincia() + ", " + ub.getCanton().getCanton();
    }

    default String formatearDireccionCompleta(ClienteUbicacion ubicacion) {
        if (ubicacion == null) {
            return null;
        }

        StringBuilder direccion = new StringBuilder();
        direccion.append(ubicacion.getProvincia().getProvincia()).append(", ");
        direccion.append(ubicacion.getCanton().getCanton()).append(", ");
        direccion.append(ubicacion.getDistrito().getDistrito());

        if (ubicacion.getBarrio() != null) {
            direccion.append(", ").append(ubicacion.getBarrio().getBarrio());
        }

        if (ubicacion.getOtrasSenas() != null && !ubicacion.getOtrasSenas().isEmpty()) {
            direccion.append(" - ").append(ubicacion.getOtrasSenas());
        }

        return direccion.toString();
    }

    default Integer calcularDiasParaVencer(ClienteExoneracion exoneracion) {
        if (exoneracion.getFechaVencimiento() == null) {
            return null;
        }

        long dias = java.time.temporal.ChronoUnit.DAYS.between(
            java.time.LocalDate.now(),
            exoneracion.getFechaVencimiento()
        );

        return dias < 0 ? 0 : (int) dias;
    }

    Cliente toEntity(Cliente cliente);
}