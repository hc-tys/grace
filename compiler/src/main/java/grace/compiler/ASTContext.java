package grace.compiler;

import java.lang.annotation.Annotation;
import java.util.Set;

import grace.compiler.element.AnnotationValues;

/**
 * Created by hechao on 2017/3/7.
 */

public interface ASTContext {

    <A extends Annotation> AnnotationValues<A> getNodeAnnotationValues(ASTNode node, Class<A> annotationType);

    Set<ASTNode> getAllNodeAnnotatedWith(Class<? extends Annotation> annotationType);

    <T> T getAttribute(Class<T> attr);

    Object getAttribute(String id);

    void setAttribute(Object attr);

    void setAttribute(String id,Object attr);

    <T> T removeAttribute(Class<T> attr);

    Object removeAttribute(String id);
}
