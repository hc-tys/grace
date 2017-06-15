package grace.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import grace.core.http.NameValuePair;
import grace.core.http.Request;

/**
 * Created by hechao on 2017/4/4.
 */

public class ExecutorUtil {

    public static final String CHARSET_UTF_8 = "UTF-8";
    
    public static String formUrl(Request request){
        StringBuilder sb = new StringBuilder();
        sb.append(request.protocol().name().toString().toLowerCase()).append("://").append(request.host());
        if(request.port() != 80 && request.port() > 0){
            sb.append(":").append(request.port());
        }
        if(request.path() != null && !request.path().isEmpty()){
            sb.append(request.path());
        }
        if(request.parameters().size() > 0) {
            sb.append("?").append(ExecutorUtil.fromParamListToString(request.parameters()));
        }
        return sb.toString();
    }

    public static String fromParamListToString(List<NameValuePair> nameValuePairs) {
        return joinNameValuePair(nameValuePairs, "&",true);
    }

    public static String joinNameValuePair(List<NameValuePair> nameValuePairs, String sp, boolean encode) {

        if(nameValuePairs == null || nameValuePairs.size() <= 0){
            return "";
        }

        StringBuilder params = new StringBuilder();

        for (NameValuePair pair : nameValuePairs) {
            try {
                if (pair == null || pair.getValue() == null || pair.getName() == null)
                    continue;
                String name = encode ? URLEncoder.encode(pair.getName(), CHARSET_UTF_8)  : pair.getName();
                String value =encode ? URLEncoder.encode(pair.getValue(), CHARSET_UTF_8) : pair.getValue();

                params.append(name).append("=").append(value).append(sp);

            } catch (UnsupportedEncodingException e) {
                GLogger.info("Failed to convert param  to string : %s ",e.getMessage());
                return null;
            }
        }
        if (params.length() > 0) {
            params = params.deleteCharAt(params.length() - 1);
        }
        return params.toString();
    }


}
