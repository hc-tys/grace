package grace.compiler.element;

import java.lang.annotation.Annotation;

/**
 * Created by hechao on 2017/3/3.
 */

public interface AnnotationValues<T extends Annotation>  {

    boolean isExplicit(String property);

    T getInstance();

    Class<T> getAnnotationType();
}
