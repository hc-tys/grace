package grace.core;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import grace.core.mapper.Converter;
import grace.core.mapper.GraceMapper;
import grace.core.mapper.GraceRuleMapper;
import grace.core.util.GLogger;
import grace.core.util.TypeUtil;

/**
 * Created by hechao on 2017/4/11.
 */

class GraceMapperImpl implements GraceMapper {

    Converter.Factory converterFactory;

    Object source;

    public GraceMapperImpl(Object source, Converter.Factory converterFactory) {
        this.converterFactory = converterFactory;
        this.source = source;
    }

    @Override
    public <T> List<T> toList(Class<T> type) {
        return to(List.class,type);
    }

    @Override
    public <T> Set<T> toSet(Class<T> type) {
        return to(Set.class,type);
    }

    @Override
    public <K, V> Map<K, V> toMap(Class<K> keyType, Class<V> valueType) {
        return to(Map.class,keyType,valueType);
    }

    @Override
    public <T> T to(Class<T> raw, Type... typeParam) {
       return (T) _to(raw, typeParam);
    }

    @Override
    public <T> T to(Type type) {
        return (T) _to(type,new Type[0]);
    }

    @Override
    public <T> T to(Class<T> type) {
        return (T) _to(type,new Type[0]);
    }

    private Object _to(Type raw, Type... typeParam) {
        try {
            Object target = transformTarget(source);
            if(target == null) {
                GLogger.info("target value is null");
                return TypeUtil.getDefaultValue(raw);
            }

            if(raw == GraceRuleMapper.class){
                return Grace.filter(target.toString());
            }

            Converter converter = typeParam == null || typeParam.length == 0
                    ? converterFactory.create(target.getClass(),raw)
                    : converterFactory.create(target.getClass(),(Class)raw,typeParam);
            return converter.convert(target);
        } catch (IOException e) {
            GLogger.info("mapper to type %s failed",raw);
            e.printStackTrace();
            return TypeUtil.getDefaultValue(raw);
        }
    }

    Object transformTarget(Object source){
        return source;
    }

}
