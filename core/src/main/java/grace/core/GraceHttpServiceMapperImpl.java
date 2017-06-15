package grace.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import grace.anno.Default;
import grace.anno.http.Body;
import grace.anno.http.Config;
import grace.anno.http.Header;
import grace.anno.http.Http;
import grace.anno.http.Https;
import grace.anno.http.Path;
import grace.anno.http.Port;
import grace.anno.http.Query;
import grace.core.factory.GraceFactory;
import grace.core.http.Call;
import grace.core.http.Executor;
import grace.core.http.Filter;
import grace.core.http.HttpService;
import grace.core.http.Interceptor;
import grace.core.http.Request;
import grace.core.http.mapper.GraceHttpServiceMapper;
import grace.core.http.mapper.MapperConfig;
import grace.core.util.GLogger;

/**
 * Created by hechao on 2017/4/12.
 */

class GraceHttpServiceMapperImpl implements GraceHttpServiceMapper {

    @Override
    public <T> T toService(Class<T> httpService) {
        if (!httpService.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }

        HttpService target = GraceFactory.GenCode.loadService(httpService);
        if(target == null) return (T) createService(httpService);

        target.setCallFactory(GraceCall.CALL_FACTORY);
        return (T) target;
    }

    private Object createService(Class httpService){
        if(!httpService.isAnnotationPresent(Https.class) && !httpService.isAnnotationPresent(Http.class)) throw new RuntimeException("Not support for :"+httpService);
        return Proxy.newProxyInstance(httpService.getClassLoader(),new Class[]{httpService},new ServiceHandler(httpService));
    }

    private static class ServiceHandler implements InvocationHandler {

        Class service;

        public ServiceHandler(Class service) {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            CallParam callParam = loadMethodCall(service,method);
            for (int i = 0 ; i < args.length ; i++){
                List<ParameterHandler> parameterHandlers = callParam.parameterHandlers.get(i);
                for (ParameterHandler handler : parameterHandlers){
                    handler.apply(callParam.request,args[i]);
                }
            }
            GLogger.info("method return type:%s",callParam.type);
            if(callParam.type instanceof ParameterizedType && ((ParameterizedType)callParam.type).getRawType() == Call.class){
                ParameterizedType parameterizedType = (ParameterizedType)callParam.type;
                Type[] actualTypes = parameterizedType.getActualTypeArguments();
                return GraceCall.CALL_FACTORY.create(callParam.request.build(),callParam.config,actualTypes.length == 0 ? Object.class : actualTypes[0]);
            }else return null;
        }
    }

    private static ConcurrentHashMap<String,TypeInfo> sServiceClassCache = new ConcurrentHashMap<>(10);

    private static ConcurrentHashMap<Method,CallParam> sMethodCallCache = new ConcurrentHashMap<>(30);

    private static CallParam loadMethodCall(Class service,Method method) throws IllegalAccessException, InstantiationException {
        if(sMethodCallCache.containsKey(method)) return sMethodCallCache.get(method);

        CallParam callParam = new CallParam();

        Class executor = Default.class;
        Class interceptor = Default.class;
        Class filter = Default.class;
        String headers = null;
        String path = null;
        for (Annotation annotation : method.getAnnotations()){
            if(annotation instanceof Config){
                Config config = (Config) annotation;
                executor = config.executor();
                filter = config.filter();
                interceptor = config.interceptor();
            }else if(annotation instanceof Header){
                headers = ((Header)annotation).value();
            }else if(annotation instanceof Path){
                path = ((Path)annotation).value();
            }
        }

        TypeInfo typeInfo = loadType(service);

        MapperConfig.Builder configBuilder = new MapperConfig.Builder<>();
        if(executor != Default.class){
            configBuilder.executor((Executor) executor.newInstance());
        }else if(typeInfo.executor != Default.class){
            configBuilder.executor((Executor) typeInfo.executor.newInstance());
        }

        if(interceptor != Default.class){
            configBuilder.interceptor((Interceptor) interceptor.newInstance());
        }else if(typeInfo.interceptor != Default.class){
            configBuilder.interceptor((Interceptor) typeInfo.interceptor.newInstance());
        }

        if(filter != Default.class){
            configBuilder.filter((Filter) filter.newInstance());
        }else if(typeInfo.filter != Default.class){
            configBuilder.filter((Filter) typeInfo.filter.newInstance());
        }
        MapperConfig config = configBuilder.build();
        Request.Builder requestBuilder
                = new Request.Builder(typeInfo.host).path(path)
                .headers(headers)
                .headers(typeInfo.headers)
                .method(typeInfo.method)
                .protocol(typeInfo.protocol);
        if(typeInfo.port > 0){
            requestBuilder.port(typeInfo.port);
        }

        callParam.config = config;
        callParam.request = requestBuilder;
        callParam.type = method.getGenericReturnType();

        //handle method parameters
        //Parameter[] parameters = method.getParameters(); 此方法在android没有,且parameters的名字在编译后会改变,
        // 因此注解值为空时我们不像在注解处理器那样，使用参数值替代
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<List<ParameterHandler>> parameterHandlers = new ArrayList<>();
        for(int i = 0 ; i < parameterAnnotations.length ; i++){
            Annotation[] annotations = parameterAnnotations[i];
            if(annotations.length == 0) continue;

            List<ParameterHandler> handlers = new ArrayList<>();
            parameterHandlers.add(handlers);

            for (Annotation annotation : annotations){

                if(annotation instanceof Body){
                    handlers.add(new ParameterHandler.Body());
                }
                if(annotation instanceof Header){
                    String value = ((Header)annotation).value();
                    handlers.add(new ParameterHandler.Header(value));
                }
                if(annotation instanceof Query){
                    String value = ((Query)annotation).value();
                    handlers.add(new ParameterHandler.Query(value));
                }
                if(annotation instanceof Path){
                    String value = ((Path)annotation).value();
                    handlers.add(new ParameterHandler.Path(value));
                }
            }
        }
        callParam.parameterHandlers = parameterHandlers;
        sMethodCallCache.put(method,callParam);
        return callParam;
    }


