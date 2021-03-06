package grace.core;

import java.net.MalformedURLException;
import java.net.URL;

import grace.core.factory.GraceFactory;
import grace.core.mapper.Converter;
import grace.core.http.Executor;
import grace.core.http.Request;
import grace.core.mapper.GraceHttpCallMapper;
import grace.core.mapper.GraceMapper;
import grace.core.mapper.GraceRuleMapper;
import grace.core.mapper.MapperConfig;

/**
 * Created by hechao on 2017/4/4.
 */

public class Grace {

    private static Converter.Factory DEFAULT_CONVERTER_FACTORY = GraceFactory.createConvertFactory();

    private static Executor sExecutor = GraceFactory.createStringSynExecutor();

    private static Converter.Factory sConvertFactory = DEFAULT_CONVERTER_FACTORY;

    public static <S> S from(Class<S> service){
        return new GraceHttpServiceMapperImpl().toService(service);
    }

    public static GraceHttpCallMapper from(Request request){
        return new GraceHttpCallMapperImpl(new MapperConfig.Builder<String>().executor(sExecutor).build(),request,sConvertFactory, GraceCall.CALL_FACTORY);
    }

    public static GraceHttpCallMapper from(URL url){
        return from(Request.Builder.of(url).build());
    }

    public static GraceHttpCallMapper fromUrl(String url){
        try {
            URL urlObj = new URL(url);
            return from(urlObj);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("invalid url:"+url);
        }
    }

    public static GraceMapper from(byte[] value){
        return new GraceMapperImpl(value,sConvertFactory);
    }

    public static GraceMapper from(String value){
        return new GraceMapperImpl(value, sConvertFactory);
    }

    public static String toString(Object value){
        return new GraceMapperImpl(value, sConvertFactory).to(String.class);
    }

    public static GraceRuleMapper filter(String value){
        return new GraceFilterMapperImpl(value,sConvertFactory);
    }

    public static void setDefaultExecutor(Executor executor){
        sExecutor = executor;
    }

    public static void setDefaultConverterFactory(Converter.Factory converterFactory){
        sConvertFactory = converterFactory;
    }
}
