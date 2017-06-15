package grace.core.factory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.ClassKey;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;

import grace.anno.json.JSONSerializer;
import grace.core.mapper.Converter;
import grace.core.util.GLogger;
import grace.core.util.TypeUtil;

/**
 * Created by hechao on 2017/4/27.
 */

public class JacksonConvertFactory implements Converter.Factory {
    @Override
    public <F> Converter<F, ?> create(Class<F> clazz, final Type type) {
        return new Converter<F, Object>() {
            @Override
            public Object convert(F value) throws IOException {
                return mapperTo(value,type);
            }
        };
    }

    @Override
    public <F, T> Converter<F, T> create(Class<F> clazz,final Class<T> raw, final Type... typeParams) {
        return new Converter<F, T>() {
            @Override
            public T convert(F value) throws IOException {
                return (T) mapperTo(value,raw,typeParams);
            }
        };
    }

    private static ObjectMapper sObjectMapper = new ObjectMapper();
    static {

        //register interface and implement
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.setAbstractTypes(new GraceAbstractTypeResolver());
        sObjectMapper.registerModule(simpleModule);

        //ignore unknown properties
        sObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //ignore method
        sObjectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
        sObjectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
        //deserializer ignore null property
        sObjectMapper.setPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL,JsonInclude.Include.NON_NULL));
    }

    private static Object mapperTo(Object from, Class raw, Type... typeParams) throws IOException{
        if(typeParams == null || typeParams.length == 0) return mapperTo(from,raw);
        JavaType[] javaTypes = new JavaType[typeParams.length];
        for (int i = 0 ; i < javaTypes.length ; i++){
            javaTypes[i] = sObjectMapper.constructType(typeParams[i]);
        }
        JavaType targetType = sObjectMapper.getTypeFactory().constructParametricType(raw,javaTypes);
        return mapperTo(from, targetType);
    }

    private static Object mapperTo(Object from,Type type) throws IOException {

        if(type == String.class) return mapperToString(from);

        JavaType javaType = toJavaType(type);
        return javaType == null ? TypeUtil.getDefaultValue(type) : mapperValue(from,javaType);
    }

    /**
     * @param type
     * @return
     */
    private static JavaType toJavaType(Type type){
        return sObjectMapper.constructType(type);
    }

    private static Object mapperValue(Object value,JavaType type) throws IOException {
        if(value == null || type == null) return null;
        if(value instanceof byte[]) return sObjectMapper.readValue((byte[]) value,type);
        else if(value instanceof String) return sObjectMapper.readValue((String) value,type);
        else {
            GLogger.info("Meet Unsupported type %s ,filter its value to String :",type);
            String strValue = sObjectMapper.writeValueAsString(value);
            return sObjectMapper.readValue(strValue,sObjectMapper.constructType(type));
        }
    }

    private static String mapperToString(Object value) throws JsonProcessingException {
        return sObjectMapper.writeValueAsString(value);
    }

    private static class GraceAbstractTypeResolver extends SimpleAbstractTypeResolver{

        protected final HashMap<ClassKey,Class<?>> _implementsMappings = new HashMap<ClassKey,Class<?>>();

        public GraceAbstractTypeResolver() {
            for (Class clazz : GraceFactory.sGenCodeFactory.getAllJSONSerializer().values()){
                _implementsMappings.put(new ClassKey(clazz.getInterfaces()[0]),clazz);
            }
        }

        @Override
        public JavaType resolveAbstractType(DeserializationConfig config, BeanDescription typeDesc) {
            Class<?> beanClass = typeDesc.getBeanClass();
            Class target = _implementsMappings.get(new ClassKey(beanClass));
            if(target != null) return config.getTypeFactory().constructType(target);

            if(!beanClass.isInterface() || !beanClass.isAnnotationPresent(JSONSerializer.class)) return null;
            target = createConcrete(beanClass);
            if(target == null) return null;
            _implementsMappings.put(new ClassKey(beanClass),target);
            return config.getTypeFactory().constructType(target);
        }

        // TODO: 2017/6/15  create concrete class for interface
        private Class createConcrete(Class<?> beanClass){
            return null;
        }
    }
}
