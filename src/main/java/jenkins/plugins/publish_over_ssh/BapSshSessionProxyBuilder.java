package jenkins.plugins.publish_over_ssh;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;

public class BapSshSessionProxyBuilder {

    public static final String HTTP_PROXY_TYPE = "http";
    public static final String SOCKS_4_PROXY_TYPE = "socks4";
    public static final String SOCKS_5_PROXY_TYPE = "socks5";

    private String proxyHost;
    private int proxyPort;
    private String proxyType;
    private String username;
    private String password;

    public BapSshSessionProxyBuilder withProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public BapSshSessionProxyBuilder withProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public BapSshSessionProxyBuilder withProxyType(String proxyType) {
        this.proxyType = proxyType;
        return this;
    }

    public BapSshSessionProxyBuilder withUser(String username, String password) {
        if(username != null && password != null) {
            this.username = username;
            this.password = password;
        }
        return this;
    }

    public Proxy build() {

        ProxyHTTP proxyHTTP = null;
        ProxySOCKS4 proxySOCKS4 = null;
        ProxySOCKS5 proxySOCKS5 = null;

        switch(proxyType) {
            case HTTP_PROXY_TYPE:
                proxyHTTP = new ProxyHTTP(proxyHost, proxyPort);
                proxyHTTP.setUserPasswd(username, password);
                return proxyHTTP;
            case SOCKS_4_PROXY_TYPE:
                proxySOCKS4 = new ProxySOCKS4(proxyHost, proxyPort);
                proxySOCKS4.setUserPasswd(username, password);
                return proxySOCKS4;
            case SOCKS_5_PROXY_TYPE:
                proxySOCKS5 = new ProxySOCKS5(proxyHost, proxyPort);
                proxySOCKS5.setUserPasswd(username, password);
                return proxySOCKS5;
            default:
                return null;
            }
    }
}
