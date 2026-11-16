package app.lasttime.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.lasttime.R
import app.lasttime.data.AppDatabase
import app.lasttime.data.Palette
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "lasttime.db")
            .addCallback(object : RoomDatabase.Callback() {
                // Seeds the default categories once, when the database file is first created.
                override fun onCreate(db: SupportSQLiteDatabase) = seedCategories(db, context)
            })
            .build()

    @Provides fun eventDao(db: AppDatabase) = db.eventDao()
    @Provides fun entryDao(db: AppDatabase) = db.entryDao()
    @Provides fun categoryDao(db: AppDatabase) = db.categoryDao()
}

/** The default categories, written once when the database file is first created. */
internal fun seedCategories(db: SupportSQLiteDatabase, context: Context) {
    val names = listOf(
        R.string.category_home, R.string.category_car, R.string.category_family,
        R.string.category_pets, R.string.category_me,
    )
    names.forEachIndexed { i, res ->
        db.execSQL(
            "INSERT INTO category (name, color, sort_order) VALUES (?, ?, ?)",
            arrayOf<Any?>(context.getString(res), Palette.colors[i % Palette.colors.size], i),
        )
    }
}
