package org.fdroid.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalPlay
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PermPhoneMsg
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoChat
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryItem(val id: String, val name: String) {
    val imageVector: ImageVector
        get() = when (id) {
            "App Store & Updater" -> Icons.Default.Storefront
            "Bookmark" -> Icons.Default.Bookmarks
            "Browser" -> Icons.Default.OpenInBrowser
            "Calculator" -> Icons.Default.Calculate
            "Calendar & Agenda" -> Icons.Default.CalendarMonth
            "Cloud Storage & File Sync" -> Icons.Default.Cloud
            "Connectivity" -> Icons.Default.SignalCellularAlt
            "Development" -> Icons.Default.DeveloperMode
            "DNS & Hosts" -> Icons.Default.Dns
            "Draw" -> Icons.Default.Draw
            "Ebook Reader" -> AutoMirrored.Default.MenuBook
            "Email" -> Icons.Default.AlternateEmail
            "File Encryption & Vault" -> Icons.Default.EnhancedEncryption
            "File Transfer" -> Icons.Default.UploadFile
            "Finance Manager" -> Icons.Default.MonetizationOn
            "Forum" -> Icons.Default.Image
            "Gallery" -> Icons.Default.PhotoSizeSelectActual
            "Games" -> Icons.Default.Games
            "Graphics" -> Icons.Default.Brush
            "Habit Tracker" -> Icons.Default.TrackChanges
            "Icon Pack" -> Icons.Default.Collections
            "Internet" -> Icons.Default.Language
            "Keyboard & IME" -> Icons.Default.Keyboard
            "Launcher" -> Icons.Default.Home
            "Local Media Player" -> Icons.Default.LocalPlay
            "Messaging" -> AutoMirrored.Default.Message
            "Money" -> Icons.Default.Money
            "Multimedia" -> Icons.Default.MusicVideo
            "Music Practice Tool" -> Icons.Default.MusicNote
            "Navigation" -> Icons.Default.Navigation
            "News" -> Icons.Default.Newspaper
            "Note" -> Icons.Default.NoteAlt
            "Online Media Player" -> Icons.Default.Airplay
            "Pass Wallet" -> Icons.Default.AccountBalanceWallet
            "Password & 2FA" -> Icons.Default.Password
            "Phone & SMS" -> Icons.Default.PermPhoneMsg
            "Podcast" -> Icons.Default.Podcasts
            "Public Transport" -> Icons.Default.DirectionsBus
            "Reading" -> AutoMirrored.Default.MenuBook
            "Recipe Manager" -> Icons.Default.RestaurantMenu
            "Science & Education" -> Icons.Default.Science
            "Security" -> Icons.Default.Security
            "Shopping List" -> Icons.Default.ShoppingCart
            "Social Network" -> Icons.Default.Groups
            "Sports & Health" -> Icons.Default.HealthAndSafety
            "System" -> Icons.Default.Settings
            "Task" -> Icons.Default.TaskAlt
            "Text Editor" -> Icons.Default.EditNote
            "Theming" -> Icons.Default.Style
            "Time" -> Icons.Default.AccessTime
            "Translation & Dictionary" -> Icons.Default.Translate
            "Voice & Video Chat" -> Icons.Default.VideoChat
            "Unit Convertor" -> Icons.Default.CurrencyExchange
            "VPN & Proxy" -> Icons.Default.VpnLock
            "Wallet" -> Icons.Default.Wallet
            "Wallpaper" -> Icons.Default.Wallpaper
            "Weather" -> Icons.Default.WbSunny
            "Workout" -> Icons.Default.FitnessCenter
            "Writing" -> Icons.Default.EditNote
            else -> Icons.Default.Category
        }
}
