package grace.compiler.element;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Created by hechao on 2017/3/31.
 */

public class JavaPoetHelper {

    /**
     * 注意泛型的处理
     * @param executableElement
     * @return
     */
    public static MethodSpec.Builder createMethodBuilder(ExecutableElement executableElement){
        String methodName = executableElement.getSimpleName().toString();
        List<TypeName> exceptions = new ArrayList<>();
        for (TypeMirror typeMirror : executableElement.getThrownTypes()){
            exceptions.add(TypeName.get(typeMirror));
        }
        TypeName returnType = TypeName.get(executableElement.getReturnType());

        List<ParameterSpec> parameterSpecs = new ArrayList<>();
        List<? extends VariableElement> variableElements = executableElement.getParameters();
        for (int i = 0 ; i< variableElements.size() ;i++){
            VariableElement parameter = variableElements.get(i);
            parameterSpecs.add(ParameterSpec.get(parameter));
        }

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addExceptions(exceptions)
                .addParameters(parameterSpecs)
                .returns(returnType);
    }

    public static MethodSpec.Builder createMethodBuilder(Method method){
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

    public static MethodSpec createMethod(Method method,CodeBlock codeBlock){
        return createMethodBuilder(method).addCode(codeBlock).build();
    }

    public static MethodSpec createMethod(ExecutableElement executableElement,CodeBlock codeBlock){
        return createMethodBuilder(executableElement).addCode(codeBlock).build();
    }

}
