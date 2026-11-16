package app.lasttime.di

import android.content.Context
import app.lasttime.data.EventRepository
import app.lasttime.data.SettingsRepository
import app.lasttime.widget.WidgetRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Gives framework-created classes (workers, widgets) access to the Hilt graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun repository(): EventRepository
    fun settings(): SettingsRepository
    fun widgets(): WidgetRefresher
}

fun Context.appEntryPoint(): AppEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
