package grace.core.http;

/**
 * Created by hechao on 2017/4/2.
 */

public interface Executor<F>{

    interface SynExecutor<F> extends Executor<F>{
        Response<F> executeRequest(Request request);
    }

    interface ASynExecutor<F> extends Executor<F>{
        void executeRequest(Request request,Callback<F> callback);
    }
}
