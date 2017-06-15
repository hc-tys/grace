package grace.compiler;

/**
 * Created by hechao on 2017/1/10.
 */

public interface ASTPrinter {

    void printError(String format, Object... args);

    void printWarning(String format, Object... args);

    void printInfo(String format, Object... args);
}
