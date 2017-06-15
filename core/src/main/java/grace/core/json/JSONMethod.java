package grace.core.json;

import grace.anno.json.Json;

/**
 * Created by hechao on 2017/5/16.
 */

public interface JSONMethod<T> {

    T value();

    String methodName();

    String returnType();

    AnnotationValues<Json> annotation();
}
