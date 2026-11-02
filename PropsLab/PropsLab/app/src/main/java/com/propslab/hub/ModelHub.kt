package com.propslab.hub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** A model family worth surfacing before the user types anything. */
data class CuratedModel(
    val repo: String,
    val label: String,
    val params: String,
    val blurb: String,
)

/** One downloadable weight file inside a Hugging Face repository. */
data class HubFile(
    val repo: String,
    val name: String,
    val bytes: Long,
    /** SHA-256 Hugging Face reports for the blob, when the file is stored in LFS. */
    val sha256: String = "",
) {
    val url: String get() = "https://huggingface.co/$repo/resolve/main/$name?download=true"
    val isProjector: Boolean get() = name.contains("mmproj", ignoreCase = true)
    val isLora: Boolean get() = name.contains("lora", ignoreCase = true)
    val quant: String get() = QUANT.find(name)?.value?.uppercase(Locale.ROOT) ?: "GGUF"
    val sizeLabel: String get() = if (bytes <= 0) "size unknown" else "%.2f GB".format(bytes / 1e9)

    companion object {
        private val QUANT = Regex("(q\\d+(?:_[a-z0-9]+)*|f16|f32|bf16|iq\\d+(?:_[a-z0-9]+)*)", RegexOption.IGNORE_CASE)
    }
}

/**
 * The model shop, reading Hugging Face's public API.
 *
 * No repository list is hardcoded into the download path: a curated set of *repositories*
 * is offered as a starting point, then the actual files, their sizes and their
 * quantizations are read from the API at the moment of the tap. That is the difference
 * between a link that rots and a shop that stays current — a GGUF file renamed upstream is
 * still discoverable because nothing here claims to know the file name in advance.
 *
 * Nothing is uploaded and no account is required; these are anonymous public reads.
 */
object ModelHub {

    private const val API = "https://huggingface.co/api/models"

    /** A short, opinionated shelf. Repos only; the files come from the API. */
    val curated: List<CuratedModel> = listOf(
        CuratedModel(
            repo = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            label = "Qwen2.5 0.5B Instruct",
            params = "0.5B",
            blurb = "The smallest useful model. Loads in a second, answers fast, fits any phone.",
        ),
        CuratedModel(
            repo = "bartowski/Qwen2.5-1.5B-Instruct-GGUF",
            label = "Qwen2.5 1.5B Instruct",
            params = "1.5B",
            blurb = "The default recommendation: plain-language explanations at a usable speed.",
        ),
        CuratedModel(
            repo = "bartowski/Qwen2.5-3B-Instruct-GGUF",
            label = "Qwen2.5 3B Instruct",
            params = "3B",
            blurb = "Better reasoning for formula talk. Wants a recent phone with 8 GB or more.",
        ),
        CuratedModel(
            repo = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            label = "Llama 3.2 1B Instruct",
            params = "1B",
            blurb = "Short, direct prose. A good second opinion beside Qwen.",
        ),
        CuratedModel(
            repo = "bartowski/Llama-3.2-3B-Instruct-GGUF",
            label = "Llama 3.2 3B Instruct",
            params = "3B",
            blurb = "The strongest small Llama. Needs roughly 2.5 GB of free space.",
        ),
        CuratedModel(
            repo = "microsoft/Phi-3.5-mini-instruct-gguf",
            label = "Phi-3.5 mini Instruct",
            params = "3.8B",
            blurb = "Dense and analytical, with a permissive licence. Slower than a 1.5B.",
        ),
    )

    /** Where weights live. One directory, shared with [com.propslab.mathx.LlamaCppModel]. */
    fun modelsDir(root: File): File = File(root, "models").apply { mkdirs() }

    /**
     * Base weights only. A LoRA adapter is a delta that needs a base model beside it, so
     * offering it as "use this model" would fail at load time; the adapter stays on disk and
     * is attached by name when present.
     */
    fun installed(root: File): List<File> =
        modelsDir(root).listFiles { f ->
            f.isFile && f.extension.equals("gguf", true) && !f.name.contains("lora", true)
        }.orEmpty().sortedByDescending { it.length() }

    /** The name written by the shop. Falls back to the newest GGUF already on disk. */
    fun activeName(root: File): String? {
        val pointer = File(modelsDir(root), ACTIVE_POINTER)
        val named = runCatching { pointer.readText().trim() }.getOrNull()
        val files = installed(root)
        return named?.takeIf { name -> files.any { it.name == name } }?.let { named }
            ?: files.firstOrNull()?.name
    }

