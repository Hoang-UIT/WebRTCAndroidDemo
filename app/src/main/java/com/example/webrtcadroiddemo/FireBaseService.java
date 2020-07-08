package com.example.webrtcadroiddemo;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface FireBaseService {
    @Headers({"Content-Type: application/json", "Authorization: key=AAAALtjEy3Q:APA91bF1GyfRaJzfDddjDOABAQZ26YGpR0Hu8APem_f6zAofigAGC87uUh_mv958IhAcoqD8cnv6yW55975IoE2HISlIlU6jP_gg8cRMDmQvYDZ5D4KRvPZI9Jb61DDwpEEuLrSFZBlv"})
    @POST("/fcm/send")
    Call<ResponseBody> PushNotification(@Body RequestBody body);
}