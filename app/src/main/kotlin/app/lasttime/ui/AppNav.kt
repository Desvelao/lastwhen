package app.lasttime.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.lasttime.ui.categories.CategoriesScreen
import app.lasttime.ui.detail.DetailScreen
import app.lasttime.ui.form.FormScreen
import app.lasttime.ui.home.HomeScreen
import app.lasttime.ui.settings.ArchivedScreen
import app.lasttime.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{id}"
    const val FORM = "form?id={id}&name={name}&icon={icon}"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val ARCHIVED = "archived"
    fun detail(id: Long) = "detail/$id"
    fun form(id: Long?, name: String?, icon: String?) =
        "form?id=${id ?: -1}&name=${android.net.Uri.encode(name.orEmpty())}&icon=${android.net.Uri.encode(icon.orEmpty())}"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenEvent = { nav.navigate(Routes.detail(it)) },
                onAdd = { name, icon -> nav.navigate(Routes.form(null, name, icon)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            DetailScreen(onBack = { nav.popBackStack() }, onEdit = { nav.navigate(Routes.form(it, null, null)) })
        }
        composable(
            Routes.FORM,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("icon") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            FormScreen(
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
                // Archived or deleted: the detail screen below no longer applies, so return to the list.
                onGone = { nav.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ARCHIVED) {
            ArchivedScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate(Routes.detail(it)) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onCategories = { nav.navigate(Routes.CATEGORIES) },
                onArchived = { nav.navigate(Routes.ARCHIVED) },
            )
        }
    }
}
