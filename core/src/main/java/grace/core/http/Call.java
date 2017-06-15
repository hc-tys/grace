package grace.core.http;

import java.lang.reflect.Type;

import grace.core.http.mapper.MapperConfig;

public interface Call<T> extends Cloneable {
    /**
     * Synchronously send the request and return its response.
     */
    Response<T> execute();

    /**
     * Asynchronously send the request and notify {@code callback} of its response.
     */
    void enqueue(Callback<T> callback);

    /**
     * Returns true if this call has been either {@linkplain #execute() executed} or {@linkplain
     * #enqueue(Callback) enqueued}. It is an error to execute or enqueue a call more than once.
     */
    boolean isExecuted();

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    void cancel();

    /**
     * True if {@link #cancel()} was called.
     */
    boolean isCanceled();

    /**
     * request
     * @return
     */
    Request request();
    /**
     * Create a new, identical call to this one which can be enqueued or executed even if this call
     * has already been.
     */
    Call<T> clone();

    interface Factory {

        Call<?> create(Request request, MapperConfig config, Type type);

        <T> Call<T> create(Request request, MapperConfig config, Class<T> raw, Type... typeParam);
    }
}
