package com.example.features.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    return resolveInfos.map { resolveInfo ->
        AppInfo(
            packageName = resolveInfo.activityInfo.packageName,
            appName = resolveInfo.loadLabel(pm).toString(),
            icon = resolveInfo.loadIcon(pm)
        )
    }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
}

fun getDefaultClassification(pkgName: String, appName: String): String {
    val lowerPkg = pkgName.lowercase()
    val lowerApp = appName.lowercase()

    // Distracting package keywords or names
    val distractingPkgs = listOf(
        "instagram", "facebook", "katana", "tiktok", "musically", "snapchat", "youtube",
        "spotify", "netflix", "amazon.avod", "primevideo", "twitter", "reddit", "discord",
        "twitch", "pinterest", "tinder", "hulu", "disneyplus", "games", "gaming", "play"
    )
    val distractingApps = listOf(
        "instagram", "facebook", "tiktok", "snapchat", "youtube", "spotify", "netflix",
        "prime video", "discord", "x", "reddit", "games", "game", "pubg", "freefire", "candy crush",
        "twitch", "pinterest"
    )

    // Productive package keywords or names
    val productivePkgs = listOf(
        "docs", "sheets", "slides", "drive", "notion", "chatgpt", "openai", "classroom",
        "physicswallah", "khanacademy", "coursera", "udemy", "zoom", "meetings", "github",
        "gmail", "calendar", "keep", "todo", "todoist", "trello", "slack", "vscode", "clion",
        "intellij", "android.studio", "duolingo"
    )
    val productiveApps = listOf(
        "google", "google drive", "google docs", "google sheets", "google slides", "gmail",
        "calendar", "notion", "chatgpt", "physics wallah", "khan academy", "coursera",
        "udemy", "zoom", "google meet", "vs code", "github", "slack", "keep", "todoist", "trello",
        "duolingo"
    )

    // Check Distracting
    if (distractingPkgs.any { lowerPkg.contains(it) } || distractingApps.any { lowerApp.contains(it) }) {
        return "DISTRACTING"
    }

    // Check Productive
    if (productivePkgs.any { lowerPkg.contains(it) } || productiveApps.any { lowerApp.contains(it) }) {
        return "PRODUCTIVE"
    }

    // Default recommendation fallback
    return "PRODUCTIVE"
}
