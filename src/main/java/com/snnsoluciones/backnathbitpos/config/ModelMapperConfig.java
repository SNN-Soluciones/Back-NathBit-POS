package com.snnsoluciones.backnathbitpos.config;

import com.snnsoluciones.backnathbitpos.dto.producto.ProductoCreateDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoDto;
import com.snnsoluciones.backnathbitpos.dto.producto.ProductoListDto;
import com.snnsoluciones.backnathbitpos.entity.Producto;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
    
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        // Configuración estricta para evitar mapeos incorrectos
        modelMapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT)
            .setFieldMatchingEnabled(true)
            .setSkipNullEnabled(true)
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
        
        // Configuraciones personalizadas si las necesitas
        configurarMapeosPersonalizados(modelMapper);
        
        return modelMapper;
    }
    
    private void configurarMapeosPersonalizados(ModelMapper modelMapper) {
        // Ejemplo: Si necesitas mapeos especiales
        
        // Producto -> ProductoDto
        modelMapper.typeMap(Producto.class, ProductoDto.class)
            .addMappings(mapper -> {
                mapper.map(src -> src.getEmpresa().getId(), ProductoDto::setEmpresaId);
                mapper.skip(ProductoDto::setImpuestos); // Lo mapeamos manualmente
            });
        
        // ProductoCreateDto -> Producto
        modelMapper.typeMap(ProductoCreateDto.class, Producto.class)
            .addMappings(mapper -> {
                mapper.skip(Producto::setId);
                mapper.skip(Producto::setEmpresa);
                mapper.skip(Producto::setImpuestos);
            });

      modelMapper.typeMap(Producto.class, ProductoListDto.class)
          .addMappings(mapper -> {
            mapper.map(Producto::getImagenUrl, ProductoListDto::setImagenUrl);
            mapper.map(Producto::getThumbnailUrl, ProductoListDto::setThumbnailUrl);
          });
    }
}