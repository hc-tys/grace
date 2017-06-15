package grace.core.http.mapper;

import grace.core.http.Executor;
import grace.core.http.Filter;
import grace.core.http.Interceptor;

/**
 * Created by hechao on 2017/4/23.
 */

public interface GraceHttpMapper<F> extends GraceMapper{

    GraceHttpMapper<F> executor(Executor<F> executor);

    GraceHttpMapper<F> interceptor(Interceptor interceptor);

    GraceHttpMapper<F> filter(Filter<F> filter);
}
