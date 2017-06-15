package grace.compiler.element;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

import grace.anno.http.Http;
import grace.anno.http.Https;
import grace.anno.json.JSONSerializer;
import grace.compiler.AST;
import grace.compiler.ASTContext;
import grace.compiler.ASTFactory;
import grace.compiler.ASTNode;
import grace.compiler.ASTPrinter;
import grace.compiler.ASTVisitor;
import grace.compiler.AnnotationHandler;
import grace.compiler.AnnotationHandlerProvider;
import grace.compiler.Constants;
import grace.compiler.core.AnnotationValuesCreator;
import grace.compiler.core.BasicAST;
import grace.compiler.core.BasicASTContext;
import grace.compiler.core.BasicASTNode;
import grace.core.factory.GraceFactory;

/**
 * Created by hechao on 2017/3/14.
 */

//@AutoService(ASTFactory.class)
public class ElementApt implements ASTFactory ,AnnotationHandlerProvider{

    private List<AST> cache = new ArrayList<>();

    private GenCode genCode;
    @Override
    public void init(ASTContext context) {
        genCode = createFactory(context);
    }

    @Override
    public AST create(ASTContext context, Element root) {
        AST ast = new JASTImpl(new EASTContextImpl(this,context,root),root);
        cache.add(ast);
        return ast;
    }

    @Override
    public String identity() {
        return "element";
    }

    @Override
    public List<AnnotationHandler> getAnnotationHandlers() {
        List<AnnotationHandler> handlers = new ArrayList<>();
        handlers.add(new HandleJSONSerializer());
        handlers.add(new HandleHttp());
        handlers.add(new HandleHttps());
        return handlers;
    }

    private static GenCode createFactory(ASTContext context){

        GenCode genCode = new GenCode();

        FieldSpec jsonSerializers = FieldSpec.builder(ClassName.get(Map.class),"jsonSerializers",Modifier.PRIVATE).initializer("new $T()",ClassName.get(HashMap.class)).build();

        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        for(Element element : context.getAttribute(RoundEnvironment.class).getElementsAnnotatedWith(JSONSerializer.class)){
            GenCode.GenType genType = genCode.addJsonType((DeclaredType) element.asType());
            codeBuilder.addStatement("jsonSerializers.put($T.class.getCanonicalName(),$T.class)",ClassName.get(genType.type),ClassName.get(genType.packageName,genType.simpleName));
        }

        FieldSpec httpServices = FieldSpec.builder(ClassName.get(Map.class),"httpServices",Modifier.PRIVATE).initializer("new $T()",ClassName.get(HashMap.class)).build();

        Map<String,GenCode.GenType> httpMap = new HashMap<>();

        for(Element element : context.getAttribute(RoundEnvironment.class).getElementsAnnotatedWith(Https.class)){
            GenCode.GenType genType = genCode.addHttpType((DeclaredType) element.asType());
            codeBuilder.addStatement("httpServices.put($T.class.getCanonicalName(),new $T())",ClassName.get(genType.type),ClassName.get(genType.packageName,genType.simpleName));
        }

        for(Element element : context.getAttribute(RoundEnvironment.class).getElementsAnnotatedWith(Http.class)){
            GenCode.GenType genType = genCode.addHttpType((DeclaredType) element.asType());
            codeBuilder.addStatement("httpServices.put($T.class.getCanonicalName(),new $T())",ClassName.get(genType.type),ClassName.get(genType.packageName,genType.simpleName));
        }

        MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).addCode(codeBuilder.build()).build();

        List<MethodSpec> methodSpecs = new ArrayList<>();
        for(Method method: GraceFactory.GenCodeFactory.class.getMethods()){
            if(method.getName().equals("getAllJSONSerializer")) {
                methodSpecs.add(JavaPoetHelper.createMethod(method, CodeBlock.builder().add("return jsonSerializers;").build()));
            }else{
                methodSpecs.add(JavaPoetHelper.createMethod(method, CodeBlock.builder().add("return httpServices;").build()));
            }
        }

