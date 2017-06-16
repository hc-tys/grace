package grace.core.http;

import java.util.Map;

/**
 * Created by hechao on 2017/4/2.
 */

public interface Response<T> {

    /**
     * The deserialized response body
     *
     * @return the deserialized response body
     */
    T body();

    /**
     * http headers
     *
     * @return http response headers
     */
    Map<String, String> headers();

    /**
     * error message;
     * if the status is {@link Status#USER} , the value is {@link FilterException#getMessage()};
     *
     * @return error message
     */
    String message();

    /**
     * HTTP status code.
     * If the status is {@link Status#NO_NETWORK} or {@link Status#INVALID_URL}
     * or {@link Status#CONNECTION_FAILED} or {@link Status#UNEXPECTED},the value is -1;
     * if the status is {@link Status#USER} , the value is {@link FilterException#getCode()};
     * if the status is{@link Status#OK},the value is http response code;
     *
     * @return  code
     */
    int code();

    /**
     * response status
     * @return response status
     */
    Status status();

    enum Status {

        /**
         * Get http Response code success ,such as 200 ,also may be 404 or 500
         */
        OK,

        /**
         * No available network
         */
        NO_NETWORK,

        /**
         * invalid url
         */
        INVALID_URL,

        /**
         * create and open connection failed for the url
         */
        CONNECTION_FAILED,

        /**
         * 取消
         */
        CANNCEL,

        /**
         * other unexpected error
         */
        UNEXPECTED,

        /**
         * this statue only be return when {@link Filter} is called and an exception of {@link FilterException} is thrown
         */
        USER

    }

    final class Builder{

        private Map<String,String> headers;

        private String message;

        private int code;

        private Status status;

        public static Builder of(Response response){
            return new Builder().headers(response.headers())
                    .code(response.code())
                    .status(response.status())
                    .message(response.message());
        }

        public Builder headers(Map<String, String> headers) {
            this.headers =  headers;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public <T> Response<T> build(T body){
            return new ResponseAdapter(body,this);
        }

        public Response build(){
            return new ResponseAdapter(null,this);
        }

        private static class ResponseAdapter<T> implements Response<T>{

            private T body ;

            private Map<String,String> headers;

            private String message;

            private int code;

            private Status status;

            public ResponseAdapter(T body, Builder  builder) {
                this.body = body;
                this.headers = builder.headers;
                this.message = builder.message;
                this.code = builder.code;
                this.status = builder.status;
            }

            @Override
            public T body() {
                return body;
            }

            @Override
            public Map<String, String> headers() {
                return headers;
            }

            @Override
            public String message() {
                return message;
            }

            @Override
            public int code() {
                return code;
            }

            @Override
            public Status status() {
                return status;
            }
        }
    }
}
