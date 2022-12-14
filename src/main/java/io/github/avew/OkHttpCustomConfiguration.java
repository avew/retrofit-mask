package io.github.avew;


import com.bartoszwesolowski.okhttp3.logging.CustomizableHttpLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang.StringUtils;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("SpellCheckingInspection")

@RequiredArgsConstructor
@Slf4j
public class OkHttpCustomConfiguration {

    private CustomHttpConfig config;
    private List<String> masking = new ArrayList<>();
    private boolean debug;

    public OkHttpCustomConfiguration(CustomHttpConfig config, List<String> masking, boolean debug) {
        this.config = config;
        this.masking = masking;
        this.debug = debug;
    }


    public Retrofit.Builder builder() throws RuntimeException {
        try {

            OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder();

            httpClient.connectTimeout(config.getCustomTimeout().getConnectTimeout(), config.getCustomTimeout().getConnectTimeoutUnit());
            httpClient.readTimeout(config.getCustomTimeout().getReadTimeout(), config.getCustomTimeout().getReadTimeoutUnit());
            httpClient.writeTimeout(config.getCustomTimeout().getWriteTimeout(), config.getCustomTimeout().getWriteTimeoutUnit());

            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("User-Agent", config.getAgent())
                        .build();
                return chain.proceed(request);
            });
            if (config.isMasking()) {
                httpClient.addNetworkInterceptor(customNetworkInterceptors());
            } else {
                httpClient.addInterceptor(httpLoggingInterceptor());
            }

            this.setTrustManager(httpClient);

            if (config.isProxy()) {
                log.info("CONNECTION TO {} USE PROXY", config.getUrl());

                if (config.isProxyAuth()) {
                    log.info("CONNECTION TO {} USE PROXY WITH AUTH", config.getUrl());
                    Authenticator proxyAuthenticator = (route, response) -> {
                        String credential = Credentials.basic(config.getProxyUsername(), config.getProxyPassword());
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    };
                    httpClient.proxyAuthenticator(proxyAuthenticator);
                }

                httpClient.proxySelector(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {

                        // Host
                        final String host = uri.getHost();

                        String noProxyHost = config.getUrlSkipProxy().stream().collect(Collectors.joining(","));
                        if (StringUtils.contains(noProxyHost, host)) {
                            return List.of(Proxy.NO_PROXY);
                        } else {
                            // Add Proxy
                            return List.of(new Proxy(Proxy.Type.HTTP,
                                    new InetSocketAddress(config.getProxyHost(), config.getProxyPort())));
                        }

                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                        throw new UnsupportedOperationException("Proxy Not supported yet.");
                    }
                });
            }

            OkHttpClient client = httpClient.build();

            return new Retrofit.Builder()
                    .baseUrl(config.getUrl())
                    .client(client)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    private void setTrustManager(OkHttpClient.Builder httpClient) throws NoSuchAlgorithmException, KeyManagementException {
        /* create a trust manager that does not validate certificate chains */
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        httpClient.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        httpClient.hostnameVerifier((String s, SSLSession sslSession) -> true);
    }

    private String maskingLog(List<String> words, String msg) {
        List<String> maskPatterns = words.stream().map(s -> "\"([" + s + "\"]+)\"\\s*:\\s*\"([^\"]+)\",?").collect(Collectors.toList());
        Pattern multilinePattern = Pattern.compile(String.join("|", maskPatterns), Pattern.MULTILINE);
        StringBuilder sb = new StringBuilder(msg);
        Matcher matcher = multilinePattern.matcher(sb);
        while (matcher.find()) {
            IntStream.rangeClosed(1, matcher.groupCount()).forEach(group -> {
                if (matcher.group(group) != null) {
                    IntStream.range(matcher.start(group), matcher.end(group)).forEach(i -> sb.setCharAt(i, '*'));
                }
            });
        }
        return sb.toString();
    }

    private HttpLoggingInterceptor httpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return httpLoggingInterceptor;
    }

    private CustomizableHttpLoggingInterceptor customNetworkInterceptors() {
        CustomizableHttpLoggingInterceptor logging = new CustomizableHttpLoggingInterceptor(
                (String msg) -> {
                    String maskingLog = maskingLog(masking, msg);
                    if (debug) {
                        if (StringUtils.isNotBlank(maskingLog))
                            log.info("HTTP {}", maskingLog(masking, msg));
                    }
                },
                debug,
                debug);
        logging.redactHeader("Authorization");
        logging.redactHeader("Cookie");
        return logging;
    }

}
