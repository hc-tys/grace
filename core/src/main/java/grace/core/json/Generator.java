package grace.core.json;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import grace.anno.json.JSONSerializer;
import grace.anno.json.Json;

/**
 * Created by hechao on 2017/5/16.
 * 生成代码中使用的javax.lang.model.element.Modifier
 * 在android不支持。
 */

public class Generator {

    public static JavaFile createJavaFile(Class<?> serializer){

        if(!serializer.isInterface())
            throw new IllegalArgumentException(String.format("%s is not a interface",serializer));

        if(!serializer.isAnnotationPresent(JSONSerializer.class))
            throw new IllegalArgumentException(String.format("%s is not annotated with @JSONSerializer",serializer));

        try {
            return new JSONSerializerHandler().generateJavaFile(serializer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class JSONSerializerHandler extends BaseJSONSerializer<Method>{

        private static final String JSON_SUFFIX = "_Json";

        public JavaFile generateJavaFile(Class<?> serializer) throws ProcessException {

            String pkgName = serializer.getPackage().getName();
            String className = createJsonClassSimpleName(serializer.getSimpleName());
            TypeName classType = ClassName.get(serializer);
            return generateJavaFile(pkgName,className,classType,getJsonMethod(serializer));
        }

        private static String createJsonClassSimpleName(String simpleName){
            return new StringBuilder().append("_").append(simpleName).append(JSON_SUFFIX).toString();
        }

        @Override
        public FieldSpec.Builder createFieldBuilder(String fieldName, Method method) {
            TypeName fieldType = TypeName.get(method.getReturnType());
            return FieldSpec.builder(fieldType, fieldName, Modifier.PUBLIC);
        }

        @Override
        public MethodSpec.Builder createMethodBuilder(Method method) {
            String methodName = method.getName();
            TypeName returnType = TypeName.get(method.getReturnType());
            List<TypeName> exceptions = new ArrayList<>();
            for(Class exception : method.getExceptionTypes()){
                exceptions.add(TypeName.get(exception));
            }

            List<ParameterSpec> parameterSpecs = new ArrayList<>();
            Parameter[] parameters = method.getParameters();
            for (int i = 0 ; i<parameters.length ;i++){
                Parameter parameter = parameters[i];
                parameterSpecs.add(ParameterSpec.builder(parameter.getParameterizedType(),"arg"+i).build());
            }

            return MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addExceptions(exceptions)
                    .addParameters(parameterSpecs)
                    .returns(returnType);
        }

        @Override
        public String defaultValue(Method method) {
            Class retType = method.getReturnType();
            if(retType.isPrimitive()){
                if(retType == boolean.class){
                    return "false";
                }else {
                    return "0";
                }
            }else {
                return null;
            }
        }

        private static List<JSONMethod<Method>> getJsonMethod(Class interfaceClass) throws ProcessException {

            List<JSONMethod<Method>> methods = new ArrayList<>();

            for (Method method : interfaceClass.getMethods()) {
                if(method == null) continue;

                if (method.getParameterCount() != 0) {
                    throw new ProcessException(String.format("Method included in interface annotated with @JSONSerializer should have no parameter,method %s not fit", method.getName()));
                }

                Json annotation = method.getAnnotation(Json.class);
                if(annotation == null){
                    methods.add(new ReflectJSONMethod(method,null));
                }else {
                    AnnotationValues<Json> annotationValues = new ReflectAnnotationValues(annotation);
                    methods.add(new ReflectJSONMethod(method,annotationValues));
                }
            }
            return methods;
        }
    }

    private static class ReflectAnnotationValues implements AnnotationValues<Json>{

        Json json;

        public ReflectAnnotationValues(Json json) {
            this.json = json;
        }

        @Override
        public boolean isExplicit(String property) {
            return false;
        }

        @Override
        public Json getInstance() {
            return json;
        }

        @Override
        public Class<Json> getAnnotationType() {
            return Json.class;
        }
    }

    private static class ReflectJSONMethod implements JSONMethod<Method>{

        private Method value;

        private AnnotationValues<Json> annotationValues;

        public ReflectJSONMethod(Method value, AnnotationValues<Json> annotationValues) {
            this.value = value;
            this.annotationValues = annotationValues;
        }

        @Override
        public Method value() {
            return value;
        }

        @Override
        public String methodName() {
            return value.getName();
        }

        @Override
        public String returnType() {
            return value.getReturnType().getCanonicalName();
        }

        @Override
        public AnnotationValues<Json> annotation() {
            return annotationValues;
        }
    }
}
