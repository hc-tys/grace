package grace.core.mapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import grace.core.http.Call;

/**
 * Created by hechao on 2017/4/11.
 */

public interface Mapper{

    <T> T to(Class<T> raw,Type... typeParams);

    <T> T to(Type type);

    <T> T to(Class<T> type);

    interface CollectionMapper {

        <T> List<T> toList(Class<T> type);

        <T> Set<T> toSet(Class<T> type);

        <K,V> Map<K,V> toMap(Class<K> keyType, Class<V> valueType);
    }

    interface CallMapper {
        <T> Call<T> toCall(Type type);

        <T> Call<T> toCall(Class<T> raw,Type... typeParams);
    }

    interface CallCollectionMapper {

        <T> Call<List<T>> toListCall(Class<T> type);

        <T> Call<Set<T>> toSetCall(Class<T> type);

        <K,V> Call<Map<K,V>> toMapCall(Class<K> keyType, Class<V> valueType);
    }

    interface ServiceMapper {
        <T> T toService(Class<T> httpService);
    }

}
