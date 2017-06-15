package grace.core.http;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by hechao on 2017/4/3.
 */

public interface Converter<F,T>{
    T convert(F value) throws IOException;

    interface Factory {

        <F> Converter<F,?> create(Class<F> clazz,Type type);

        <F,T> Converter<F,T> create(Class<F> clazz,Class<T> raw,Type... typeParams);

    }
}
