package io.github.avew;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class OkHttpTest {

    private Retrofit retrofit;
    private final List<String> maskingLog = Arrays.asList("user", "password", "name");

    private ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter();
        mapper.coercionConfigFor(LogicalType.POJO)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Before
    public void init() {
        CustomHttpConfig customHttpConfig = CustomHttpConfig.builder()
                .url("https://reqres.in")
                .masking(true)
                .excludeHeaders(List.of("X-Api-Key"))
                .build();

        retrofit = new VewHttp(customHttpConfig, maskingLog, true)
                .builder()
                .addConverterFactory(JacksonConverterFactory.create(mapper()))
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
