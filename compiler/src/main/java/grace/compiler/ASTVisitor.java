package grace.compiler;

import grace.compiler.element.AnnotationValues;

/**
 * Created by hechao on 2017/3/31.
 */

public interface ASTVisitor {

    void visitorMethod(ASTNode node);
    void visitorAnnotationOnMethod(ASTNode node,AnnotationValues annotationValues);
    void endVisitorMethod(ASTNode node);

    void visitorType(ASTNode node);
    void visitorAnnotationOnType(ASTNode node,AnnotationValues annotationValues);
    void endVisitorType(ASTNode node);

    void visitorField(ASTNode node);
    void visitorAnnotationOnField(ASTNode node,AnnotationValues annotationValues);
    void endVisitorField(ASTNode node);
}
