package grace.core.http.mapper;

/**
 * Created by hechao on 2017/4/23.
 */

public interface GraceHttpCallMapper<F> extends Mapper.RawCallMapper,Mapper.CollectionCallMapper,GraceHttpMapper<F> {
}
