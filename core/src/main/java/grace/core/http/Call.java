package grace.core.http;

import java.lang.reflect.Type;

import grace.core.mapper.MapperConfig;

public interface Call<T> extends Cloneable {

    /**
     * Synchronously send the request and return its response.
     * @return 返回Response对象
     */
    Response<T> execute();

    /**
     * Asynchronously send the request and notify {@code callback} of its response.
     * @param callback 回调
     */
    void enqueue(Callback<T> callback);

    /**
     * Returns true if this call has been either {@linkplain #execute() executed} or {@linkplain
     * #enqueue(Callback) enqueued}. It is an error to execute or enqueue a call more than once.
     * @return if this call object has executed
     */
    boolean isExecuted();

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    void cancel();

    /**
     * True if {@link #cancel()} was called.
     * @return if this call object has cancelled
     */
    boolean isCancelled();

    /**
     * request
     * @return return request executed by this call.
     */
    Request request();
    /**
     * Create a new, identical call to this one which can be enqueued or executed even if this call
     * has already been.
     * @return copy to a new call object with sample request and config
     */
    Call<T> clone();

    interface Factory {

        Call<?> create(Request request, MapperConfig config, Type type);

        <T> Call<T> create(Request request, MapperConfig config, Class<T> raw, Type... typeParam);
    }
}