        TypeSpec factory = TypeSpec.classBuilder("_Grace_Factory")
                .addSuperinterface(GraceFactory.GenCodeFactory.class)
                .addModifiers(Modifier.FINAL,Modifier.PUBLIC)
                .addField(jsonSerializers)
                .addField(httpServices)
                .addMethod(constructor)
                .addMethods(methodSpecs)
                .build();
        try {
            JavaFile javaFile = JavaFile.builder(Constants.GRACE_CODEGEN_FACTORY_PACKAGE, factory).build();
            Filer filer = context.getAttribute(Filer.class);
            javaFile.writeTo(filer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return genCode;
    }

    private static class JASTImpl extends BasicAST<Element> {

        private static Map<Element,ASTNode> cache = new HashMap<>();

        private ASTNode<Element> top;

        public JASTImpl(ASTContext context, Element element) {
            super(context);
            this.top = buildNode(element);
        }

        @Override
        public ASTNode<Element> getTop() {
            return top;
        }

        @Override
        public void traverse(ASTVisitor visitor) {
            top.traverse(visitor);
        }

        @Override
        public ASTNode<Element> asNode(Element element) {
            return cache.get(element);
        }

        @Override
        public Element asElement(ASTNode<Element> node) {
            return node != null ? node.getValue() : null;
        }

        @Override
        public ASTNode<Element> buildNode(Element obj) {
            if(cache.containsKey(obj)) return cache.get(obj);
            ASTNode<Element> node;
            switch (obj.getKind()){
                case METHOD:
                case CONSTRUCTOR:
                    node = buildMethod((ExecutableElement) obj);
                    break;
                case FIELD:
                case ENUM_CONSTANT:
                    node = buildField((VariableElement) obj);
                    break;
                case CLASS:
                    node = buildType((TypeElement) obj);
                    break;
                case INTERFACE:
                    node = buildInterface((TypeElement) obj);
                    break;
                default:
                    node =  null;
            }
            cache.put(obj,node);
            return node;
        }

        private ASTNode<Element> buildMethod(ExecutableElement element){
            return new JASTNodeImpl(this,element,new ArrayList<ASTNode>(),Kind.METHOD,element.getSimpleName().toString());
        }

        private ASTNode<Element> buildField(VariableElement element){
            return new JASTNodeImpl(this,element,new ArrayList<ASTNode>(),Kind.FIELD,element.getSimpleName().toString());
        }

        private ASTNode<Element> buildType(TypeElement element){
            List<ASTNode> children = new ArrayList<>();
            for (Element child :element.getEnclosedElements()){
                ASTNode node = buildNode(child);
                if(node != null){
                    children.add(node);
                }
            }
            return new JASTNodeImpl(this,element,children,Kind.TYPE,element.getSimpleName().toString());
        }

        private ASTNode<Element> buildInterface(TypeElement element){

            List<ASTNode> children = new ArrayList<>();
            for (Element memberElement : getContext().getAttribute(Elements.class).getAllMembers(element)){
                switch (memberElement.getKind()){
                    case METHOD:
                        if(memberElement.getModifiers().contains(Modifier.ABSTRACT)){
                            children.add(buildMethod((ExecutableElement) memberElement));
                        }
                        break;
                    case INTERFACE:
                        children.add(buildInterface((TypeElement) memberElement));
                        break;
                    default:
                        break;
                }
            }
            return new JASTNodeImpl(this,element,children,Kind.TYPE,element.getSimpleName().toString());
        }
    }

    private static class JASTNodeImpl extends BasicASTNode<Element> {

        public JASTNodeImpl(AST ast, Element value, List<ASTNode> children, AST.Kind kind, String name) {
            super(ast, value, children, kind, name);
        }

        @Override
        public void traverse(ASTVisitor visitor) {
            switch (kind()){
                case TYPE:
                    visitor.visitorType(this);
                    visitorAnnotation(this,visitor);
                    traverseChildren(this,visitor);
                    visitor.endVisitorType(this);
                    break;
                case METHOD:
                    visitor.visitorMethod(this);
                    visitorAnnotation(this,visitor);
                    traverseChildren(this,visitor);
                    visitor.endVisitorMethod(this);
                    break;
                case FIELD:
                    visitor.visitorField(this);
                    visitorAnnotation(this,visitor);
                    visitor.endVisitorField(this);
                    break;
            }
        }

        private static void traverseChildren(ASTNode node, ASTVisitor visitor){
            List<ASTNode> children = node.getChildren();
            for (ASTNode child : children) child.traverse(visitor);
        }

        private static void visitorAnnotation(ASTNode astNode,ASTVisitor visitor){
            Element element = astNode.getAST().asElement(astNode);
            if(element == null) return;
            for (AnnotationMirror mirror :element.getAnnotationMirrors()){
                AnnotationValues annotationValues = AnnotationValuesCreator.creator(element,mirror);
                if(annotationValues == null) continue;
                switch (astNode.kind()){
                    case TYPE:
                        visitor.visitorAnnotationOnType(astNode,annotationValues);
                        break;
                    case METHOD:
                        visitor.visitorAnnotationOnMethod(astNode,annotationValues);
                        break;
                    case FIELD:
                        visitor.visitorAnnotationOnField(astNode,annotationValues);
                        break;
                }
            }
        }
    }

    private static class EASTContextImpl extends BasicASTContext {

        private Map<String,AnnotationValues> annotationValuesCache = new HashMap<>();

        private static Map<String,Set> sAnnotationNodeCache = new HashMap<>();

        private ElementApt factory;

        EASTContextImpl(ElementApt factory, ASTContext context, Element root){
            super(context.getAttribute(ASTPrinter.class),context.getAttribute(ProcessingEnvironment.class),context.getAttribute(RoundEnvironment.class));
            this.factory = factory;
            PackageElement packageElement = context.getAttribute(Elements.class).getPackageOf(root);
            setAttribute(PackageElement.class.getCanonicalName(),packageElement);
            setAttribute(factory.genCode);
        }

        @Override
        public <A extends Annotation> AnnotationValues<A> getNodeAnnotationValues(ASTNode node, Class<A> annotationType) {
            Element element = node != null ? (Element) node.getValue() : null;
            if(element == null || annotationType == null) return null;

            String key = createKey(element,annotationType);
            if(sAnnotationNodeCache.containsKey(key)) return (AnnotationValues<A>) sAnnotationNodeCache.get(key);

            AnnotationValues annotationValues = AnnotationValuesCreator.creator(element,annotationType);
            annotationValuesCache.put(key,annotationValues);
            return annotationValues;
        }

        @Override
        public Set<ASTNode> getAllNodeAnnotatedWith(final Class<? extends Annotation> annotationType) {
            String key = annotationType.getCanonicalName();
            if(sAnnotationNodeCache.containsKey(key)){
                return sAnnotationNodeCache.get(key);
            }
            final Set<ASTNode> nodes = new HashSet<>();
            for (AST ast : factory.cache){
                ast.traverse(new ASTVisitor() {
                    @Override
                    public void visitorMethod(ASTNode node) {
                        if(getNodeAnnotationValues(node,annotationType) != null) nodes.add(node) ;
                    }

                    @Override
                    public void visitorAnnotationOnMethod(ASTNode node, AnnotationValues annotationValues) {
                        if(annotationType == annotationValues.getAnnotationType()) nodes.add(node);
                    }

                    @Override
                    public void endVisitorMethod(ASTNode node) {

                    }

                    @Override
                    public void visitorType(ASTNode node) {
                    }

                    @Override
                    public void visitorAnnotationOnType(ASTNode node, AnnotationValues annotationValues) {
                        if(annotationType == annotationValues.getAnnotationType()) nodes.add(node);
                    }

                    @Override
                    public void endVisitorType(ASTNode node) {
                    }

                    @Override
                    public void visitorField(ASTNode node) {
                    }

                    @Override
                    public void visitorAnnotationOnField(ASTNode node, AnnotationValues annotationValues) {
                        if(annotationType == annotationValues.getAnnotationType()) nodes.add(node);
                    }

                    @Override
                    public void endVisitorField(ASTNode node) {

                    }
                });
            }
            sAnnotationNodeCache.put(key,nodes);
            return nodes;
        }

        private String createKey(Element element, Class annotationType){
            return element.toString() + annotationType.getCanonicalName();
        }
    }
}
