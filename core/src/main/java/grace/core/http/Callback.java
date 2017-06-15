package grace.core.http;

public interface Callback<T> {

    /**
     * Invoked for a received HTTP response.
     * @param response http response
     */
    void onResponse(Response<T> response);
}
