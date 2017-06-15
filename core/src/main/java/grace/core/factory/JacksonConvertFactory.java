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
import com.squareup.javapoet.JavaFile;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import grace.anno.json.JSONSerializer;
import grace.core.http.Converter;
import grace.core.json.Generator;
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

//        GLogger.info("map type %s to java type %s, raw class : %s",type,javaType,javaType.getRawClass());

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
            JavaFile javaFile = Generator.createJavaFile(beanClass);
            if(javaFile == null) return null;

            StringBuilder classFullNameBuilder = new StringBuilder();
            classFullNameBuilder.append(javaFile.packageName).append(".").append(javaFile.typeSpec.name);
            target = new JsonClassLoader(javaFile.toJavaFileObject(),classFullNameBuilder.toString()).loadJsonClass();
            if(target == null) return null;
            _implementsMappings.put(new ClassKey(beanClass),target);
            return config.getTypeFactory().constructType(target);
        }
    }

    private static class JsonClassLoader extends URLClassLoader{

        JavaFileObject javaFileObject;
        String fullName;

        public JsonClassLoader(JavaFileObject javaFileObject,String fullName) {
            super(new URL[0], JsonClassLoader.class.getClassLoader());
            this.javaFileObject = javaFileObject;
            this.fullName = fullName;
        }

        public Class<?> loadJsonClass(){
            try {
                return loadClass(fullName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            GLogger.info("find class : %s,full : %s",name,fullName);
            try {
                Map<String, byte[]> classBytes = compile(javaFileObject);
                if(classBytes == null || classBytes.size() == 0) return null;
                byte[] buf = classBytes.remove(name);
                if(buf == null) return null;
                return defineClass(name, buf, 0, buf.length);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public Map<String, byte[]> compile(JavaFileObject javaFileObject) throws IOException {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            try (MemoryJavaFileManager manager = new MemoryJavaFileManager(compiler.getStandardFileManager(null, null, null))) {
                JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject));
                Boolean result = task.call();
                if (result == null || !result.booleanValue()) {
                    GLogger.info("Compilation failed for java:%s",fullName);
                    return null;
                }
                return manager.getClassBytes();
            }
        }
    }

    static class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        // compiled classes in bytes:
        final Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

        MemoryJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        public Map<String, byte[]> getClassBytes() {
            return new HashMap<String, byte[]>(this.classBytes);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
            classBytes.clear();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind,
                                                   FileObject sibling) throws IOException {
            if (kind == JavaFileObject.Kind.CLASS) {
                return new MemoryOutputJavaFileObject(className);
            } else {
                return super.getJavaFileForOutput(location, className, kind, sibling);
            }
        }

        class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
            final String name;

            MemoryOutputJavaFileObject(String name) {
                super(URI.create("string:///" + name), Kind.CLASS);
                this.name = name;
            }

            @Override
            public OutputStream openOutputStream() {
                return new FilterOutputStream(new ByteArrayOutputStream()) {
                    @Override
                    public void close() throws IOException {
                        out.close();
                        ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
                        classBytes.put(name, bos.toByteArray());
                    }
                };
            }
        }
    }

}
