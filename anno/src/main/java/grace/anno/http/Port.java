package grace.anno.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by hechao on 2017/4/4.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Port {
    int value() default 80;
}
