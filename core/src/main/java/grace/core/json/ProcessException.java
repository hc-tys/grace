package grace.core.json;

/**
 * Created by hechao on 2017/5/16.
 */

public class ProcessException extends Exception {

    public ProcessException(String message) {
        super(message);
    }

    public ProcessException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
