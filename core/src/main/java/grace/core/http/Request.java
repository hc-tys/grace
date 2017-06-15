package grace.core.http;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.internal.objects.NativeUint8Array;

/**
 * Created by hechao on 2017/4/2.
 */

public interface Request {
    enum Method {
        GET, POST,DELETE
    }

    enum Protocol {
        HTTP, HTTPS
    }

    /**
     * http method
     */
    Method method();

    /**
     * http protocol
     */
    Protocol protocol();

    /**
     * http host
     */

    String host();

    /**
     * host port
     */
    int port();

    /**
     * http path
     */
    String path();

    /**
     * http body
     */
    String body();

    /**
     * http headers
     */
    Map<String, String> headers();

    /**
     * http parameters
     */

    List<NameValuePair> parameters();

    final class Builder {

        public static final int DEFAULT_PORT = 80;

        private Method method = Method.GET;

        private Protocol protocol = Protocol.HTTP;

        private int port = DEFAULT_PORT;

        private List<NameValuePair> parameters = new ArrayList<>();

        private Map<String,String> headers = new HashMap<>();

        private final String host;

        private String path;

        private String body;

        public static Builder of(Request request){
            return new Builder(request.host())
                    .path(request.path())
                    .headers(request.headers())
                    .parameters(request.parameters())
                    .port(request.port())
                    .protocol(request.protocol())
                    .body(request.body())
                    .method(request.method());
        }

        public static Builder of(URL url){
            String queries = url.getQuery();
            List<NameValuePair> queryList = new ArrayList<>();

            if(queries != null && !queries.isEmpty()){
                Map<String,String> map = toMap(url.getQuery(),"&");
                for (Map.Entry<String,String> entry : map.entrySet()){
                    queryList.add(NameValuePair.newPair(entry.getKey(),entry.getValue()));
                }
            }

            return new Request.Builder(url.getHost()).path(url.getPath())
                    .protocol(Request.Protocol.valueOf(url.getProtocol().toUpperCase()))
                    .parameters(queryList);
        }

        public Builder(String host) {
            this.host = host;
        }

        public Builder path(String path){
            this.path = path;
            return this;
        }

        public Builder port(int port) {
            this.port = port > 0 ? port : DEFAULT_PORT;
            return this;
        }

        public Builder protocol(Protocol protocol) {
            this.protocol = protocol == null ? Protocol.HTTP : protocol;
            return this;
        }

        public Builder method(Method method) {
            this.method = method == null ? Method.GET : method;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder headers(Map<String,String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder header(String name, int value) {
            return header(name, String.valueOf(value));
        }

        public Builder header(String name, long value) {
            return header(name, String.valueOf(value));
        }

        public Builder header(String name, float value) {
            return header(name, String.valueOf(value));
        }

        public Builder header(String name, double value) {
            return header(name, String.valueOf(value));
        }

        public Builder header(String field,String value){
            if (null != field && !field.isEmpty() && null != value && !value.isEmpty()) {
                this.headers.put(field,value);
            }
            return this;
        }

        public Builder headers(String fieldValues){
            for (Map.Entry<String,String> entry : toMap(fieldValues).entrySet()){
                header(entry.getKey(),entry.getValue());
            }
            return this;
        }

        public Builder parameters(List<NameValuePair> parameters){
            this.parameters.addAll(parameters);
            return this;
        }

        /**
         * add (key, value) pair into request.
         *
         * @param key
         *            null and "" will be ignored
         * @param value
         *            null and "" will be ignored
         */
        public Builder parameter(String key, String value) {
            if (null != key && !key.isEmpty() && null != value && !value.isEmpty()) {
                NameValuePair pair = NameValuePair.newPair(key, value);
                this.parameters.add(pair);
            }
            return this;
        }

        public Builder parameter(String name, int value) {
            return parameter(name, String.valueOf(value));
        }

        public Builder parameter(String name, long value) {
            return parameter(name, String.valueOf(value));
        }

        public Builder parameter(String name, float value) {
            return parameter(name, String.valueOf(value));
        }

        public Builder parameter(String name, double value) {
            return parameter(name, String.valueOf(value));
        }

        public Request build() {
            return new RequestAdapter(this);
        }

        private static Map<String,String> toMap(String values){
            return toMap(values,";");
        }

        private static Map<String,String> toMap(String values,String delimiter){
            if(values == null || values.isEmpty()) return new HashMap<>(0);
            if(delimiter == null || delimiter.isEmpty()) return new HashMap<>(0);
            String[] pairs = values.split(delimiter);

            Map<String,String> map = new HashMap<>();

            for (String pair : pairs){
                if(pair == null || pair.isEmpty()) continue;
                int index = pair.indexOf('=');
                if(index < 0) continue;
                String key = pair.substring(0,index);
                String value = index + 1 < pair.length() ? pair.substring(index + 1) : null;
                if(key == null || key.isEmpty() || value == null || value.isEmpty()) continue;
                map.put(key,value);
            }
            return map;
        }

        private static class RequestAdapter implements Request{

            private final Method method;

            private final Protocol protocol;

            private final int port;

            private final String host;

            private final String path;

            private final String body;

            private final Map<String,String> headers;

            private final List<NameValuePair> parameters;

            public RequestAdapter(Builder builder) {
                this.method = builder.method;
                this.protocol = builder.protocol;
                this.host = builder.host;
                this.port = builder.port;
                this.path = builder.path;
                this.body = builder.body;
                this.headers = builder.headers;
                this.parameters = builder.parameters;
            }

            @Override
            public Method method() {
                return method;
            }

            @Override
            public Protocol protocol() {
                return protocol;
            }

            @Override
            public String host() {
                return host;
            }

            @Override
            public int port() {
                return port;
            }

            @Override
            public String path() {
                return path;
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Map<String, String> headers() {
                return new HashMap<>(headers);
            }

            @Override
            public List<NameValuePair> parameters() {
                return new ArrayList<>(parameters);
            }
        }
    }
}
