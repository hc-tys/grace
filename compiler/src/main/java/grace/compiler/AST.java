package grace.compiler;

import java.lang.annotation.Annotation;

import javax.lang.model.element.Element;

import grace.core.json.AnnotationValues;

/**
 * Created by hechao on 2017/3/3.
 */

public interface AST<T> {

    enum Kind{
        TYPE, FIELD, METHOD
    }

    ASTContext getContext();

    /** The AST top node */
    ASTNode<T> getTop();

    void traverse(ASTVisitor visitor);

    ASTNode<T> asNode(Element element);

    ASTNode<T> buildNode(T obj);

    Element asElement(ASTNode<T> node);

    <A extends Annotation> AnnotationValues<A> getNodeAnnotationValues(ASTNode<T> node, Class<A> annotationType);
}
