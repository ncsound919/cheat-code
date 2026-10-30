package com.propslab.hub

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlin.math.max

/**
 * What this phone can actually run.
 *
 * The numbers are read from the platform rather than guessed: total RAM from
 * [ActivityManager], free space from the app's own filesystem, cores from the runtime and
 * the ABI from [Build]. The recommendation is deliberately conservative — Android keeps a
 * large share of RAM for the system, and a model that only just fits is a model that gets
 * killed in the background.
 */
data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val sdk: Int,
    val abi: String,
    val cores: Int,
    val totalRamGb: Double,
    val freeStorageGb: Double,
    /** The largest model size worth offering, in bytes, with room left for the OS. */
    val modelBudgetBytes: Long,
    /** A short sentence a person can act on. */
    val verdict: String,
) {
    /** The parameter class this phone should be shopping in: "0.5–1.5B", "3B", "7B". */
    val recommendedBand: String
        get() = when {
            totalRamGb >= 14.0 -> "3B–8B"
            totalRamGb >= 9.0 -> "1.5B–4B"
            totalRamGb >= 7.0 -> "1B–2B"
            else -> "0.5B–1B"
        }

    /** A conservative quantization hint, so the file picker opens on a sane row. */
    val recommendedQuant: String get() = "Q4_K_M"

    val summary: String
        get() = "$manufacturer $model · $cores cores · " +
            "%.1f GB RAM · %.1f GB free".format(totalRamGb, freeStorageGb)

    val ramLine: String
        get() = when {
            totalRamGb >= 14.0 -> "Enough memory for a 7–8B model at four bits per weight."
            totalRamGb >= 9.0 -> "Comfortable for a 3–4B model; 1.5B is the safe pick for long chats."
            totalRamGb >= 7.0 -> "Best with 1–2B weights. Anything larger will be reclaimed by Android."
            else -> "Stick to 0.5–1B weights. The template explanations remain available regardless."
        }

    companion object {

        /** Android needs headroom; a model may use at most this share of total RAM. */
        const val RAM_SHARE = 0.55

        /** Free storage that must remain after a download, so updates still fit. */
        const val STORAGE_RESERVE_BYTES = 600L * 1024L * 1024L

        fun read(context: Context): DeviceProfile {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalRamGb = info.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val stat = runCatching { StatFs(context.filesDir.absolutePath) }.getOrNull()
            val freeBytes = stat?.availableBytes ?: 0L
            val freeGb = freeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val ramBudget = (info.totalMem * RAM_SHARE).toLong()
            val storageBudget = max(0L, freeBytes - STORAGE_RESERVE_BYTES)
            val budget = minOf(ramBudget, storageBudget)
            val profile = DeviceProfile(
                manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                model = Build.MODEL,
                sdk = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                cores = Runtime.getRuntime().availableProcessors(),
                totalRamGb = totalRamGb,
                freeStorageGb = freeGb,
                modelBudgetBytes = budget,
                verdict = "",
            )
            return profile.copy(verdict = verdictFor(profile, ramBudget, storageBudget, freeBytes))
        }

        private fun verdictFor(profile: DeviceProfile, ramBudget: Long, storageBudget: Long, freeBytes: Long): String =
            when {
                freeBytes < STORAGE_RESERVE_BYTES ->
                    "This phone is nearly full. Free up about 1 GB before downloading weights."
                storageBudget < ramBudget ->
                    "Storage is the limit: keep the download under %.1f GB.".format(storageBudget / 1e9)
                profile.totalRamGb >= 14.0 -> "A 7–8B model fits comfortably. 3B is faster for long answers."
                profile.totalRamGb >= 9.0 -> "A 3–4B model fits. Pick 1.5B if the phone runs hot."
                profile.totalRamGb >= 7.0 -> "A 1–2B model is the sweet spot here."
                else -> "Use a 0.5–1B model, or stay with the built-in explanations."
            }
    }
}
