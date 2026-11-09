package com.intentlock

/**
 * The social apps that IntentLock guards, and how long the forced pause lasts
 * before each one becomes usable. Durations are defaults; the user can change
 * them on the setup screen (stored in [Prefs]).
 */
data class TargetApp(
    val key: String,
    val label: String,
    val packages: Set<String>,
    val defaultMinutes: Int
)

object TargetApps {

    val ALL: List<TargetApp> = listOf(
        TargetApp(
            key = "tiktok",
            label = "TikTok",
            packages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            defaultMinutes = 10
        ),
        TargetApp(
            key = "instagram",
            label = "Instagram",
            packages = setOf("com.instagram.android"),
            defaultMinutes = 10
        ),
        TargetApp(
            key = "x",
            label = "X",
            packages = setOf("com.twitter.android", "com.x.android"),
            defaultMinutes = 30
        )
    )

    /** Packages we should ignore entirely (never treated as "leaving" a guarded app). */
    val NEUTRAL: Set<String> = setOf(
        "com.intentlock",
        "com.android.systemui"
    )

    fun match(pkg: String): TargetApp? = ALL.firstOrNull { pkg in it.packages }
}
