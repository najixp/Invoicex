package com.bytecodr.invoicing;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class App extends Application {

    private static final String BASE_URL = "http://soldv.com/gold/index.php/";
    public static final String SERVER_KEY_HASH = "d4c8255fd7e91f8e3a9c3af31ff8274a";

    final static int dbSchemaVersion = 1;

    private static Retrofit retrofit = null;
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(getApplicationContext());

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .schemaVersion(dbSchemaVersion)
                .build();

        Realm.setDefaultConfiguration(realmConfiguration);
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient
                .Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }

    private static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .client(buildClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(BASE_URL)
                    .build();
        }
        return retrofit;
    }

    public static Apis getApis() {
        return getClient().create(Apis.class);
    }
}
