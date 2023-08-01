package io.github.avew;


import com.bartoszwesolowski.okhttp3.logging.CustomizableHttpLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Authenticator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Slf4j
public class VewHttp {

    private CustomHttpConfig config;
    private List<String> masking = new ArrayList<>();
    private boolean debug;
    private OkHttpClient client;

    public VewHttp(CustomHttpConfig config, List<String> masking, boolean debug) {
        this.config = config;
        this.masking = masking;
        this.debug = debug;
    }

    public VewHttp(CustomHttpConfig config, OkHttpClient client, List<String> masking, boolean debug) {
        this.config = config;
        this.masking = masking;
        this.debug = debug;
        this.client = client;
    }

    public Retrofit.Builder builder() throws RuntimeException {
        try {
            return new Retrofit.Builder()
                    .baseUrl(config.getUrl())
                    .client(client == null ? setupClient() : client)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @NotNull
    public OkHttpClient setupClient() throws NoSuchAlgorithmException, KeyManagementException {
        OkHttpClient.Builder httpClient = new OkHttpClient().newBuilder();

        httpClient.connectTimeout(config.getCustomTimeout().getConnectTimeout(), config.getCustomTimeout().getConnectTimeoutUnit());
        httpClient.readTimeout(config.getCustomTimeout().getReadTimeout(), config.getCustomTimeout().getReadTimeoutUnit());
        httpClient.writeTimeout(config.getCustomTimeout().getWriteTimeout(), config.getCustomTimeout().getWriteTimeoutUnit());
        httpClient.retryOnConnectionFailure(config.isRetryConnectionFailure());
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder request = original.newBuilder();
            if (config.getAgent() != null)
                request.header("User-Agent", config.getAgent());
            if (config.isRetryConnectionFailure()) request.header("Connection", "close");

            if (config.getAccessToken() != null)
                request.header("Authorization", "Bearer " + config.getAccessToken());

            if (config.getClientId() != null)
                request.header("X-Client", config.getClientId());

            return chain.proceed(request.build());
        });
        httpClient.addNetworkInterceptor(customNetworkInterceptors());

        ConnectionSpec connectionSpecClearText = new ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT)
                .build();
        switch (config.getTLS()) {
            case TLS1_1:
                ConnectionSpec tls11 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_0, TlsVersion.TLS_1_1)
                        .build();

                httpClient.connectionSpecs(Arrays.asList(tls11, connectionSpecClearText));
                break;
            case TLS1_2:
                ConnectionSpec tls12 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();
                httpClient.connectionSpecs(Arrays.asList(tls12, connectionSpecClearText));
                break;
            case TLS1_3:
                ConnectionSpec tls13 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_3)
                        .build();
                httpClient.connectionSpecs(Arrays.asList(tls13, connectionSpecClearText));
                break;
        }

        this.setTrustManager(httpClient);

        if (config.getProxy() != null) {
            CustomProxy proxy = config.getProxy();
            if (proxy.isProxy()) {
                log.info("CONNECTION TO {} USE PROXY", config.getUrl());

                if (proxy.isAuth()) {
                    log.info("CONNECTION TO {} USE PROXY WITH AUTH AND PROXY TYPE {}", config.getUrl(), proxy.getType());
                    switch (proxy.getType()) {
                        case SOCKS:
                            java.net.Authenticator.setDefault(new java.net.Authenticator() {
                                private final PasswordAuthentication authentication = new PasswordAuthentication(proxy.getUsername(), proxy.getPassword().toCharArray());

                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return authentication;
                                }
                            });
                            httpClient.proxy(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(proxy.getHost(), proxy.getPort())));
                            break;
                        case HTTP:
                            Authenticator proxyAuthenticator = (route, response) -> {
                                String credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                                return response.request().newBuilder()
                                        .header("Proxy-Authorization", credential)
                                        .build();
                            };
                            httpClient.authenticator(proxyAuthenticator);
                            httpClient.proxyAuthenticator(proxyAuthenticator);
                            httpClient.proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxy.getHost(), proxy.getPort())));
                            break;

                    }
                }

                httpClient.proxySelector(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {

                        // Host
                        final String host = uri.getHost();

                        String noProxyHost = proxy.getUrlSkip().stream().collect(Collectors.joining(","));
                        if (StringUtils.contains(noProxyHost, host)) {
                            return Arrays.asList(Proxy.NO_PROXY);
                        } else {
                            // Add Proxy
                            if (proxy.getType().equals(ProxyType.HTTP)) {
                                return Arrays.asList(new Proxy(Proxy.Type.HTTP,
                                    new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                            } else {
                                return Arrays.asList(new Proxy(Proxy.Type.SOCKS,
                                    new InetSocketAddress(proxy.getHost(), proxy.getPort())));
                            }

                        }
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                        throw new UnsupportedOperationException("Proxy Not supported yet.");
                    }
                });
            }
        }

        return httpClient.build();
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
        StringBuilder sb = new StringBuilder(msg);
        if (config.isMasking()) {
            List<String> maskPatterns = words.stream().map(s -> "\"([" + s + "\"]+)\"\\s*:\\s*\"([^\"]+)\",?").collect(Collectors.toList());
            Pattern multilinePattern = Pattern.compile(String.join("|", maskPatterns), Pattern.MULTILINE);
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
        return sb.toString();
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
        if (config.getExcludeHeaders().isEmpty()) {
            logging.redactHeader("Authorization");
            logging.redactHeader("Cookie");
        } else config.getExcludeHeaders().forEach(logging::redactHeader);

        return logging;
    }

}
