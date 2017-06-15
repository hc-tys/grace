package grace.compiler.element;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import grace.anno.json.JSONSerializer;
import grace.anno.json.Json;
import grace.compiler.AST;
import grace.compiler.ASTContext;
import grace.compiler.ASTNode;
import grace.compiler.ASTPrinter;
import grace.compiler.AnnotationHandler;
import grace.core.json.AnnotationValues;
import grace.core.json.BaseJSONSerializer;
import grace.core.json.JSONMethod;
import grace.core.json.ProcessException;

/**
 * Created by hechao on 2017/3/13.
 */

class HandleJSONSerializer extends BaseJSONSerializer<ExecutableElement> implements AnnotationHandler<JSONSerializer> {

    private ASTContext mContext;

    @Override
    public void handle(AnnotationValues<JSONSerializer> annotationValues, ASTNode classNode) {

        AST ast = classNode.getAST();
        mContext = ast.getContext();
        ASTPrinter printer = mContext.getAttribute(ASTPrinter.class);

        TypeElement element = (TypeElement) ast.asElement(classNode);
        if (element == null) {
            printer.printInfo("no element for node : %s", classNode);
            return;
        }

        if (element.getKind() != ElementKind.INTERFACE) {
            printer.printError("@JSONSerializer should only be annotated on interface,%s is not a interface", classNode.getName());
            return;
        }

        GenCode.GenType genType = mContext.getAttribute(GenCode.class).getGenJson((DeclaredType) element.asType());
        if (genType == null) {
            printer.printInfo("not find gen type for node : %s", classNode);
            return;
        }

        try {
            JavaFile javaFile = generateJavaFile(genType.packageName,genType.simpleName,ClassName.get(element.asType()),getJsonMethod(classNode));
            Filer filer = mContext.getAttribute(Filer.class);
            javaFile.writeTo(filer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Class<JSONSerializer> getAnnotationType() {
        return JSONSerializer.class;
    }

    @Override
    public FieldSpec.Builder createFieldBuilder(String fieldName, ExecutableElement executableElement) {
        TypeName fieldType = TypeMapper.getTypeName(mContext, executableElement.getReturnType());
        return FieldSpec.builder(fieldType, fieldName, Modifier.PUBLIC);
    }

    @Override
    public MethodSpec.Builder createMethodBuilder(ExecutableElement executableElement) {
        return JavaPoetHelper.createMethodBuilder(executableElement);
    }

    @Override
    public String defaultValue(ExecutableElement element) {
        switch (element.getReturnType().getKind()) {
            case BOOLEAN:
                return  "false";
            case BYTE:
            case DOUBLE:
            case INT:
            case SHORT:
            case LONG:
            case CHAR:
                return  "0";
            case FLOAT:
                return "0.0f";
            default:
                return null;
        }
    }

    private static List<JSONMethod<ExecutableElement>> getJsonMethod(ASTNode classNode) throws ProcessException {
        AST ast = classNode.getAST();
        List<JSONMethod<ExecutableElement>> methods = new ArrayList<>();

        for (ASTNode node : (Iterable<ASTNode>) classNode.getChildren()) {
            if (node.kind() != AST.Kind.METHOD) continue;

            ExecutableElement element = (ExecutableElement) ast.asElement(node);
            if (element == null) continue;
            if (element.getParameters().size() != 0) {
                throw new ProcessException(String.format("Method included in interface annotated with @JSONSerializer should have no parameter,method %s not fit", element.getSimpleName()));
            }

            AnnotationValues<Json> annotationValues = ast.getNodeAnnotationValues(node, Json.class);
            methods.add(new ElementJSONMethod(element,annotationValues));
        }
        return methods;
    }

    private static class ElementJSONMethod implements JSONMethod<ExecutableElement>{

        private ExecutableElement value;

        private AnnotationValues<Json> annotationValues;

        public ElementJSONMethod(ExecutableElement value, AnnotationValues<Json> annotationValues) {
            this.value = value;
            this.annotationValues = annotationValues;
        }

        @Override
        public ExecutableElement value() {
            return value;
        }

        @Override
        public String methodName() {
            return value.getSimpleName().toString();
        }

        @Override
        public String returnType() {
            return value.getReturnType().toString();
        }

        @Override
        public AnnotationValues<Json> annotation() {
            return annotationValues;
        }
    }
}
