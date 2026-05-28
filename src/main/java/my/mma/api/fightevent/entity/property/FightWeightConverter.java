package my.mma.api.fightevent.entity.property;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FightWeightConverter implements AttributeConverter<FightWeight, String> {

    @Override
    public String convertToDatabaseColumn(FightWeight attribute) {
        return attribute == null ? null : attribute.getDisplayName();
    }

    @Override
    public FightWeight convertToEntityAttribute(String dbData) {
        return FightWeight.fromDisplayName(dbData);
    }
}