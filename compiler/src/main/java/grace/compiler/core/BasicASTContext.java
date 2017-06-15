package grace.compiler.core;

import grace.compiler.ASTNode;
import grace.compiler.ASTContext;
import grace.compiler.ASTPrinter;
import grace.compiler.element.AnnotationValues;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Created by hechao on 2017/3/7.
 */

public class BasicASTContext implements ASTContext {


    private final Map<String, Object> map = new ConcurrentHashMap<String, Object>();

    public BasicASTContext(ASTPrinter printer, ProcessingEnvironment pEnvironment, RoundEnvironment roundEnvironment) {

        setAttribute(ProcessingEnvironment.class.getCanonicalName(),pEnvironment);
        setAttribute(RoundEnvironment.class.getCanonicalName(),roundEnvironment);
        setAttribute(ASTPrinter.class.getCanonicalName(),printer);
        setAttribute(Filer.class.getCanonicalName(),pEnvironment.getFiler());
        setAttribute(Types.class.getCanonicalName(),pEnvironment.getTypeUtils());
        setAttribute(Elements.class.getCanonicalName(),pEnvironment.getElementUtils());
    }

    @Override
    public <A extends Annotation> AnnotationValues<A> getNodeAnnotationValues(ASTNode node, Class<A> annotationType) {
        return null;
    }

    @Override
    public Set<ASTNode> getAllNodeAnnotatedWith(Class<? extends Annotation> annotationType) {
        return null;
    }

    @Override
    public <T> T getAttribute(Class<T> attr) {
        return attr == null ? null : (T) getAttribute(attr.getCanonicalName());
    }

    @Override
    public void setAttribute(Object attr) {
        if(attr == null){
            return;
        }
        setAttribute(attr.getClass().getCanonicalName(),attr);
    }

    @Override
    public Object getAttribute(final String id) {
        if(isEmpty(id)){
            return null;
        }
        return this.map.get(id);
    }

    @Override
    public void setAttribute(final String id, final Object obj) {
        if(isEmpty(id)){
            return;
        }
        if (obj != null) {
            this.map.put(id, obj);
        } else {
            this.map.remove(id);
        }
    }

    @Override
    public <T> T removeAttribute(Class<T> attr) {
        return attr != null ? (T) removeAttribute(attr.getCanonicalName()) : null;
    }

    @Override
    public Object removeAttribute(String id) {
        return id != null ? this.map.remove(id) : null;
    }

    private static boolean isEmpty(String id){
        return id == null || id.isEmpty();
    }
}
