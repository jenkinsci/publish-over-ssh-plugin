package jenkins.plugins.publish_over_ssh;

import hudson.util.SecretHelper;

public class MySecretHelper extends SecretHelper {
    private static final String KEY = "7b41d2675f759b479460a41a9758bd0f6049a23ac685dc739bf7bb2d2319472d";

    public static void setSecretKey() {
        set(KEY);
    }

    public static void clearSecretKey() {
        set(null);
    }
}
