package grace.core.http;

/**
 * Created by hechao on 2017/4/11.
 */

public interface Filter<F> {

    /**
     * this method can only be called when the response of the request meets that
     * the status  get by {@link Response#status()} is {@link grace.core.http.Response.Status#OK},
     * the code get by {@link Response#code()} is 200 and the body get by {@link Response#body()} is not empty.
     * @param request request
     * @param data http response data
     * @return filter data
     * @throws FilterException filter data failed
     */
    F filter(Request request,F data) throws FilterException;
}
