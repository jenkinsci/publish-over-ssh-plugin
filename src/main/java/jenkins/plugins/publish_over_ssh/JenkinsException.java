package jenkins.plugins.publish_over_ssh;

public class JenkinsException extends NullPointerException {

    @Override
    public String getMessage() {
        return "Jenkins is not ready or is going to be offline.";
    }

}
