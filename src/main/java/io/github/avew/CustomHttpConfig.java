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
}
