package io.github.avew;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomHttpConfig {

    private String url;

    @Builder.Default
    private CustomProxy proxy = new CustomProxy();

    @Builder.Default
    private String agent = null;

    @Builder.Default
    private CustomTimeout customTimeout = new CustomTimeout();

    @Builder.Default
    private boolean masking = true;
    @Builder.Default
    private boolean retryConnectionFailure = false;
    @Builder.Default
    private List<String> excludeHeaders = new ArrayList<>();

    @Builder.Default
    private HttpTls TLS = HttpTls.TLS1_2;

    public static String mask(String value) {
        if (value == null) {
            return null;
        }

        char[] chs = new char[value.length()];
        for (int i = 1; i < chs.length; i++) {
            chs[i] = '*';
        }
        chs[0] = value.charAt(0);
        chs[chs.length - 1] = value.charAt(value.length() - 1);
        return new String(chs);
    }


}
