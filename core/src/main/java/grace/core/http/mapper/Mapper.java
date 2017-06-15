package grace.core.http.mapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import grace.core.http.Call;
import grace.core.http.Executor;
import grace.core.http.Filter;
import grace.core.http.Interceptor;

/**
 * Created by hechao on 2017/4/11.
 */

public interface Mapper{

    interface RawMapper{

        <T> T to(Class<T> raw,Type... typeParams);

        <T> T to(Type type);

        <T> T to(Class<T> type);
}

    interface CollectionMapper{

        <T> List<T> toList(Class<T> type);

        <T> Set<T> toSet(Class<T> type);

        <K,V> Map<K,V> toMap(Class<K> keyType, Class<V> valueType);
    }

    interface RawCallMapper{

        <T> Call<T> toCall(Type type);

        <T> Call<T> toCall(Class<T> raw,Type... typeParams);

    }

    interface CollectionCallMapper{

        <T> Call<List<T>> toListCall(Class<T> type);

        <T> Call<Set<T>> toSetCall(Class<T> type);

        <K,V> Call<Map<K,V>> toMapCall(Class<K> keyType, Class<V> valueType);
    }

    interface HttpMapper{
        <T> T toService(Class<T> httpService);
    }

}
