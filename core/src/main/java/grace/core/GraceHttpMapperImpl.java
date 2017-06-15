package grace.core;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import grace.core.factory.GraceFactory;
import grace.core.http.Callback;
import grace.core.http.Executor;
import grace.core.http.Filter;
import grace.core.http.FilterException;
import grace.core.http.Interceptor;
import grace.core.http.Request;
import grace.core.http.Response;
import grace.core.mapper.Converter;
import grace.core.mapper.GraceHttpMapper;
import grace.core.mapper.MapperConfig;

/**
 * Created by hechao on 2017/4/23.
 */

class GraceHttpMapperImpl<F> extends GraceMapperImpl implements GraceHttpMapper<F> {

    Executor<F> executor;

    Interceptor interceptor;

    Filter<F> filter;

    Request request;

    ThreadLocal<Response> responseCache;

    GraceHttpMapperImpl(MapperConfig<F> config, Request request, Converter.Factory factory) {
        super(request,factory);
        this.executor = config.getExecutor();
        this.interceptor = config.getInterceptor();
        this.filter = config.getFilter();
        this.request = request;
        responseCache = new ThreadLocal<>();
    }

    @Override
    public GraceHttpMapper<F> executor(Executor<F> executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public GraceHttpMapper<F> interceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    @Override
    public GraceHttpMapper<F> filter(Filter<F> filter) {
        this.filter = filter;
        return this;
    }

    @Override
    Object transformTarget(Object source) {
        Request request = (Request) source;
        Response response =  execute(request);
        responseCache.set(response);
        return response.body();
    }

        @Override
    public <T> T to(Class<T> raw, Type... typeParam) {
        if(raw == Response.class){
            if(typeParam == null || typeParam.length == 0) return (T) execute(request);
            final Object value;
            if(typeParam.length == 1){
                value = super.to(typeParam[0]);
            } else{
                value = super.to((Class) typeParam[0], Arrays.copyOfRange(typeParam,1,typeParam.length));
            }
            Response target = Response.Builder.of(responseCache.get()).build(value);
            return (T) target;
        }
        return super.to(raw, typeParam);
    }

    private Response<F> execute(Request request){

        Request targetRequest = interceptor != null ? interceptor.intercept(request) : request;

        if(request == null) return new Response.Builder().message("no request").status(Response.Status.INVALID_URL).code(-1).build();

        final Executor.SynExecutor<F> targetExecutor;
        if(executor instanceof Executor.SynExecutor){
            targetExecutor = (Executor.SynExecutor) executor;
        }else if(executor instanceof Executor.ASynExecutor){
            targetExecutor = toSynExecutor((Executor.ASynExecutor)executor);
        }else {
            targetExecutor = (Executor.SynExecutor<F>) GraceFactory.createStringSynExecutor();
        }

        final Response<F> response = targetExecutor.executeRequest(targetRequest);

        if(filter == null || !isValidResponse(response)) return response;

        try {
            F filterBody = filter.filter(targetRequest,response.body());
            return Response.Builder.of(response).build(filterBody);
        } catch (FilterException e) {
            e.printStackTrace();
            return Response.Builder.of(response).status(Response.Status.USER).code(e.getCode()).message(e.getMessage()).build();
        }
    }

    private static boolean isValidResponse(Response response){
        if(response.status() != Response.Status.OK || response.code() != 200 || response.body() == null) return false;

        Object body = response.body();
        if( body instanceof String ) return ((String) body).length() != 0;
        else if( body.getClass().isArray()) return Array.getLength(body) != 0;
        else if(body instanceof Collection) return ((Collection) body).size() != 0;
        else return true;
    }

    private static <T> Executor.SynExecutor<T> toSynExecutor(final Executor.ASynExecutor<T> asynExecutor){
        return (Executor.SynExecutor) new Executor.SynExecutor() {
            @Override
            public Response executeRequest(Request request) {
                final CallFuture<Response<T>> future = new CallFuture<>();
                asynExecutor.executeRequest(request, new Callback<T>() {
                    @Override
                    public void onResponse(Response<T> response) {
                        future.setResult(response);
                    }
                });
                try {
                    return future.getResult();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    return new Response.Builder().status(Response.Status.UNEXPECTED).code(-1).message("Unexpected execute exception ").build();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return new Response.Builder().status(Response.Status.UNEXPECTED).code(-1).message("Execute interrupted").build();
                }
            }
        };
    }

    private static class CallFuture<T> extends FutureTask<T> {

        public CallFuture() {
            super(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    return null;
                }
            });
        }

        public void setResult(T result){
            set(result);
        }

        public T getResult() throws ExecutionException, InterruptedException {
            return get();
        }
    }
}
