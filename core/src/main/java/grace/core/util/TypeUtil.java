package grace.core.util;


import java.lang.reflect.Type;

/**
 * Created by hechao on 2017/4/6.
 */

public class TypeUtil {

    public static Object getDefaultValue(Type type) {
        if(!(type instanceof Class<?>)) return null;
        Class<?> clz = (Class<?>) type;
        return clz.isPrimitive()?(clz == Boolean.TYPE?Boolean.valueOf(false):Integer.valueOf(0)):null;
    }

}
