package app.lasttime.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import app.lasttime.R

/** The icons an event can use. Stored by key so the set can grow without a migration. */
enum class EventIcon(val key: String, val vector: ImageVector, @StringRes val label: Int) {
    Home("home", Icons.Filled.Home, R.string.icon_home),
    Star("star", Icons.Filled.Star, R.string.icon_star),
    Heart("heart", Icons.Filled.Favorite, R.string.icon_heart),
    Build("build", Icons.Filled.Build, R.string.icon_build),
    Cart("cart", Icons.Filled.ShoppingCart, R.string.icon_cart),
    Phone("phone", Icons.Filled.Phone, R.string.icon_phone),
    Mail("mail", Icons.Filled.Email, R.string.icon_mail),
    Place("place", Icons.Filled.Place, R.string.icon_place),
    Person("person", Icons.Filled.Person, R.string.icon_person),
    Face("face", Icons.Filled.Face, R.string.icon_face),
    Calendar("calendar", Icons.Filled.DateRange, R.string.icon_calendar),
    Bell("bell", Icons.Filled.Notifications, R.string.icon_bell),
    Check("check", Icons.Filled.Check, R.string.icon_check),
    Refresh("refresh", Icons.Filled.Refresh, R.string.icon_refresh),
    Thumb("thumb", Icons.Filled.ThumbUp, R.string.icon_thumb),
    Info("info", Icons.Filled.Info, R.string.icon_info);

    companion object {
        fun of(key: String): EventIcon = entries.firstOrNull { it.key == key } ?: Star
    }
}
