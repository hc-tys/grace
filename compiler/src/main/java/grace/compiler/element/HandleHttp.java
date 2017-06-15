package grace.compiler.element;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

import grace.anno.http.Body;
import grace.anno.http.Config;
import grace.anno.http.Header;
import grace.anno.http.Http;
import grace.anno.http.Https;
import grace.anno.http.Method;
import grace.anno.http.Path;
import grace.anno.http.Port;
import grace.anno.http.Query;
import grace.compiler.AST;
import grace.compiler.ASTContext;
import grace.compiler.ASTNode;
import grace.compiler.ASTPrinter;
import grace.compiler.AnnotationHandler;
import grace.core.http.Call;
import grace.core.http.HttpService;
import grace.core.http.Request;
import grace.core.mapper.MapperConfig;

/**
 * Created by hechao on 2017/4/5.
 */

public class HandleHttp implements AnnotationHandler<Http> {

    @Override
    public void handle(AnnotationValues<Http> annotationValues, ASTNode annotatedNode) {
        handleHttp(false,annotatedNode);
    }

    public static void handleHttp(boolean isHttps,ASTNode classNode){

        AST ast = classNode.getAST();
        ASTContext context = ast.getContext();
        ASTPrinter printer = context.getAttribute(ASTPrinter.class);

        TypeElement element = (TypeElement)ast.asElement(classNode);
        if(element == null){
            printer.printInfo("no element for node : %s",classNode);
            return;
        }

        if(element.getKind() != ElementKind.INTERFACE ){
            printer.printError("@Http or @Https should only be annotated on interface,%s is not a interface",classNode.getName());
            return;
        }

        GenCode.GenType genType = context.getAttribute(GenCode.class).getGenHttp((DeclaredType) element.asType());
        if(genType == null){
            printer.printInfo("not find gen type for node : %s",classNode);
            return;
        }

        List<MethodSpec> methodSpecs = null;
        try {
            methodSpecs = generateInterfaceMethods(isHttps,context,classNode);
        } catch (ProcessException e) {
            e.printStackTrace();
            printer.printError(e.getMessage());
            return;
        }

        TypeSpec typeSpec = TypeSpec.classBuilder(genType.simpleName)
                .addModifiers(Modifier.PUBLIC,Modifier.FINAL)
                .addSuperinterface(HttpService.class)
                .addSuperinterface(ClassName.get(element))
                .addField(generateCallFactoryField())
                .addMethod(generateCallFactoryMethod())
                .addMethods(methodSpecs)
                .build();

        try {
            JavaFile javaFile = JavaFile.builder(genType.packageName, typeSpec).build();
            Filer filer = context.getAttribute(Filer.class);
            javaFile.writeTo(filer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MethodSpec generateCallFactoryMethod(){
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        codeBlock.addStatement("this.callFactory = arg0");
        return JavaPoetHelper.createMethod(HttpService.class.getMethods()[0],codeBlock.build());
    }

    private static FieldSpec generateCallFactoryField(){
        return FieldSpec.builder(Call.Factory.class,"callFactory", Modifier.PRIVATE).build();
    }

    private static List<MethodSpec> generateInterfaceMethods(boolean isHttps,ASTContext context,ASTNode classNode) throws ProcessException{

        List<MethodSpec> methodSpecs = new ArrayList<>();

        AST ast = classNode.getAST();
        final String host;
        if(isHttps){
            AnnotationValues<Https> hostAnnotation = context.getNodeAnnotationValues(classNode,Https.class);
            host = hostAnnotation.getInstance().value();
        }else {
            AnnotationValues<Http> hostAnnotation = context.getNodeAnnotationValues(classNode,Http.class);
            host = hostAnnotation.getInstance().value();
        }

        AnnotationValues<Port> portAnnotation = context.getNodeAnnotationValues(classNode,Port.class);
        AnnotationValues<Method> methodAnnotation = context.getNodeAnnotationValues(classNode,Method.class);
        AnnotationValues<Header> headerAnnotation = context.getNodeAnnotationValues(classNode,Header.class);

        AnnotationValues<Config> configAnnotation = context.getNodeAnnotationValues(classNode,Config.class);
        TypeName[] configTypes = configAnnotation != null ? getConfig(configAnnotation) : new TypeName[]{null,null,null};
        List<ASTNode> children = classNode.getChildren();
        for (ASTNode node : children){
            if(node.kind() != AST.Kind.METHOD) continue;
            ExecutableElement executableElement = (ExecutableElement) ast.asElement(node);
            if(executableElement == null) continue;
            CodeBlock.Builder codeBuilder = CodeBlock.builder();
            codeBuilder.add("$T request = new $T($S)",ClassName.get(Request.class),ClassName.get(Request.Builder.class),host);
            if(portAnnotation != null){
                codeBuilder.add(".port($L)",portAnnotation.getInstance().value());
            }
            if(isHttps){
                codeBuilder.add(".protocol($T.$L)",TypeName.get(Request.Protocol.class),Request.Protocol.HTTPS);
            }

            if(methodAnnotation != null){
                codeBuilder.add(".method($L)",Request.Method.valueOf(methodAnnotation.getInstance().value().name()));
            }

            if(headerAnnotation != null){
                codeBuilder.add(".headers($S)",headerAnnotation.getInstance().value());
            }

            AnnotationValues<Header> methodHeaderAnnotation = context.getNodeAnnotationValues(node,Header.class);
            if(methodHeaderAnnotation != null){
                codeBuilder.add(".headers($S)",methodHeaderAnnotation.getInstance().value());
            }

            AnnotationValues<Path> pathAnnotation = context.getNodeAnnotationValues(node,Path.class);
            handleMethodParameter(codeBuilder,pathAnnotation,executableElement);
            codeBuilder.addStatement(".build()");

            AnnotationValues<Config> methodConfigAnnotation = context.getNodeAnnotationValues(node,Config.class);
            String config = handlerConfig(codeBuilder,configTypes,getConfig(methodConfigAnnotation));
            handleReturn(context,classNode,executableElement,codeBuilder,config);
            methodSpecs.add(JavaPoetHelper.createMethod(executableElement,codeBuilder.build()));
        }
        return methodSpecs;
    }

    private static boolean useReflex(ParameterizedTypeName parameterizedTypeName){
        for (int i = 0 ;  i < parameterizedTypeName.typeArguments.size() ; i++){
            TypeName temp  = parameterizedTypeName.typeArguments.get(i);
            if( !(temp instanceof ClassName) && !(temp instanceof ArrayTypeName)){
                return true;
            }
        }
        return false;
    }

    private static String handlerConfig(CodeBlock.Builder codeBuilder ,TypeName[] configTypes, TypeName[] methodConfigTypes){

        TypeName executor = methodConfigTypes[0] != null ? methodConfigTypes[0] : configTypes[0];
        TypeName filter = methodConfigTypes[1] != null ? methodConfigTypes[1] : configTypes[1];
        TypeName interceptor = methodConfigTypes[2] != null ? methodConfigTypes[2] : configTypes[2];

        if(executor != null || filter != null || interceptor != null){
            CodeBlock.Builder configBuilder = CodeBlock.builder();
            configBuilder.add("$T config = new $T.Builder()",ClassName.get(MapperConfig.class),ClassName.get(MapperConfig.class));
            if(executor != null) configBuilder.add(".executor(new $T())",executor);
            if(interceptor != null) configBuilder.add(".interceptor(new $T())",interceptor);
            if(filter != null) configBuilder.add(".filter(new $T())",filter);
            configBuilder.addStatement(".build()");
            codeBuilder.add(configBuilder.build());
            return "config";
        }
        return null;
    }

    private static TypeName[] getConfig(AnnotationValues<Config> configAnnotation){

        TypeName[] configTypes = new TypeName[]{null,null,null};
        if(configAnnotation == null) return configTypes;

        Config typeConfig = configAnnotation.getInstance();
        if(configAnnotation.isExplicit("executor")){
            try {
                ClassName.get(typeConfig.executor());
            }catch (MirroredTypeException e){
                configTypes[0] = TypeName.get(e.getTypeMirror());
            }
        }
        if(configAnnotation.isExplicit("filter")) {
            try {
                ClassName.get(typeConfig.filter());
            } catch (MirroredTypeException e) {
                configTypes[1] = TypeName.get(e.getTypeMirror());
            }
        }
        if(configAnnotation.isExplicit("interceptor")){
            try {
                ClassName.get(typeConfig.interceptor());
            }catch (MirroredTypeException e){
                configTypes[2] = TypeName.get(e.getTypeMirror());
            }
        }
        return configTypes;
    }

    private static void handleReturn( ASTContext context,ASTNode classNode,ExecutableElement executableElement,CodeBlock.Builder codeBuilder,String config){
        DeclaredType returnDeclareType = (DeclaredType) executableElement.getReturnType();

        TypeName returnTypeName = TypeName.get(returnDeclareType);
        if(returnTypeName instanceof ParameterizedTypeName){
            returnTypeName = ((ParameterizedTypeName)returnTypeName).rawType;
        }
        TypeName typeName = TypeMapper.getTypeName(context,returnDeclareType.getTypeArguments().get(0));

        if(typeName instanceof ParameterizedTypeName){
            ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) typeName;

            if(useReflex(parameterizedTypeName)){
                codeBuilder.addStatement("final $T type",ClassName.get(ParameterizedType.class));
                codeBuilder.add("try{\n type = (ParameterizedType)this.getClass().getMethod($S",executableElement.getSimpleName());
                for (VariableElement variableElement : executableElement.getParameters()){
                    codeBuilder.add(",$T.class",TypeName.get(variableElement.asType()));
                }
                codeBuilder.addStatement(").getGenericReturnType()");
                codeBuilder.addStatement("}catch($T e){throw new $T(e.getMessage());}",ClassName.get(Exception.class),ClassName.get(RuntimeException.class));
                codeBuilder.addStatement("return ($T)callFactory.create(request,$L,type.getActualTypeArguments()[0])",returnTypeName,config);
            }else{
                int typeArgumentSize = parameterizedTypeName.typeArguments.size();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("return ($T)callFactory.create(request,$L,$T.class");
                for (int i = 0 ;i < typeArgumentSize ; i++){
                    stringBuilder.append(",$T.class");
                }
                stringBuilder.append(")");
                Object[] objects = new Object[typeArgumentSize + 3];
                objects[0] = returnTypeName;
                objects[1] = config;
                objects[2] = parameterizedTypeName.rawType;
                for (int i = 0 ; i < typeArgumentSize ; i++){
                    objects[i+3] = parameterizedTypeName.typeArguments.get(i);
                }
                codeBuilder.addStatement(stringBuilder.toString(),objects);
            }
        }else{
            codeBuilder.addStatement("return ($T)callFactory.create(request,$L,$T.class)",returnTypeName,config, typeName);
        }
    }

    private static void handleMethodParameter(CodeBlock.Builder codeBuilder ,AnnotationValues<Path> pathAnnotation,ExecutableElement executableElement){

        Map<String,String> pathParams = new HashMap<>();
        for (VariableElement  variableElement : executableElement.getParameters()){
            String variableName = variableElement.getSimpleName().toString();

            Body bodyAnnotation = variableElement.getAnnotation(Body.class);
            if(bodyAnnotation != null){
                codeBuilder.add(".body(%L)",variableName);
            }

            Query parameterAnnotation = variableElement.getAnnotation(Query.class);
            if(parameterAnnotation != null){
                String parameter = parameterAnnotation.value().isEmpty() ? variableName : parameterAnnotation.value();
                codeBuilder.add(".parameter($S,$L)",parameter,variableName);
            }

            Header pheaderAnnotation = variableElement.getAnnotation(Header.class);
            if(pheaderAnnotation != null){
                String header = pheaderAnnotation.value().isEmpty() ? variableName : pheaderAnnotation.value();
                codeBuilder.add(".header($S,$L)",header,variableName);
            }

            Path param = variableElement.getAnnotation(Path.class);
            if(param != null) {
                String value = param.value().isEmpty() ? variableName : param.value();
                pathParams.put(value, variableName);
            }
        }

        String path = pathAnnotation != null ? pathAnnotation.getInstance().value() : null;

        if(path == null || path.isEmpty()) return;

        if(pathParams.size() == 0){
            codeBuilder.add(".path($S)",path);
        }else{
            codeBuilder.add(".path($L)",buildPath(pathParams,path));
        }
    }

    private static String buildPath(Map<String,String> pathParams,String path){
        StringBuilder builder = new StringBuilder();
        List<String> paramValues = new ArrayList<>();

        StringBuilder keyBuilder = new StringBuilder();
        boolean isKey = false;
        for (int i = 0; i < path.length();i++){
            char temp = path.charAt(i);
            if(temp == '{'){
                isKey = true;
            }else if(temp == '}'){
                String key = keyBuilder.toString();
                keyBuilder.delete(0,keyBuilder.length());
                paramValues.add(pathParams.get(key));
                builder.append('%').append(paramValues.size()).append("$s");
                isKey = false;
            }else{
                if(isKey){
                    keyBuilder.append(temp);
                }else {
                    builder.append(temp);
                }
            }
        }

        StringBuilder result = new StringBuilder();
        result.append("String.format(\"").append(builder.toString()).append("\"");
        for (String value : paramValues){
            result.append(",").append(value);
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public Class<Http> getAnnotationType() {
        return Http.class;
    }
}
