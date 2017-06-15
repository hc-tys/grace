package grace.compiler;

import java.lang.annotation.Annotation;

import grace.core.json.AnnotationValues;

/**
 * Created by hechao on 2017/3/3.
 */

public interface AnnotationHandler <T extends Annotation>{

    void handle(AnnotationValues<T> annotationValues, ASTNode annotatedNode);

    Class<T> getAnnotationType();
}
