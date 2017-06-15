package grace.core.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

import grace.anno.json.Json;

/**
 * Created by hechao on 2017/5/15.
 */

public abstract class BaseJSONSerializer<T>{

    private static final String FIELD_PREFIX = "_";

    private static final String RAW_VALUE_FIELD_PREFIX = "_raw_";

    private static final String HELPER_FIELD_PREFIX = "__";

    private static final String HELPER_CLASS_PREFIX = "_H_";

    public JavaFile generateJavaFile(String packageName,String className,TypeName classType,List<JSONMethod<T>> methods) throws ProcessException {
        RootNode<T> rootNode = new RootNode<>(className,classType);
        for (JSONMethod<T> methodObj : methods){
            if(methodObj == null) continue;
            AnnotationValues<Json> annotationValues = methodObj.annotation();

            if (annotationValues == null) {
                rootNode.addUnJsonMethod(methodObj.value());
            } else {
                addJsonMethod(rootNode,annotationValues,methodObj);
            }
        }
        return JavaFile.builder(packageName, generatorSource(rootNode)).build();
    }

    private TypeSpec generatorSource(RootNode<T> node){
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(node.name)
                .addModifiers(Modifier.PUBLIC,Modifier.FINAL)
                .addSuperinterface(node.supperInterface);

        if(node.hasValue()){
            typeBuilder.addField(generateFieldSource(node).build());
            typeBuilder.addMethod(generateMethodSource(node).build());
        }

        for (T method : node.unJsonMethods){
            MethodSpec.Builder methodBuilder = createMethodBuilder(method);
            methodBuilder.addCode(CodeBlock.builder().addStatement("throw new UnsupportedOperationException(\"Not implemented\")").build());
            typeBuilder.addMethod(methodBuilder.build());
        }

        List<MethodSpec> methodSpecs = new ArrayList<>();
        for (TreeNode treeNode : node.children.values()){
            generatorSource(typeBuilder,methodSpecs,treeNode);
        }

        return typeBuilder.addMethods(methodSpecs).build();
    }

    private void generatorSource(TypeSpec.Builder typeBuilder,List<MethodSpec> methodSpecs,TreeNode<T> node){

        if(node.hasValue()){
            typeBuilder.addField(generateFieldSource(node).build());
            methodSpecs.add(generateMethodSource(node).build());
        }

        if(node.kind() == TreeNode.Kind.PATH && !node.isLeaf()){
            String helperTypeName = generateHelperTypeName(node.name);
            String helperFieldName = generateHelperFieldName(node);

            if(helperFieldName.equals(node.name)){
                typeBuilder.addField(FieldSpec.builder(ClassName.get("",helperTypeName),helperFieldName,Modifier.PUBLIC).build());
            }else{
                typeBuilder.addField(FieldSpec.builder(ClassName.get("",helperTypeName),helperFieldName,Modifier.PUBLIC).addAnnotation(generateFieldAliaAnnotation(node.name)).build());
            }

            TypeSpec.Builder helperTypeBuilder = TypeSpec.classBuilder(helperTypeName).addModifiers(Modifier.PUBLIC,Modifier.STATIC,Modifier.FINAL);
            for (TreeNode treeNode : node.children.values()){
                generatorSource(helperTypeBuilder,methodSpecs,treeNode);
            }

            typeBuilder.addType(helperTypeBuilder.build());
        }
    }

    private FieldSpec.Builder generateFieldSource(TreeNode<T> node){
        String fieldName = generateFieldName(node);
        return fieldName.equals(node.name) ? createFieldBuilder(node.name,node.value) : createFieldBuilder(fieldName,node.value).addAnnotation(generateFieldAliaAnnotation(node.name));
    }

    private String generateFieldName(TreeNode<T> node){
        return validFieldName(node.name) ? node.name : FIELD_PREFIX + node.name;
    }

    private String generateHelperFieldName(TreeNode<T> node){
        return node.hasValue() || !validFieldName(node.name) ? HELPER_FIELD_PREFIX + node.name : node.name;
    }

    private AnnotationSpec generateFieldAliaAnnotation(String rawName){
        return AnnotationSpec.builder(JsonProperty.class).addMember("value", "$S", rawName).build();
    }

    private String generateFieldAccess(TreeNode<T> node){
        switch (node.kind()) {
            case ROOT:
                return RAW_VALUE_FIELD_PREFIX + node.name;
            case PATH:
                StringBuilder builder = new StringBuilder();
                TreeNode temp = node.parent;
                while (!temp.isRoot()) {
                    builder.insert(0, ".").insert(0, generateHelperFieldName(temp));
                    temp = temp.parent;
                }
                builder.append(generateFieldName(node));
                return builder.toString();
            default:
                return null;
        }
    }

