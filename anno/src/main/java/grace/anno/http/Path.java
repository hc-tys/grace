package grace.anno.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by hechao on 2017/4/4.
 */
@Target({METHOD,PARAMETER})
@Retention(RUNTIME)
public @interface Path {
    String value() default "";
}
