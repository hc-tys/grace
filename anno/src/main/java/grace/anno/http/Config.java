package grace.anno.http;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import grace.anno.Default;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by hechao on 2017/4/13.
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
public @interface Config {
    /**
     *
     * @return
     */
    Class<?> executor() default Default.class;

    /**
     *
     * @return
     */
    Class<?> filter() default Default.class;

    /**
     *
     * @return
     */
    Class<?> interceptor() default Default.class;
}
