package com.authentication.auth.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class DomainConfigProperties {

    private String siteDomain;
    private String serverCookieDomain;

    public String getSiteDomain() {
        return siteDomain;
    }

    public void setSiteDomain(String siteDomain) {
        this.siteDomain = siteDomain;
    }

    public String getServerCookieDomain() {
        return serverCookieDomain;
    }

    public void setServerCookieDomain(String serverCookieDomain) {
        this.serverCookieDomain = serverCookieDomain;
    }
}
