package grace.core.http;

/**
 * Created by hechao on 2017/4/8.
 */

public interface Interceptor{
    Request intercept(Request request);
}
