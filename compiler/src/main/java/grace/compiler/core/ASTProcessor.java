package grace.compiler.core;

import com.google.auto.service.AutoService;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import grace.anno.http.Http;
import grace.anno.http.Https;
import grace.anno.json.JSONSerializer;

/**
 * Created by hechao on 2017/3/3.
 */
@AutoService(Processor.class)
public class ASTProcessor extends AbstractProcessor {

    private grace.compiler.ASTPrinter mPrinter;

    private ASTTransformation mASTTransform;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mPrinter = new MessagePrinter(processingEnv.getMessager());
        mPrinter.printInfo("-------------init(%s)-----------",processingEnv);
        mASTTransform = new ASTTransformation(processingEnv,mPrinter);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        mPrinter.printInfo("-------------process(%s)-----------",roundEnv.processingOver());
        if (roundEnv.processingOver()) return false;

        for (TypeElement typeElement : annotations){
            if(containSupportAnnotation(typeElement)){
                mASTTransform.transform(annotations, roundEnv);
                break;
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String>  types = new LinkedHashSet<>();
        types.add(JSONSerializer.class.getCanonicalName());
        types.add(Http.class.getCanonicalName());
        types.add(Https.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    private boolean containSupportAnnotation(TypeElement typeElement){
        for(String annotationType : getSupportedAnnotationTypes()){
            if(annotationType.equals(typeElement.asType().toString())){
                return true;
            }
        }
        return false;
    }
    private static final boolean DEBUG = true;

    private static final class MessagePrinter implements grace.compiler.ASTPrinter {

        private Messager messager;

        public MessagePrinter(Messager messager) {
            this.messager = messager;
        }

        @Override
        public void printError(String format, Object... args) {
            if(DEBUG){
                messager.printMessage(Diagnostic.Kind.ERROR,buildMessage(format, args));
            }else{
                System.err.println(buildMessage(format, args));
            }
        }

        @Override
        public void printWarning(String format, Object... args) {
            if(DEBUG) {
                messager.printMessage(Diagnostic.Kind.WARNING, buildMessage(format, args));
            }else{
                System.err.println(buildMessage(format, args));
            }
        }

        @Override
        public void printInfo(String format, Object... args) {
            if(DEBUG) {
                messager.printMessage(Diagnostic.Kind.NOTE, buildMessage(format, args));
            }else{
                System.out.println(buildMessage(format,args));
            }
        }

        /**
         * Formats the caller's provided message and prepends useful info
         */
        private static String buildMessage(String format, Object... args) {
            String msg ;
            try{
                msg = (args == null) ? format : String.format(Locale.US, format, args);
            }catch (Exception e){
                msg = format + Arrays.toString(args);
            }
            return msg;
        }
    }
}
