package grace.compiler.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import grace.core.json.AnnotationValues;

/**
 * Created by hechao on 2017/3/31.
 */

public class AnnotationValuesCreator {

    public static <T extends Annotation> AnnotationValues<T> creator(Element element, Class<T> annotationType){
        AnnotationMirror mirror = getAnnotationMirror(element,annotationType);
        return mirror != null ? new AnnotationValuesImpl(element.getAnnotation(annotationType),getAnnotationMirror(element,annotationType)) : null;
    }

    public static <T extends Annotation> AnnotationValues<T> creator(Element element, AnnotationMirror annotationMirror){
        try {
            Class<? extends Annotation> annotationType = (Class<? extends Annotation>) Class.forName(annotationMirror.getAnnotationType().toString());
            return creator(element, (Class<T>) annotationType);
        } catch (Exception e) {
            return null;
        }
    }

    private static AnnotationMirror getAnnotationMirror(Element element,Class annotationType){
        if(element == null || annotationType == null) return null;
        String fullQualifiedName = annotationType.getCanonicalName();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()){
            if(fullQualifiedName.equals(mirror.getAnnotationType().toString())){
                return mirror;
            }
        }
        return null;
    }

    private static class AnnotationValuesImpl<T extends Annotation> implements AnnotationValues<T> {

        private T instance;

        private List<String> explicitProperties;

        public AnnotationValuesImpl(T instance,AnnotationMirror annotationMirror){
            this.explicitProperties = getExplicitProperties(annotationMirror);
            this.instance = instance;
        }

        @Override
        public boolean isExplicit(String property) {
            return explicitProperties.contains(property);
        }

        @Override
        public T getInstance() {
            return instance;
        }

        @Override
        public Class<T> getAnnotationType() {
            return (Class<T>) instance.annotationType();
        }

        private static List<String> getExplicitProperties(AnnotationMirror annotationMirror){
            Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotationMirror.getElementValues();
            List<String> explicitProperties = new ArrayList<>();
            for (ExecutableElement executableElement : values.keySet()){
                explicitProperties.add(executableElement.getSimpleName().toString());
            }
            return explicitProperties;
        }
    }
}
