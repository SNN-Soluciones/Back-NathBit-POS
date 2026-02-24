package com.snnsoluciones.backnathbitpos.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Converter
public class MapJsonConverter implements AttributeConverter<Map<Long, BigDecimal>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<Long, BigDecimal> attribute) {
      if (attribute == null) {
        return "{}";
      }
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Override
    public Map<Long, BigDecimal> convertToEntityAttribute(String dbData) {
      if (dbData == null || dbData.isBlank()) {
        return new HashMap<>();
      }
        try {
            return mapper.readValue(dbData,
                mapper.getTypeFactory().constructMapType(HashMap.class, Long.class, BigDecimal.class));
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}