package grace.core.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLContext;

import grace.core.mapper.Converter;
import grace.core.http.Executor;
import grace.core.http.HttpService;
import grace.core.util.GLogger;

/**
 * Created by hechao on 2017/4/4.
 */

public final class GraceFactory {

    public static Executor.SynExecutor<String> createStringSynExecutor(){
        return new SimpleExecutor();
    }

    public static Executor.SynExecutor<String> createStringSynExecutor(SSLContext sslContext, List<String> hostNames){
        return new SimpleExecutor(sslContext,hostNames);
    }

    public static Converter.Factory createConvertFactory(){
        return new JacksonConvertFactory();
    }

    public interface Schedulers{
        ExecutorService IO = java.util.concurrent.Executors.newCachedThreadPool();
    }

    public static final class GenCode{
        public static HttpService loadService(Class service){
            return sGenCodeFactory.getAllHttpService().get(service.getCanonicalName());
        }
        static Class<?> loadJsonClass(Class service){
            return sGenCodeFactory.getAllJSONSerializer().get(service.getCanonicalName());
        }
    }

    public interface GenCodeFactory{
        Map<String,Class> getAllJSONSerializer();

        Map<String,HttpService> getAllHttpService();
    }

    static GenCodeFactory sGenCodeFactory;
    static {
        try {
            sGenCodeFactory = (GenCodeFactory) Class.forName("grace.codegen._Grace_Factory").newInstance();
            GLogger.info("load factory success");
        } catch (Exception e) {
            GLogger.warning("load factory failed");
            sGenCodeFactory = new GenCodeFactory() {
                @Override
                public Map<String, Class> getAllJSONSerializer() {
                    return new HashMap<>();
                }

                @Override
                public Map<String, HttpService> getAllHttpService() {
                    return new HashMap<>();
                }
            };
        }
    }
}
