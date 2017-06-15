package grace.compiler.core;

import grace.compiler.AST;
import grace.compiler.ASTNode;
import grace.compiler.ASTContext;
import grace.core.json.AnnotationValues;

import java.lang.annotation.Annotation;

/**
 * Created by hechao on 2017/3/13.
 */

public abstract class BasicAST<T> implements AST<T> {

    private ASTContext mContext;

    public BasicAST(ASTContext context) {
        mContext = context;
    }

    @Override
    public ASTContext getContext() {
        return mContext;
    }

    @Override
    public <A extends Annotation> AnnotationValues<A> getNodeAnnotationValues(ASTNode<T> node, Class<A> annotationType) {
        return mContext.getNodeAnnotationValues(node, annotationType);
    }
}
