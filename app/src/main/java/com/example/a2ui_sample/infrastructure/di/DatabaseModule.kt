package com.example.a2ui_sample.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.example.a2ui_sample.infrastructure.persistence.AppDatabase
import com.example.a2ui_sample.infrastructure.persistence.dao.MenuDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "restaurant_db"
        ).build()
    }

    @Provides
    fun provideMenuDao(database: AppDatabase): MenuDao {
        return database.menuDao()
    }
}
