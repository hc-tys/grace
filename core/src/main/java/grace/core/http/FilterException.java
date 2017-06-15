package grace.core.http;

/**
 * Created by hechao on 2017/4/21.
 */

public class FilterException extends Exception {

    private int code;

    public FilterException(int code,String message, Object... args) {
        super((args == null || args.length == 0) ? message : String.format( message, args));
        this.code = code;
    }

    public FilterException(String message, Object... args) {
        this(-1,message,args);
    }

    public int getCode() {
        return code;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
