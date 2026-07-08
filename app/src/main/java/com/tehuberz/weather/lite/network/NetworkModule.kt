package com.tehuberz.weather.lite.network

import com.tehuberz.weather.lite.BuildConfig
import com.tehuberz.weather.lite.api.ApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient  {
                val interceptorLevel = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
                return OkHttpClient.Builder()
                        .addInterceptor(HttpLoggingInterceptor().apply {
                        level = interceptorLevel
                        }).build()
        }

        @Provides
        @Singleton
        fun provideApiService(retrofit: Retrofit) : ApiService {
                return retrofit.create(ApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
                return Retrofit.Builder()
                        .baseUrl(ApiServiceHost.getActive().baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                        .build()
        }

        @Provides
        @Singleton
        @ApiKey // Custom qualifier if needed, simple string injection for now
        fun provideApiKey(): String {
                // Basic check - replace with proper validation/handling
                val apiKey = if (ApiServiceHost.ACCUWEATHER.isActive) BuildConfig.ACCUWEATHER_API_KEY
                        else BuildConfig.OPEN_WEATHER_API_KEY
                if (apiKey.isBlank()) {
                        throw IllegalArgumentException("API Key is not set in BuildConfig. Please add it to your local.properties or gradle file.")
                }
                println("Using API Key: ...${apiKey.takeLast(4)}") // Avoid logging full key
                return apiKey
        }

        @javax.inject.Qualifier
        @Retention(AnnotationRetention.BINARY)
        annotation class ApiKey

}