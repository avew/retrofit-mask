package io.github.avew;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomHttpConfig {
    private String url;
    @Builder.Default
    private boolean proxy = false;
    @Builder.Default
    private boolean proxyAuth = false;
    private String proxyUsername;
    private String proxyPassword;
    private List<String> urlSkipProxy;
    private String proxyHost;
    private int proxyPort;
    private String agent;
    @Builder.Default
    private CustomTimeout customTimeout = new CustomTimeout();


    @Builder.Default
    private boolean masking = true;
    @Builder.Default
    private boolean retryConnectionFailure = false;

    @Override
    public String toString() {
        return "CustomHttpConfig{" +
                "url='" + url + '\'' +
                ", proxy=" + proxy +
                ", proxyAuth=" + proxyAuth +
                ", proxyUsername='" + proxyUsername + '\'' +
                ", proxyPassword='" + proxyPassword + '\'' +
                ", urlSkipProxy=" + urlSkipProxy +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", agent='" + agent + '\'' +
                ", customTimeout=" + customTimeout +
                '}';
    }
}
