package io.github.avew;

import retrofit2.Response;
import retrofit2.http.GET;
import rx.Observable;

public interface API {

    @GET("/api/unknown")
    Observable<Response<MultipleResource>> doGetListResources();
}
