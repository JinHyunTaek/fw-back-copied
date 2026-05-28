package my.mma.api.global.resolver;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

public class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

    public RecordSupportingTypeResolver(ObjectMapper.DefaultTyping t, PolymorphicTypeValidator ptv) {
        super(t, ptv);
    }

    @Override
    public boolean useForType(JavaType t) {
        boolean isRecord = t.getRawClass().isRecord();
        boolean superResult = super.useForType(t);

        if (isRecord) {
            return true;
        }
        return superResult;
    }
}