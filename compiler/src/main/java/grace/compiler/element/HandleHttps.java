package grace.compiler.element;

import grace.anno.http.Https;
import grace.compiler.ASTNode;
import grace.compiler.AnnotationHandler;

import static grace.compiler.element.HandleHttp.handleHttp;

/**
 * Created by hechao on 2017/4/5.
 */

public class HandleHttps implements AnnotationHandler<Https> {
    @Override
    public void handle(AnnotationValues<Https> annotationValues, ASTNode annotatedNode) {
        handleHttp(true,annotatedNode);
    }

    @Override
    public Class<Https> getAnnotationType() {
        return Https.class;
    }

}