    private static abstract class ParameterHandler{

        public abstract void apply(Request.Builder builder,Object object);

        static final class Body extends ParameterHandler{
            @Override
            public void apply(Request.Builder builder, Object object) {
                builder.body(String.valueOf(object)).build();
            }
        }

        static final class Header extends ParameterHandler{

            public String name;

            public Header(String name) {
                this.name = name;
            }

            @Override
            public void apply(Request.Builder builder, Object object) {
                builder.header(name,String.valueOf(object)).build();
            }
        }

        static final class Query extends ParameterHandler{

            public String query;

            public Query(String query) {
                this.query = query;
            }

            @Override
            public void apply(Request.Builder builder, Object object) {
                builder.parameter(query,String.valueOf(object)).build();
            }
        }

        static final class Path extends ParameterHandler{

            String key;

            public Path(String key) {
                this.key = key;
            }

            @Override
            public void apply(Request.Builder builder, Object object) {
                String path = builder.build().path();
                if(path != null) path = path.replaceAll("\\{"+key+"\\}",String.valueOf(object));
                builder.path(path);
            }
        }
    }

    private static TypeInfo loadType(Class httpService){
        String key = httpService.getCanonicalName();
        if(sServiceClassCache.containsKey(key)) return sServiceClassCache.get(key);

        TypeInfo typeInfo = new TypeInfo();
        for (Annotation annotation : httpService.getAnnotations()){
            if(annotation instanceof Https){
                typeInfo.protocol = Request.Protocol.HTTPS;
                typeInfo.host = ((Https)annotation).value();
            }else if(annotation instanceof Http){
                typeInfo.host = ((Http)annotation).value();
                typeInfo.protocol = Request.Protocol.HTTP;
            }else if(annotation instanceof Port){
                typeInfo.port = ((Port)annotation).value();
            }else if(annotation instanceof grace.anno.http.Method){
                grace.anno.http.Method.HttpMethod method = ((grace.anno.http.Method)annotation).value();
                switch (method){
                    case DELETE:
                        typeInfo.method = Request.Method.DELETE;
                        break;
                    case GET:
                        typeInfo.method = Request.Method.GET;
                        break;
                    case POST:
                        typeInfo.method = Request.Method.POST;
                        break;
                }
            }else if(annotation instanceof Config){
                Config config = (Config) annotation;
                typeInfo.executor = config.executor();
                typeInfo.filter = config.filter();
                typeInfo.interceptor = config.interceptor();
            }else if(annotation instanceof Header){
                typeInfo.headers = ((Header)annotation).value();
            }
        }
        sServiceClassCache.put(key,typeInfo);
        return typeInfo;
    }

    private static class ConfigInfo{
        Class executor = Default.class;
        Class filter = Default.class;
        Class interceptor = Default.class;
    }

    private static class TypeInfo extends ConfigInfo{
        String headers;
        int port = -1;
        String host;
        Request.Protocol protocol;
        Request.Method method;
    }

    private static class CallParam{
        Request.Builder request;
        MapperConfig config;
        Type type;
        List<List<ParameterHandler>> parameterHandlers = new ArrayList<>(10);
    }
}
