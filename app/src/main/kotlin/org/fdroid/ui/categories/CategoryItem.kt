package org.fdroid.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BrowserUpdated
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CrueltyFree
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FiberSmartRecord
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalPlay
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PermPhoneMsg
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoChat
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryItem(val id: String, val name: String, val description: String? = null) {
  val imageVector: ImageVector
    get() =
      when (id) {
        "AI Chat" -> Icons.Default.VoiceChat
        "App Manager" -> Icons.Default.Apps
        "App Store & Updater" -> Icons.Default.Storefront
        "Action Game" -> Icons.Default.SportsMartialArts
        "Battery" -> Icons.Default.BatteryChargingFull
        "Board Game" -> Icons.Default.DeveloperBoard
        "Bookmark" -> Icons.Default.Bookmarks
        "Browser" -> Icons.Default.OpenInBrowser
        "Calculator" -> Icons.Default.Calculate
        "Calendar & Agenda" -> Icons.Default.CalendarMonth
        "Camera" -> Icons.Default.CameraAlt
        "Card Game" -> Icons.Default.Style
        "Casual Game" -> Icons.Default.Gamepad
        "Clock" -> Icons.Default.AccessTime
        "Cloud Storage & File Sync" -> Icons.Default.Cloud
        "Connectivity" -> Icons.Default.SignalCellularAlt
        "Contact" -> Icons.Default.Contacts
        "Development" -> Icons.Default.DeveloperMode
        "Dice" -> Icons.Default.Casino
        "Diet" -> Icons.Default.Fastfood
        "DNS & Hosts" -> Icons.Default.Dns
        "Download" -> Icons.Default.Download
        "Draw" -> Icons.Default.Draw
        "Ebook Reader" -> AutoMirrored.Default.MenuBook
        "Educational Game" -> Icons.Default.School
        "Email" -> Icons.Default.AlternateEmail
        "Emulator" -> Icons.Default.VideogameAsset
        "File Encryption & Vault" -> Icons.Default.EnhancedEncryption
        "File Manager" -> Icons.Default.FileCopy
        "File Transfer" -> Icons.Default.UploadFile
        "Finance Manager" -> Icons.Default.MonetizationOn
        "Firewall" -> Icons.Default.AppBlocking
        "Flashlight" -> Icons.Default.FlashlightOn
        "Forum" -> Icons.Default.Image
        "Gallery" -> Icons.Default.PhotoSizeSelectActual
        "Game Helper" -> Icons.Default.Handyman
        "Graphics" -> Icons.Default.Brush
        "Habit Tracker" -> Icons.Default.TrackChanges
        "Health Manager" -> Icons.Default.HealthAndSafety
        "Icon Pack" -> Icons.Default.Collections
        "Internet" -> Icons.Default.Language
        "Inventory" -> Icons.Default.AllInbox
        "Keyboard & IME" -> Icons.Default.Keyboard
        "Launcher" -> Icons.Default.Home
        "Local Media Player" -> Icons.Default.LocalPlay
        "Location Tracker & Sharer" -> Icons.Default.MyLocation
        "Lyrics" -> AutoMirrored.Default.QueueMusic
        "Market & Price" -> Icons.Default.StackedLineChart
        "Meditation" -> Icons.Default.SelfImprovement
        "Messaging" -> AutoMirrored.Default.Message
        "Money" -> Icons.Default.Money
        "Multimedia" -> Icons.Default.MusicVideo
        "Music Practice Tool" -> Icons.Default.MusicNote
        "Navigation" -> Icons.Default.Navigation
        "Network Analyzer" -> Icons.Default.NetworkCheck
        "News" -> Icons.Default.Newspaper
        "Note" -> Icons.Default.NoteAlt
        "Online Media Player" -> Icons.Default.Airplay
        "Party Game" -> Icons.Default.Celebration
        "Pass Wallet" -> Icons.Default.AccountBalanceWallet
        "Password & 2FA" -> Icons.Default.Password
        "Phone & SMS" -> Icons.Default.PermPhoneMsg
        "Platformer Game" -> Icons.Default.CrueltyFree
        "Podcast" -> Icons.Default.Podcasts
        "Public Transport" -> Icons.Default.DirectionsBus
        "Puzzle Game" -> Icons.Default.Extension
        "Radio" -> Icons.Default.Radio
        "Reading" -> AutoMirrored.Default.MenuBook
        "Recipe Manager" -> Icons.Default.RestaurantMenu
        "Recorder" -> Icons.Default.FiberSmartRecord
        "Religion" -> Icons.Default.Church
        "Role-Playing Game" -> Icons.Default.Diversity3
        "Remote Access" -> Icons.Default.BrowserUpdated
        "Remote Controller" -> Icons.Default.SettingsRemote
        "Schedule" -> Icons.Default.CalendarMonth
        "Science & Education" -> Icons.Default.Science
        "Security" -> Icons.Default.Security
        "Shooter Game" -> Icons.Default.CenterFocusWeak
        "Strategy Game" -> Icons.Default.Castle
        "Shopping List" -> Icons.Default.ShoppingCart
        "Social Network" -> Icons.Default.Groups
        "Sport Game" -> Icons.Default.SportsSoccer
        "Sports & Health" -> Icons.Default.HealthAndSafety
        "System" -> Icons.Default.Settings
        "Task" -> Icons.Default.TaskAlt
        "Text Editor" -> Icons.Default.EditNote
        "Text to Speech" -> Icons.Default.RecordVoiceOver
        "Theming" -> Icons.Default.Style
        "Time" -> Icons.Default.AccessTime
        "Time Tracker" -> Icons.Default.Timelapse
        "Timer" -> Icons.Default.Timer
        "Translation & Dictionary" -> Icons.Default.Translate
        "Visual Novel" -> Icons.Default.ModeComment
        "Voice & Video Chat" -> Icons.Default.VideoChat
        "Unit Convertor" -> Icons.Default.CurrencyExchange
        "VPN & Proxy" -> Icons.Default.VpnLock
        "Wallet" -> Icons.Default.Wallet
        "Wallpaper" -> Icons.Default.Wallpaper
        "Weather" -> Icons.Default.WbSunny
        "Word Game" -> Icons.Default.Sos
        "Workout" -> Icons.Default.FitnessCenter
        "Writing" -> Icons.Default.EditNote
        else -> Icons.Default.Category
      }

  val group: CategoryGroup
    get() =
      when (id) {
        "AI Chat" -> CategoryGroups.tools
        "App Manager" -> CategoryGroups.device
        "App Store & Updater" -> CategoryGroups.device
        "Action Game" -> CategoryGroups.games
        "Battery" -> CategoryGroups.device
        "Board Game" -> CategoryGroups.games
        "Bookmark" -> CategoryGroups.storage
        "Browser" -> CategoryGroups.network
        "Calculator" -> CategoryGroups.tools
        "Calendar & Agenda" -> CategoryGroups.productivity
        "Camera" -> CategoryGroups.device
        "Card Game" -> CategoryGroups.games
        "Casual Game" -> CategoryGroups.games
        "Clock" -> CategoryGroups.productivity
        "Cloud Storage & File Sync" -> CategoryGroups.storage
        "Connectivity" -> CategoryGroups.network
        "Contact" -> CategoryGroups.communication
        "Development" -> CategoryGroups.interests
        "Dice" -> CategoryGroups.games
        "Diet" -> CategoryGroups.interests
        "DNS & Hosts" -> CategoryGroups.network
        "Download" -> CategoryGroups.network
        "Draw" -> CategoryGroups.interests
        "Ebook Reader" -> CategoryGroups.media
        "Educational Game" -> CategoryGroups.games
        "Email" -> CategoryGroups.communication
        "Emulator" -> CategoryGroups.games
        "File Encryption & Vault" -> CategoryGroups.storage
        "File Manager" -> CategoryGroups.storage
        "File Transfer" -> CategoryGroups.storage
        "Finance Manager" -> CategoryGroups.wallets
        "Firewall" -> CategoryGroups.network
        "Flashlight" -> CategoryGroups.tools
        "Forum" -> CategoryGroups.communication
        "Gallery" -> CategoryGroups.storage
        "Game Helper" -> CategoryGroups.games
        "Graphics" -> CategoryGroups.interests
        "Habit Tracker" -> CategoryGroups.productivity
        "Health Manager" -> CategoryGroups.productivity
        "Icon Pack" -> CategoryGroups.device
        "Internet" -> CategoryGroups.network
        "Inventory" -> CategoryGroups.tools
        "Keyboard & IME" -> CategoryGroups.device
        "Launcher" -> CategoryGroups.device
        "Local Media Player" -> CategoryGroups.media
        "Location Tracker & Sharer" -> CategoryGroups.tools
        "Lyrics" -> CategoryGroups.interests
        "Market & Price" -> CategoryGroups.interests
        "Meditation" -> CategoryGroups.interests
        "Messaging" -> CategoryGroups.communication
        "Money" -> CategoryGroups.wallets
        "Multimedia" -> CategoryGroups.media
        "Music Practice Tool" -> CategoryGroups.interests
        "Navigation" -> CategoryGroups.tools
        "Network Analyzer" -> CategoryGroups.network
        "News" -> CategoryGroups.interests
        "Note" -> CategoryGroups.storage
        "Online Media Player" -> CategoryGroups.media
        "Party Game" -> CategoryGroups.games
        "Pass Wallet" -> CategoryGroups.wallets
        "Password & 2FA" -> CategoryGroups.device
        "Phone & SMS" -> CategoryGroups.communication
        "Platformer Game" -> CategoryGroups.games
        "Podcast" -> CategoryGroups.media
        "Public Transport" -> CategoryGroups.tools
        "Puzzle Game" -> CategoryGroups.games
        "Radio" -> CategoryGroups.media
        "Reading" -> CategoryGroups.media
        "Recipe Manager" -> CategoryGroups.interests
        "Recorder" -> CategoryGroups.tools
        "Religion" -> CategoryGroups.interests
        "Role-Playing Game" -> CategoryGroups.games
        "Remote Access" -> CategoryGroups.network
        "Remote Controller" -> CategoryGroups.tools
        "Schedule" -> CategoryGroups.productivity
        "Science & Education" -> CategoryGroups.interests
        "Security" -> CategoryGroups.device
        "Shooter Game" -> CategoryGroups.games
        "Strategy Game" -> CategoryGroups.games
        "Shopping List" -> CategoryGroups.tools
        "Social Network" -> CategoryGroups.communication
        "Sport Game" -> CategoryGroups.games
        "Sports & Health" -> CategoryGroups.interests
        "System" -> CategoryGroups.device
        "Task" -> CategoryGroups.productivity
        "Text Editor" -> CategoryGroups.productivity
        "Text to Speech" -> CategoryGroups.device
        "Theming" -> CategoryGroups.device
        "Time" -> CategoryGroups.productivity
        "Time Tracker" -> CategoryGroups.productivity
        "Timer" -> CategoryGroups.productivity
        "Translation & Dictionary" -> CategoryGroups.tools
        "Visual Novel" -> CategoryGroups.games
        "Voice & Video Chat" -> CategoryGroups.communication
        "Unit Convertor" -> CategoryGroups.tools
        "VPN & Proxy" -> CategoryGroups.network
        "Wallet" -> CategoryGroups.wallets
        "Wallpaper" -> CategoryGroups.device
        "Weather" -> CategoryGroups.tools
        "Word Game" -> CategoryGroups.games
        "Workout" -> CategoryGroups.interests
        "Writing" -> CategoryGroups.productivity
        else -> CategoryGroups.misc
      }
}
