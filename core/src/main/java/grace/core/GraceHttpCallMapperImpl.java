package grace.core;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import grace.core.http.Call;
import grace.core.http.Request;
import grace.core.mapper.Converter;
import grace.core.mapper.GraceHttpCallMapper;
import grace.core.mapper.MapperConfig;

/**
 * Created by hechao on 2017/4/23.
 */

class GraceHttpCallMapperImpl<F> extends GraceHttpMapperImpl<F> implements GraceHttpCallMapper<F> {

    private Call.Factory callFactory;

    GraceHttpCallMapperImpl(MapperConfig<F> config, Request request, Converter.Factory factory, Call.Factory callFactory) {
        super(config,request, factory);
        this.callFactory = callFactory;
    }

    @Override
    public <T> Call<T> toCall(Type type) {
        return (Call<T>) callFactory.create((Request)source,getConfig(),type);
    }

    @Override
    public <T> Call<T> toCall(Class<T> type, Type... typeParams) {
        return callFactory.create((Request)source,getConfig(),type,typeParams);
    }

    @Override
    public <T> Call<List<T>> toListCall(Class<T> type) {
        Call call = toCall(List.class, type);
        return call;
    }

    @Override
    public <T> Call<Set<T>> toSetCall(Class<T> type) {
        Call call = toCall(Set.class, type);
        return call;
    }

    @Override
    public <K, V> Call<Map<K, V>> toMapCall(Class<K> keyType, Class<V> valueType) {
        Call call = toCall(Map.class, keyType, valueType);
        return call;
    }

    private MapperConfig<F> getConfig(){
        return new MapperConfig.Builder<F>().filter(filter).executor(executor).interceptor(interceptor).build();
    }
}
