package io.github.avew;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.avew.CustomHttpConfig.mask;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomProxy {

    @Builder.Default
    private boolean proxy = false;
    @Builder.Default
    private boolean auth = false;
    private String username;
    private String password;
    @Builder.Default
    private List<String> urlSkip = new ArrayList<>();
    private String host;
    private int port;

    @Override
    public String toString() {
        return "CustomProxy{" +
                "proxy=" + proxy +
                ", auth=" + auth +
                ", username='" + mask(username) + '\'' +
                ", password='" + mask(password) + '\'' +
                ", urlSkip=" + urlSkip +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
