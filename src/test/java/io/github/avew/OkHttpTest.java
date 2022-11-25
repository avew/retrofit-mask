package io.github.avew;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class OkHttpTest {

    private Retrofit retrofit;
    private final List<String> maskingLog = Arrays.asList("https",
            "password",
            "docpass",
            "docpas",
            "pass",
            "data",
            "content",
            "client_secret",
            "user",
            "token",
            "sn",
            "image",
            "noidentitas",
            "namedipungut",
            "jwToken",
            "refToken");

    @Before
    public void init() {
        CustomHttpConfig customHttpConfig = CustomHttpConfig.builder()
                .url("https://reqres.in")
                .agent("OkHttp/4.1.0")
                .build();
        log.debug("CONFIG {}", customHttpConfig.getCustomTimeout());
        retrofit = new OkHttpCustomConfiguration(customHttpConfig, maskingLog, true)
                .builder()
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Test
    public void testGet() {
        Response<MultipleResource> single = retrofit.create(API.class)
                .doGetListResources()
                .toBlocking()
                .single();
        Assert.assertEquals(200, single.code());

    }
}
