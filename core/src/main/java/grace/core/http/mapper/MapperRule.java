package grace.core.http.mapper;

import grace.core.http.FilterException;

/**
 * Created by hechao on 2017/5/3.
 *
 * 作用：对json串按照相应规则进行变换过滤，每个规则仅用于当前层级，即若为object，则规则应用于当前object的key-value，不会对value进行规则的递归处理，
 * 若为array，则规则应用于每个value,对每个value的key-value进行处理，对于每个value仍为array的，根据规则做不同处理。
 *
 */
public interface MapperRule<T extends MapperRule> {

    /**
     * 判断assertMode对应值是否和value想等。如想等则继续按流程处理规则，否则抛出异常{@link FilterException}
     * @throws FilterException
     * @param assertMode
     * @param value
     * @return
     */
    T assertEqual(String assertMode, String value);

    /**
     *
     * @throws FilterException
     * @param assertMode
     * @param value
     * @return
     * @see #assertEqual(String, String)
     */
    T assertEqual(String assertMode, int value);

    /**
     * @throws FilterException
     * @param assertMode
     * @param value
     * @return
     * @see #assertEqual(String, String)
     */
    T assertEqual(String assertMode, long value);

    /**
     * @throws FilterException
     * @param assertMode
     * @param value
     * @return
     * @see #assertEqual(String, String)
     */
    T assertEqual(String assertMode, boolean value);

    /**
     * @throws FilterException
     * @param assertMode
     * @param value
     * @return
     * @see #assertEqual(String, String)
     */
    T assertEqual(String assertMode, float value);

    /**
     * 判断assertMode对应值是否和value想等。如想等则继续按流顺序处理规则，否则抛出异常{@link FilterException}
     * 若errorMode不为空，则使用其对应的value值作为异常信息，否则使用系统自定义信息
     * @throws FilterException
     * @param assertMode
     * @param value
     * @return
     * @see #assertEqual(String, String)
     */
    T assertEqual(String assertMode, String errorMode, String value);

    /**
     *
     * @throws FilterException
     * @param assertMode
     * @param errorMode
     * @param value
     * @return
     * @see #assertEqual(String, String, String)
     */
    T assertEqual(String assertMode, String errorMode, int value);

    /**
     *
     * @throws FilterException
     * @param assertMode
     * @param errorMode
     * @param value
     * @return
     * @see #assertEqual(String, String, String)
     */
    T assertEqual(String assertMode, String errorMode, long value);

    /**
     *
     * @throws FilterException
     * @param assertMode
     * @param errorMode
     * @param value
     * @return
     * @see #assertEqual(String, String, String)
     */
    T assertEqual(String assertMode, String errorMode, float value);

    /**
     *
     * @throws FilterException
     * @param assertMode
     * @param errorMode
     * @param value
     * @return
     * @see #assertEqual(String, String, String)
     */
    T assertEqual(String assertMode, String errorMode, boolean value);

    /**
     * 若为object，则遍历其key-value，将key值为originMode的key-value替换为key值为targetMode的key-value，value值保持不变。
     * 若为array，则应用规则到每个value，若value为非object,则抛出异常{@link FilterException}
     * @throws FilterException
     * @param originMode 非空，否则抛出异常{@link FilterException}
     * @param targetMode 非空，否则抛出异常{@link FilterException}
     * @return
     */
    T alias(String originMode, String targetMode);

    /**
     * 若为object，则遍历其key-value，将不包含在originModes中key对应的key-value过滤掉
     * 若为array，则应用规则到每个value，若value为非object,则抛出异常{@link FilterException}
     * @param originModes
     * @return
     */
    T include(String... originModes);

    /**
     * 若为object，则遍历其key-value，将包含在originModes中key对应的key-value过滤掉
     * 若为array，则应用规则到每个value，若value为非object,则抛出异常{@link FilterException}
     * @throws FilterException
     * @param originModes
     * @return
     */
    T exclude(String... originModes);

    /**
     * 若为object，则过滤掉匹配regexMode的key对应的key-value，将其中的value组成array，做为新value和新key targetMode一起组成新的key-value添加到原有json中
     * 若为array，则应用规则到每个value，若value为非object，则抛出异常{@link FilterException}
     * @throws FilterException
     * @param regexMode 正则表达式 , 非空，否则抛出异常{@link FilterException}
     * @param targetMode  非空，否则抛出异常{@link FilterException}
     * @return
     */
    T array(String regexMode, String targetMode);

    /**
     * 若为object，则结果仅保留其key值为originMode对应的value值。
     * 若为array，则应用规则到每个value，若value为非object,则抛出异常{@link FilterException}
     *
     * 与{@link #include(String...)}不同之处在于仅保留key-value对应的value值，不包含key值。
     *
     * @throws FilterException
     * @param originMode
     * @return
     */
    T get(String originMode);

    /**
     * 过滤结果为array的index值为originMode对应的value。非array将抛出异常{@link FilterException}
     * @throws FilterException
     * @param originMode
     * @return
     */
    T get(int originMode);

    /**
     * 若为object，则过滤掉key为keyMode和valueMode的key-value，然后将keyMode对应的value作为key，valueMode对应value作为value，添加到json中,
     * 其中keyMode对应的value值必须为数值或者字符串，否则抛出异常{@link FilterException}
     * 若为array，则应用规则到每个value，若value为非object,则抛出异常{@link FilterException}
     * @throws FilterException
     * @param keyMode  非空，否则抛出异常{@link FilterException}
     * @param valueMode  非空，否则抛出异常{@link FilterException}
     * @return
     */
    T map(String keyMode,String valueMode);

    /**
     * 若为object，将keyMode对应的value作为key，valueMode对应value作为value，组成object作为结果
     * 其中keyMode对应的value值必须为数值或者字符串，否则抛出异常{@link FilterException}
     * 若为array，则应用规则到每个value，生成的key-value添加到同一个object中作为结果。
     * 若value为非object,则抛出异常{@link FilterException}
     * @throws FilterException
     * @param keyMode  非空，否则抛出异常{@link FilterException}
     * @param valueMode  非空，否则抛出异常{@link FilterException}
     * @return
     */
    T collectMap(String keyMode,String valueMode);

    /**
     * 对json应用规则
     * @return
     * @throws FilterException
     */
    String apply() throws FilterException;

    /**
     * 内部调用{@link #apply()},发生异常时返回null
     * @return
     */
    String quietApply();
}
