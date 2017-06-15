package grace.anno.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by hechao on 2017/4/4.
 */
@Target({PARAMETER})
@Retention(RUNTIME)
public @interface Query {
    String value() default "";
}
