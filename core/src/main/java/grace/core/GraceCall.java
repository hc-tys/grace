package grace.core;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import grace.core.factory.GraceFactory;
import grace.core.http.Call;
import grace.core.http.Callback;
import grace.core.http.Request;
import grace.core.http.Response;
import grace.core.mapper.GraceHttpMapper;
import grace.core.mapper.MapperConfig;
import grace.core.util.GLogger;

/**
 * Created by hechao on 2017/4/24.
 */

class GraceCall<F,T> implements Call<T> {

    private ExecutorService executorService = GraceFactory.Schedulers.IO;

    private AtomicBoolean isCancelled = new AtomicBoolean(false);

    private AtomicBoolean isExecuted = new AtomicBoolean(false);

    private GraceHttpMapper<F> httpMapper;

    private Type[] types;

    private Request request;

    public GraceCall(GraceHttpMapper<F> httpMapper,Request request, Type type) {
        this(httpMapper,request,new Type[]{type});
    }

    public GraceCall(GraceHttpMapper<F> httpMapper, Request request,Class raw,Type... paramTypes) {
        this(httpMapper,request,buildType(raw,paramTypes));
    }

    private GraceCall(GraceHttpMapper<F> httpMapper, Request request,Type[] types){
        GLogger.info("Types:%s" , Arrays.toString(types));
        this.httpMapper = httpMapper;
        this.types = types;
        this.request = request;
    }

    private static Type[] buildType(Class raw,Type... paramTypes){
        final Type[] types = paramTypes == null || paramTypes.length == 0 ? new Type[1] : new Type[1 + paramTypes.length];
        types[0] = raw;
        for (int i = 1 ; i < types.length; i++) {
            types[i] = paramTypes[i - 1];
        }
        return types;
    }

    @Override
    public Response<T> execute() {
        checkExecutedStatus();
        Response response = httpMapper.to(Response.class,types);
        return !isCancelled.get() ? response:
                new Response.Builder().status(Response.Status.CANNCEL).code(-1).message("Cancelled!").build();
    }

    @Override
    public void enqueue(final Callback<T> callback) {
        checkExecutedStatus();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Response response = GraceCall.this.execute();
                if (callback != null) callback.onResponse(response);
            }
        });
    }

    @Override
    public boolean isExecuted() {
        return isExecuted.get();
    }

    @Override
    public void cancel() {
        isCancelled.compareAndSet(false,true);
    }

    @Override
    public boolean isCancelled() {
        return isCancelled.get();
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public Call<T> clone() {
        return new GraceCall(httpMapper,request,types);
    }

    private void checkExecutedStatus(){
        if(!isExecuted.compareAndSet(false,true)){
            throw new RuntimeException("Already executed.");
        }
    }

    static Call.Factory CALL_FACTORY = new CallFactory();

    private static final class CallFactory implements Call.Factory{

        @Override
        public Call<?> create(Request request, MapperConfig config, Type type) {
            if(config != null) {
                GraceHttpMapper<?> graceHttpMapper
                        = Grace.from(request).filter(config.getFilter())
                        .executor(config.getExecutor())
                        .interceptor(config.getInterceptor())
                        .filter(config.getFilter());
                return new GraceCall<>(graceHttpMapper,request,type);
            }else {
                return new GraceCall<>(Grace.from(request),request,type);
            }
        }

        @Override
        public <T> Call<T> create(Request request, MapperConfig config, Class<T> raw, Type... typeParam) {
            if(config != null) {
                GraceHttpMapper<?> graceHttpMapper
                        = Grace.from(request).filter(config.getFilter())
                        .executor(config.getExecutor())
                        .interceptor(config.getInterceptor())
                        .filter(config.getFilter());
                return new GraceCall<>(graceHttpMapper,request,raw,typeParam);
            }else {
                return new GraceCall<>(Grace.from(request),request,raw,typeParam);
            }
        }
    }
}
