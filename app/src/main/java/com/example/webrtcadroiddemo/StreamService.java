package com.example.webrtcadroiddemo;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface StreamService {
    @GET("/streams.json")
    Call<List<Caller>> getStreams();
}


