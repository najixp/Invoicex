package com.bytecodr.invoicing;

import com.google.gson.JsonObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface Apis {


    @FormUrlEncoded
    @POST("english/change-password.php?")
    Call<JsonObject> userChangepassword(@Query("hash") String hash, @Query("sid") String search,
                                        @Field("cpassword") String cpassword,
                                        @Field("oldpassword") String oldpassword);

    @FormUrlEncoded
    @POST("description_api/get_description")
    Call<JsonObject> getDescriptions(@Field("hash") String hash,
                                     @Field("user_id") String userId);

    @FormUrlEncoded
    @POST("description_api/add_description")
    Call<JsonObject> addDescription(@Field("hash") String hash,
                                    @Field("user_id") String userId,
                                    @Field("title") String title,
                                    @Field("desc") String description);

    @FormUrlEncoded
    @POST("description_api/add_description")
    Call<JsonObject> updateDescription(@Field("hash") String hash,
                                       @Field("user_id") String userId,
                                       @Field("desc_id") String descriptionId,
                                       @Field("title") String title,
                                       @Field("desc") String description);

    @FormUrlEncoded
    @POST("description_api/delete_description")
    Call<JsonObject> deleteDescription(@Field("hash") String hash,
                                       @Field("user_id") String userId,
                                       @Field("desc_id") String descriptionId);

    @FormUrlEncoded
    @POST("description_api/reports")
    Call<JsonObject> getReport(@Field("hash") String hash,
                               @Field("user_id") int userId);

}
