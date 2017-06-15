package grace.core.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Created by hechao on 2017/1/16.
 */

public class GLogger {

    private static String TAG = "_grace_logger";

    private static Level LOG_LEVEL = Level.INFO;

    private static boolean enableTrace = true;

    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("_grace_logger");

    static {
        logger.setLevel(LOG_LEVEL);
    }

    public static void enableTrace(boolean enable){
        enableTrace = enable;
    }

    public static void setLogLevel(Level logLevel){
        LOG_LEVEL = logLevel;
    }


    public static void info(String format, Object... args) {
        logger.info(buildMessage(format, args));
    }

    public static void severe(String format, Object... args) {
        logger.severe(buildMessage(format, args));
    }

    public static void warning(String format, Object... args) {
        logger.warning(buildMessage(format, args));
    }

    /**
     * Formats the caller's provided message and prepends useful info like
     * calling thread ID and method name.
     */
    private static String buildMessage(String format, Object... args) {
        String msg ;
        try{
            msg = (args == null) ? format : String.format(Locale.US, format, args);
        }catch (Exception e){
            msg = format + Arrays.toString(args);
        }

        if(!enableTrace){
            return String.format(Locale.US, "[%s] %s",TAG, msg);
        }
        StackTraceElement[] trace = new Throwable().fillInStackTrace().getStackTrace();

        String caller = "<unknown>";
        // Walk up the stack looking for the first caller outside of SocialLog.
        // It will be at least two frames up, so start there.
        try{
            for (int i = 2; i < trace.length; i++) {
                Class<?> clazz = trace[i].getClass();
                if (!clazz.equals(GLogger.class)) {
                    String callingClass = trace[i].getClassName();
                    callingClass = callingClass.substring(callingClass.lastIndexOf('.') + 1);
                    callingClass = callingClass.substring(callingClass.lastIndexOf('$') + 1);

                    caller = callingClass + "." + trace[i].getMethodName();
                    break;
                }
            }
        }catch (Exception e){
        }
        return String.format(Locale.US, "[%d] [%s] %s", Thread.currentThread().getId(), caller, msg);
    }
}
