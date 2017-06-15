package grace.core.http.mapper;

import grace.core.http.Executor;
import grace.core.http.Filter;
import grace.core.http.Interceptor;

/**
 * Created by hechao on 2017/6/10.
 */
public interface MapperConfig<F> {

    Executor<F> getExecutor();

    Interceptor getInterceptor();

    Filter<F> getFilter();

    final class Builder<F> {

        private Executor<F> executor;

        private Interceptor interceptor;

        private Filter<F> filter;

        public static <F> Builder<F> of(MapperConfig<F> config) {
            return config != null ? new Builder().executor(config.getExecutor())
                    .filter(config.getFilter())
                    .interceptor(config.getInterceptor()) : new Builder<F>();
        }

        public Builder<F> executor(Executor<F> executor) {
            this.executor = executor;
            return this;
        }

        public Builder<F> interceptor(Interceptor interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public Builder<F> filter(Filter<F> filter) {
            this.filter = filter;
            return this;
        }

        public MapperConfig<F> build() {
            return new Builder.HttpMapperConfigAdapter<>(this);
        }

        private static final class HttpMapperConfigAdapter<F> implements MapperConfig<F> {

            private Executor<F> executor;

            private Interceptor interceptor;

            private Filter<F> filter;

            public HttpMapperConfigAdapter(Builder<F> builder) {
                this.executor = builder.executor;
                this.interceptor = builder.interceptor;
                this.filter = builder.filter;
            }

            @Override
            public Executor<F> getExecutor() {
                return executor;
            }

            @Override
            public Interceptor getInterceptor() {
                return interceptor;
            }

            @Override
            public Filter getFilter() {
                return filter;
            }
        }
    }
}
