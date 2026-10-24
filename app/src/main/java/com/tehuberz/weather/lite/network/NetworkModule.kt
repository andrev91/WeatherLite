package com.tehuberz.weather.lite.network

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.tehuberz.weather.lite.BuildConfig
import com.tehuberz.weather.lite.api.ApiService
import com.tehuberz.weather.lite.util.ApiServiceHost
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = NetworkModule::class.java.simpleName

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val interceptorLevel =
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        return OkHttpClient
            .Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = interceptorLevel
                },
            ).build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(ApiServiceHost.getActive().baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @ApiKey
    fun provideApiKey(): String {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank()) {
            Log.w(
                TAG,
                "API Key (${ApiServiceHost.getActive().apiKeyConfigName}) is not set in BuildConfig. " +
                    "Please add it to your local.properties file.",
            )
        } else if (BuildConfig.DEBUG) {
            Log.d(TAG, "Using API Key: ...${apiKey.takeLast(4)}")
        }
        return apiKey
    }

    fun getActiveApiKey(): String =
        when (ApiServiceHost.getActive()) {
            ApiServiceHost.ACCUWEATHER -> BuildConfig.ACCUWEATHER_API_KEY
            ApiServiceHost.OPEN_WEATHER_MAP -> BuildConfig.OPEN_WEATHER_API_KEY
        }

    fun isApiKeyConfigured(): Boolean = getActiveApiKey().isNotBlank()

    @javax.inject.Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ApiKey
}