    fun setActive(root: File, name: String) {
        File(modelsDir(root), ACTIVE_POINTER).writeText(name)
    }

    fun delete(root: File, name: String): Boolean {
        val target = File(modelsDir(root), name)
        val removed = target.isFile && target.delete()
        if (removed && activeName(root) == name) {
            File(modelsDir(root), ACTIVE_POINTER).delete()
        }
        return removed
    }

    /** Free-form search over public GGUF repositories, most downloaded first. */
    suspend fun search(query: String, limit: Int = 20): List<CuratedModel> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val encoded = java.net.URLEncoder.encode(q, "UTF-8")
        val body = runCatching {
            read("$API?search=$encoded&filter=gguf&sort=downloads&direction=-1&limit=$limit", 12_000)
        }.getOrNull() ?: return@withContext emptyList()
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return@withContext emptyList()
        buildList {
            for (i in 0 until array.length()) {
                val row = array.optJSONObject(i) ?: continue
                val id = row.optString("id")
                if (id.isBlank()) continue
                val downloads = row.optLong("downloads")
                add(
                    CuratedModel(
                        repo = id,
                        label = id.substringAfterLast('/'),
                        params = paramsOf(id),
                        blurb = "%s downloads · %s".format(shortCount(downloads), id.substringBefore('/')),
                    ),
                )
            }
        }
    }

    /** The GGUF files in a repository, smallest first, with sizes when the API reports them. */
    suspend fun files(repo: String): List<HubFile> = withContext(Dispatchers.IO) {
        val body = runCatching { read("$API/$repo?blobs=true", 20_000) }.getOrNull()
            ?: return@withContext emptyList()
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext emptyList()
        val siblings = root.optJSONArray("siblings") ?: JSONArray()
        buildList {
            for (i in 0 until siblings.length()) {
                val row = siblings.optJSONObject(i) ?: continue
                val name = row.optString("rfilename")
                if (!name.endsWith(".gguf", true)) continue
                val hash = row.optJSONObject("lfs")?.optString("sha256").orEmpty()
                add(
                    HubFile(
                        repo = repo,
                        name = name,
                        bytes = row.optLong("size", 0L),
                        sha256 = hash.lowercase(Locale.ROOT),
                    ),
                )
            }
        }.sortedBy { it.bytes }
    }

    /**
     * Downloads into the models directory, resuming a partial file, verifying its SHA-256, and
     * only then renaming it into place.
     *
     * Three things can go wrong with a multi-gigabyte download on a phone: it is interrupted,
     * the connection is metered and dies, or the bytes arrive corrupted. The first two are
     * handled by keeping the `.part` file and asking Hugging Face for the rest with a Range
     * request; the third by comparing the finished file with the hash the API published. A
     * file that fails either check is never renamed, so it can never be loaded.
     */
    suspend fun download(
        file: HubFile,
        root: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = modelsDir(root)
        val target = File(dir, file.name)
        if (target.isFile && target.length() > 0L) return@withContext Result.success(target)
        val part = File(dir, file.name + ".part")
        val total = if (file.bytes > 0L) file.bytes else 0L

        // A `.part` left by an earlier attempt is kept, not thrown away: that is the resume.
        var start = if (part.isFile) part.length() else 0L
        if (total > 0L && start > total) {
            part.delete()
            start = 0L
        }
        if (total > 0L && start == total && start > 0L) {
            return@withContext finish(part, target, file)
        }

        val connection = runCatching { open(file.url, resumeFrom = start) }
            .getOrElse { return@withContext Result.failure(it) }
        try {
            val code = connection.responseCode
            if (code == 416 && start > 0L) {
                // The server thinks the part is already whole; verify rather than assume.
                return@withContext finish(part, target, file)
            }
            if (code !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("Hugging Face answered HTTP $code for ${file.name}"),
                )
            }
            // 206 means the range was honoured and the bytes continue the part file. Anything
            // else restarts from zero, so the two are never appended to each other blindly.
            val appending = code == 206 && start > 0L
            var copied = if (appending) start else 0L
            if (!appending) {
                start = 0L
                part.delete()
            }
            val expected = connection.contentLengthLong.takeIf { it > 0L }?.plus(start) ?: total
            onProgress(copied, expected)
            connection.inputStream.use { input ->
                FileOutputStream(part, appending).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(copied, expected)
                    }
                }
            }
            if (copied <= 0L) return@withContext Result.failure(IllegalStateException("The download was empty"))
            if (expected > 0L && copied != expected) {
                // Not fatal: the part stays and the next tap resumes from here.
                return@withContext Result.failure(
                    IllegalStateException(
                        "The download stopped at $copied of $expected bytes. Tap download again to resume.",
                    ),
                )
            }
            finish(part, target, file)
        } catch (t: Throwable) {
            // Keep the part file only when it holds usable prefix bytes; a zero-length or
            // half-written tail would just confuse the next resume.
            if (!part.isFile || part.length() == 0L) part.delete()
            Result.failure(t)
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    /** Hash, then rename. A mismatch deletes the part so the next attempt starts clean. */
    private fun finish(part: File, target: File, file: HubFile): Result<File> {
        if (!part.isFile || part.length() == 0L) {
            return Result.failure(IllegalStateException("No downloaded bytes were kept"))
        }
        if (file.bytes > 0L && part.length() != file.bytes) {
            return Result.failure(
                IllegalStateException(
                    "The download is ${part.length()} of ${file.bytes} bytes. Tap download again to resume.",
                ),
            )
        }
        if (file.sha256.isNotBlank()) {
            val actual = runCatching { sha256(part) }.getOrElse {
                return Result.failure(IllegalStateException("Could not read the file back to check it"))
            }
            if (!actual.equals(file.sha256, ignoreCase = true)) {
                part.delete()
                return Result.failure(
                    IllegalStateException("The file did not match Hugging Face's checksum, so it was discarded. Try again."),
                )
            }
        }
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) {
            return Result.failure(IllegalStateException("Could not move the download into place"))
        }
        return Result.success(target)
    }

    /** SHA-256 of a file, streamed so a 4 GB model is never held in memory. */
    fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun open(url: String, resumeFrom: Long = 0L): HttpURLConnection {
        require(url.startsWith("https://")) { "refusing a non-HTTPS model download" }
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "CheatCode-Android")
            setRequestProperty("Accept", "application/json, application/octet-stream")
            if (resumeFrom > 0L) setRequestProperty("Range", "bytes=$resumeFrom-")
        }
    }

    private fun read(url: String, max: Int): String {
        val connection = open(url)
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            return text.take(max)
        } finally {
            connection.disconnect()
        }
    }

    private fun paramsOf(id: String): String {
        val match = Regex("(\\d+(?:\\.\\d+)?)\\s*[bB](?![a-zA-Z])").find(id.substringAfterLast('/'))
        return match?.let { "${it.groupValues[1]}B" } ?: "GGUF"
    }

    private fun shortCount(value: Long): String = when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
        value >= 1_000 -> "%.0fk".format(value / 1_000.0)
        else -> "$value"
    }

    /** The Hugging Face page for a repository, for the "open on the web" link. */
    fun pageUrl(repo: String): String = "https://huggingface.co/$repo"

    /**
     * The shop's own answer for this phone: which curated repository to download first.
     *
     * The band comes from [DeviceProfile.recommendedBand], which is measured from RAM,
     * not guessed. Big phones get the 3B Qwen, small ones get the 0.5B, and the middle
     * gets the 1.5B default. The caller still reads the real file list before downloading,
     * so a renamed GGUF upstream cannot break this choice.
     */
    fun recommendedFor(totalRamGb: Double): CuratedModel = when {
        totalRamGb >= 14.0 -> curated.first { it.repo == "bartowski/Qwen2.5-3B-Instruct-GGUF" }
        totalRamGb >= 9.0 -> curated.first { it.repo == "bartowski/Qwen2.5-3B-Instruct-GGUF" }
        totalRamGb >= 7.0 -> curated.first { it.repo == "bartowski/Qwen2.5-1.5B-Instruct-GGUF" }
        else -> curated.first { it.repo == "Qwen/Qwen2.5-0.5B-Instruct-GGUF" }
    }

    /**
     * Picks the file to download automatically: the largest Q4_K_M that fits the budget,
     * falling back to the largest fitting file, then to the smallest file overall.
     * Returns null when the repository publishes no GGUF at all.
     */
    fun autoFile(files: List<HubFile>, budgetBytes: Long): HubFile? {
        if (files.isEmpty()) return null
        val usable = files.filter { !it.isProjector && !it.isLora }
        if (usable.isEmpty()) return null
        val fitting = if (budgetBytes > 0L) usable.filter { it.bytes in 1..budgetBytes } else usable
        val preferred = fitting.filter { it.quant.equals("Q4_K_M", ignoreCase = true) }
        return preferred.maxByOrNull { it.bytes }
            ?: fitting.maxByOrNull { it.bytes }
            ?: usable.minByOrNull { it.bytes }
    }

    const val ACTIVE_POINTER = "active.txt"
}
