package grace.compiler.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import grace.compiler.AST;
import grace.compiler.ASTContext;
import grace.compiler.ASTFactory;
import grace.compiler.ASTNode;
import grace.compiler.ASTPrinter;
import grace.compiler.ASTVisitor;
import grace.compiler.AnnotationHandler;
import grace.compiler.element.ElementApt;
import grace.compiler.element.AnnotationValues;

/**
 * Created by hechao on 2017/3/3.
 */

public class ASTTransformation {

    private Map<String,AnnotationHandler> mHandlers = new HashMap<>();

    private Map<String,ASTFactory> mASTCreators = new HashMap<>();

    private ASTPrinter mPrinter;

    private ProcessingEnvironment mEnvironment;

    public ASTTransformation(ProcessingEnvironment environment, ASTPrinter printer){
        mPrinter = printer;
        mHandlers = new HashMap<>();
        mEnvironment = environment;
        loadJASTFactoriesAndAnnotationHandlers();
    }

    public void transform(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        doTransform(annotations, roundEnv);
    }

    private void doTransform(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){

        ASTFactory creator = getJASTCreator();
        if(creator == null) {
            mPrinter.printInfo("no ast creator，"+mASTCreators.size());
            return;
        }

        creator.init(new BasicASTContext(mPrinter,mEnvironment,roundEnv));
        List<AST> astList = new ArrayList<>();
        for (Element element : roundEnv.getRootElements()) {
            ASTContext context = new BasicASTContext(mPrinter,mEnvironment,roundEnv);
            astList.add(creator.create(context,element));
        }

        for (AST ast : astList){

            ast.traverse(new ASTVisitorAdapter(){
                @Override
                public void visitorAnnotationOnMethod(ASTNode node, AnnotationValues annotationValues) {
                    handleAnnotation(node, annotationValues);
                }

                @Override
                public void visitorAnnotationOnType(ASTNode node, AnnotationValues annotationValues) {
                    handleAnnotation(node, annotationValues);
                }

                @Override
                public void visitorAnnotationOnField(ASTNode node, AnnotationValues annotationValues) {
                    handleAnnotation(node, annotationValues);
                }
            });

        }
    }

    private void handleAnnotation(ASTNode node, AnnotationValues annotationValues){
        String annotationType = annotationValues.getAnnotationType().getCanonicalName();
        AnnotationHandler handler = mHandlers.get(annotationType);
        if(handler != null){
            mPrinter.printInfo("handle @%s for %s ",annotationValues.getAnnotationType().getSimpleName(),node);
            try {
                handler.handle(annotationValues,node);
            }catch (Exception e){
                mPrinter.printWarning("handle failed");
                e.printStackTrace();
            }
        }
    }

    private void loadJASTFactoriesAndAnnotationHandlers(){
        ElementApt factory = new ElementApt();
        mASTCreators.put(factory.identity(),factory);
        for (AnnotationHandler annotationHandler : factory.getAnnotationHandlers()){
            mHandlers.put(annotationHandler.getAnnotationType().getCanonicalName(),annotationHandler);
        }
    }

    private ASTFactory getJASTCreator(){
        return mASTCreators.get("element");
    }

    private static class ASTVisitorAdapter implements ASTVisitor{

        @Override
        public void visitorMethod(ASTNode node) {

        }

        @Override
        public void visitorAnnotationOnMethod(ASTNode node, AnnotationValues annotationValues) {

        }

        @Override
        public void endVisitorMethod(ASTNode node) {

        }

        @Override
        public void visitorType(ASTNode node) {

        }

        @Override
        public void visitorAnnotationOnType(ASTNode node, AnnotationValues annotationValues) {

        }

        @Override
        public void endVisitorType(ASTNode node) {

        }

        @Override
        public void visitorField(ASTNode node) {

        }

        @Override
        public void visitorAnnotationOnField(ASTNode node, AnnotationValues annotationValues) {

        }

        @Override
        public void endVisitorField(ASTNode node) {

        }
    }
}
