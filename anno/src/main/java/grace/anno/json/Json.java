package grace.anno.json;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Created by hechao on 2017/1/5.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Json {
    /**
     * 对应于json格式的字符串
     * 如果未设置，则使用所注解字段的simple name
     * @return
     */
    String value() default "";

    /**
     * 关键字所在json str中的路劲,支持跨级查询
     * @return
     */
    String[] path() default {};

}