# Custom Retrofit With Masking Log
[https://jitpack.io/#avew/retrofit-mask](https://jitpack.io/#avew/retrofit-mask)

[![](https://jitpack.io/v/avew/retrofit-mask.svg)](https://jitpack.io/#avew/retrofit-mask)

[![CircleCI](https://circleci.com/gh/avew/retrofit-mask.svg?style=shield)](https://circleci.com/gh/avew/retrofit-mask)

# How To Use

```
 CustomHttpConfig customHttpConfig = CustomHttpConfig.builder()
                .url("https://reqres.in")
                .agent("OkHttp/4.1.0")
                .build();
 Retrofit retrofit = new OkHttpCustomConfiguration(customHttpConfig, Arrays.asList("https","password"), true).builder()
                .addConverterFactory(GsonConverterFactory.create())
                .build();
 Response<MultipleResource> single = retrofit.create(API.class)
                .doGetListResources()
                .toBlocking()
                .single();
 Assert.assertEquals(200, single.code());
```
