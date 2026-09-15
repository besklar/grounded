package com.besklar.grounded.di

import android.content.Context
import androidx.room.Room
import com.besklar.grounded.data.local.EarthquakeDao
import com.besklar.grounded.data.local.GroundedDatabase
import com.besklar.grounded.data.local.RoomEarthquakeStore
import com.besklar.grounded.data.remote.EarthquakeRemoteSource
import com.besklar.grounded.data.remote.UsgsApi
import com.besklar.grounded.data.remote.UsgsRemoteDataSource
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.EarthquakeStore
import com.besklar.grounded.data.repository.OfflineFirstEarthquakeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppBindings {
    @Binds
    @Singleton
    abstract fun bindRemoteSource(source: UsgsRemoteDataSource): EarthquakeRemoteSource

    @Binds
    @Singleton
    abstract fun bindStore(store: RoomEarthquakeStore): EarthquakeStore

    @Binds
    @Singleton
    abstract fun bindRepository(repository: OfflineFirstEarthquakeRepository): EarthquakeRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GroundedDatabase = Room.databaseBuilder(context, GroundedDatabase::class.java, "grounded.db").build()

    @Provides
    fun provideEarthquakeDao(database: GroundedDatabase): EarthquakeDao = database.earthquakeDao()

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit
        .Builder()
        .baseUrl("https://earthquake.usgs.gov/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    fun provideUsgsApi(retrofit: Retrofit): UsgsApi = retrofit.create(UsgsApi::class.java)
}
