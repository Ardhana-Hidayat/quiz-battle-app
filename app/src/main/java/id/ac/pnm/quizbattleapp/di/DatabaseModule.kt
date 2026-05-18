package id.ac.pnm.quizbattleapp.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.ac.pnm.quizbattleapp.data.local.AppDatabase
import id.ac.pnm.quizbattleapp.data.local.QuestionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "quiz_battle_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideQuestionDao(appDatabase: AppDatabase): QuestionDao {
        return appDatabase.questionDao()
    }
}