    private MethodSpec.Builder generateMethodSource(TreeNode<T> node){
        MethodSpec.Builder methodBuilder = createMethodBuilder(node.value);
        TypeName returnType = methodBuilder.build().returnType;
        String fieldAccess = generateFieldAccess(node);

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        if (node.parent.isRoot()) {
            codeBuilder.add(generateMethodBodySource(fieldAccess,returnType).build());
        } else {
            codeBuilder.add("try{\n");
            codeBuilder.add(generateMethodBodySource(fieldAccess,returnType).build());
            codeBuilder.add("}catch(Exception e){ return $L;}", defaultValue(node.value));
        }
        methodBuilder.addCode(codeBuilder.build());
        return methodBuilder;
    }

    private CodeBlock.Builder generateMethodBodySource(String fieldAccess,TypeName returnType){
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        if (returnType instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) returnType;
            codeBuilder.addStatement("return ($T)this.$N",parameterizedTypeName.rawType, fieldAccess);
        } else {
            codeBuilder.addStatement("return this.$N", fieldAccess);
        }
        return codeBuilder;
    }

    private boolean validFieldName(String name){
        return SourceVersion.isName(name);
    }

    private static String generateHelperTypeName(String name){
        return name.length() > 1 ? HELPER_CLASS_PREFIX + name.substring(0, 1).toUpperCase() + name.substring(1) : HELPER_CLASS_PREFIX + name.toUpperCase();
    }

    public abstract FieldSpec.Builder createFieldBuilder(String fieldName, T methodObj);

    public abstract MethodSpec.Builder createMethodBuilder(T methodObj);

    public abstract String defaultValue(T methodObj);

    private void addJsonMethod(RootNode<T> root ,AnnotationValues<Json> annotationValues,JSONMethod<T> methodObj) throws ProcessException {
        Json alias = annotationValues.getInstance();
        String[] path = alias.path();
        String key = alias.value();

        if (path.length == 0 && annotationValues.isExplicit("path") && key.isEmpty() && annotationValues.isExplicit("value")) {
            if (!String.class.getCanonicalName().equals(methodObj.returnType())) {
                throw new ProcessException("Explicitly set value() of @Json to empty,the return type should be java.lang.String");
            }
            addToTree(root, null, null, methodObj.value());
        } else {
            if (key.isEmpty()) key = methodObj.methodName();
            if (key.isEmpty()) throw new ProcessException("method name is null");
            addToTree(root, path, key, methodObj.value());
        }
    }

    private TreeNode addToTree(RootNode<T> root, String[] path, String fieldName, T value) throws ProcessException {

        if (value == null) return root;

        final String[] treePath;
        if (path == null || path.length == 0) {
            if (fieldName == null || fieldName.isEmpty()) {
                treePath = new String[0];
            } else {
                treePath = new String[]{fieldName};
            }
        } else {
            if (fieldName == null || fieldName.isEmpty()) {
                treePath = path;
            } else {
                treePath = Arrays.copyOf(path, path.length + 1);
                treePath[treePath.length - 1] = fieldName;
            }
        }

        TreeNode treeNode = root;
        for (String temp : treePath) {
            if (temp == null || temp.isEmpty()) continue;

            TreeNode node = treeNode.findChild(temp);
            if (node == null) {
                node = new PathNode(temp, treeNode);
                treeNode.addChild(temp,node);
            }
            treeNode = node;
        }

        if (!treeNode.isRoot() && treeNode.hasValue()) {
            throw new ProcessException(String.format("Method annotated with path :%s, key:%s has exist", Arrays.toString(path), fieldName));
        }
        treeNode.setValue(value);
        return root;
    }

    static abstract class TreeNode<T>{

        String name;

        TreeNode parent;

        T value;

        Map<String,TreeNode> children = new HashMap<>();

        enum Kind{
            ROOT,PATH
        }

        TreeNode(String name,TreeNode parent) {
            this.name = name;
            this.parent = parent;
        }

        abstract Kind kind();

        boolean hasValue(){
            return value != null;
        }

        void setValue(T value){
            this.value = value;
        }

        boolean isLeaf(){
            return children.size() == 0;
        }

        boolean isRoot(){
            return kind() == Kind.ROOT;
        }

        T value(){
            return value;
        }

        TreeNode findChild(String name){
            return children.get(name);
        }

        void addChild(String name,TreeNode<T> node){
            if(name != null && node != null) children.put(name,node);
        }
    }

    static class RootNode<T> extends TreeNode<T>{

        RootNode(String name,TypeName supperInterface) {
            super(name,null);
            this.supperInterface = supperInterface;
        }

        TypeName supperInterface;

        List<T> unJsonMethods = new ArrayList<>();

        void addUnJsonMethod(T value){
            if(value != null) unJsonMethods.add(value);
        }

        @Override
        Kind kind() {
            return Kind.ROOT;
        }
    }

    static class PathNode<T> extends TreeNode<T>{

        PathNode(String name,TreeNode parent) {
            super(name,parent);
        }

        @Override
        Kind kind() {
            return Kind.PATH;
        }
    }

}
