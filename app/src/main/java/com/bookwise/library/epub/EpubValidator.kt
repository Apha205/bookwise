package com.bookwise.library.epub

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val MAX_EPUB_BYTES = 100L * 1024 * 1024

data class EpubMetadata(
    val title: String?,
    val authors: List<String>,
    val coverBytes: ByteArray?,
    val coverMimeType: String?,
    val identifier: String?,
    val language: String?,
    val published: String?,
    val description: String?
)

data class ValidatedEpub(
    val fileSizeBytes: Long,
    val sha256: String,
    val metadata: EpubMetadata
)

sealed interface ImportResult {
    data class Success(val validated: ValidatedEpub) : ImportResult
    data class Failure(val reason: FailureReason) : ImportResult
}

enum class FailureReason {
    TooLarge,
    NotZip,
    MissingMimetype,
    BadMimetype,
    ZipSlip,
    MissingContainer,
    MissingOpf,
    MalformedOpf,
    MissingTitle,
    CorruptEntry,
    IoError,
    Unknown
}

class EpubValidator {
    fun validate(input: InputStream, declaredSize: Long? = null): ImportResult {
        val buffered = BufferedInputStream(input)
        val bytes = try {
            readFullyLimited(buffered, MAX_EPUB_BYTES)
        } catch (e: TooLargeException) {
            return ImportResult.Failure(FailureReason.TooLarge)
        } catch (e: IOException) {
            return ImportResult.Failure(FailureReason.IoError)
        }

        if (bytes.size < 4) return ImportResult.Failure(FailureReason.NotZip)
        val sig = bytes[0].toInt() and 0xFF shl 24 or (bytes[1].toInt() and 0xFF shl 16) or
            (bytes[2].toInt() and 0xFF shl 8) or (bytes[3].toInt() and 0xFF)
        if (sig != 0x504B0304 && sig != 0x504B0506 && sig != 0x504B0708) return ImportResult.Failure(FailureReason.NotZip)

        val sha = sha256Hex(bytes)
        val entries = mutableMapOf<String, ByteArray>()
        val tempFile = java.io.File.createTempFile("epub", ".zip")
        try {
            tempFile.writeBytes(bytes)
            val zipFile = java.util.zip.ZipFile(tempFile)
            try {
                val enumeration = zipFile.entries()
                while (enumeration.hasMoreElements()) {
                    val entry = enumeration.nextElement()
                    val name = entry.name
                    if (name.startsWith("/") || name.contains("..")) return ImportResult.Failure(FailureReason.ZipSlip)
                    if (!entry.isDirectory) {
                        entries[name] = zipFile.getInputStream(entry).readBytesLimited(50L * 1024 * 1024)
                    }
                }
            } finally {
                zipFile.close()
            }
        } catch (e: IOException) {
            return ImportResult.Failure(FailureReason.CorruptEntry)
        } finally {
            tempFile.delete()
        }

        val mimetypeEntry = entries["mimetype"]
            ?: entries.entries.firstOrNull {
                val name = it.key.trim().removePrefix("./").removePrefix("/").lowercase()
                name == "mimetype" || name.endsWith("/mimetype") || name.endsWith("mimetype")
            }?.value
        if (mimetypeEntry == null) {
            android.util.Log.w("EpubValidator", "mimetype entry not found. ZIP entries: ${entries.keys}")
            return ImportResult.Failure(FailureReason.MissingMimetype)
        }
        val raw = mimetypeEntry.toString(Charsets.UTF_8)
        val mimetypeText = raw.removePrefix("\uFEFF").replace(Regex("\\s+"), "")
        android.util.Log.d("EpubValidator", "mimetype raw=[$raw] normalized=[$mimetypeText]")
        if (!mimetypeText.contains("application/epub+zip", ignoreCase = true)) {
            android.util.Log.w("EpubValidator", "Bad mimetype: [$mimetypeText]")
            return ImportResult.Failure(FailureReason.BadMimetype)
        }

        val containerName = "META-INF/container.xml"
        if (containerName !in entries) return ImportResult.Failure(FailureReason.MissingContainer)

        val opfPath = extractRootfile(entries[containerName]!!) ?: return ImportResult.Failure(FailureReason.MissingOpf)
        val opfBytes = entries[opfPath] ?: return ImportResult.Failure(FailureReason.MissingOpf)

        val metadata = try {
            parseOpf(opfBytes, entries)
        } catch (e: Exception) {
            return ImportResult.Failure(FailureReason.MalformedOpf)
        }

        if (metadata.title.isNullOrBlank()) return ImportResult.Failure(FailureReason.MissingTitle)

        return ImportResult.Success(
            ValidatedEpub(
                fileSizeBytes = bytes.size.toLong(),
                sha256 = sha,
                metadata = metadata
            )
        )
    }

    private fun extractRootfile(containerBytes: ByteArray): String? {
        val text = containerBytes.toString(Charsets.UTF_8)
        val regex = Regex("""full-path\s*=\s*"([^"]+)"|full-path\s*=\s*'([^']+)'""")
        val m = regex.find(text) ?: return null
        return (m.groups[1]?.value ?: m.groups[2]?.value)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun parseOpf(opfBytes: ByteArray, entries: Map<String, ByteArray>): EpubMetadata {
        val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(opfBytes.inputStream())

        fun findAll(tag: String): List<String> {
            val nl = doc.getElementsByTagNameNS("*", tag)
            return List(nl.length) { nl.item(it).textContent?.trim().orEmpty() }.filter { it.isNotBlank() }
        }

        val metadataEl = doc.getElementsByTagNameNS("*", "metadata").let { if (it.length > 0) it.item(0) as org.w3c.dom.Element? else null }

        val title = run {
            val nl = doc.getElementsByTagNameNS("*", "title")
            (0 until nl.length).firstNotNullOfOrNull { i ->
                val el = nl.item(i) as org.w3c.dom.Element
                val refines = el.getAttribute("refine")
                if (refines.isNullOrBlank() || refines == "title-type") el.textContent?.trim()?.takeIf { it.isNotBlank() } else null
            } ?: findAll("title").firstOrNull()
        }

        val creators = findAll("creator")
        val identifier = findAll("identifier").firstOrNull()
        val language = findAll("language").firstOrNull()
        val dates = findAll("date")
        val published = dates.firstOrNull { it.matches(Regex("""\d{4}.*""")) } ?: dates.firstOrNull()
        val description = findAll("description").firstOrNull()

        var coverBytes: ByteArray? = null
        var coverMime: String? = null
        val manifest = doc.getElementsByTagNameNS("*", "manifest").let { if (it.length > 0) it.item(0) as org.w3c.dom.Element? else null }
        manifest?.let { man ->
            val items = man.getElementsByTagNameNS("*", "item")
            for (i in 0 until items.length) {
                val item = items.item(i) as org.w3c.dom.Element
                val props = item.getAttribute("properties")
                val mediaType = item.getAttribute("media-type")
                val href = item.getAttribute("href")
                if (props.contains("cover", ignoreCase = true) || item.getAttribute("id") == "cover") {
                    val bytes = entries[href] ?: findEntryByName(entries, href)
                    if (bytes != null) {
                        coverBytes = bytes
                        coverMime = mediaType.takeIf { it.isNotBlank() }
                        break
                    }
                }
            }
            if (coverBytes == null) {
                for (i in 0 until items.length) {
                    val item = items.item(i) as org.w3c.dom.Element
                    val mediaType = item.getAttribute("media-type")
                    if (mediaType.startsWith("image/")) {
                        val href = item.getAttribute("href")
                        val bytes = entries[href] ?: findEntryByName(entries, href)
                        if (bytes != null) {
                            coverBytes = bytes
                            coverMime = mediaType.takeIf { it.isNotBlank() }
                            break
                        }
                    }
                }
            }
        }

        return EpubMetadata(
            title = title,
            authors = creators,
            coverBytes = coverBytes,
            coverMimeType = coverMime,
            identifier = identifier,
            language = language,
            published = published,
            description = description
        )
    }

    private fun findEntryByName(entries: Map<String, ByteArray>, href: String): ByteArray? {
        val normalized = href.removePrefix("./")
        entries[normalized]?.let { return it }
        return entries.entries.firstOrNull { it.key.endsWith("/$normalized") || it.key.endsWith(normalized) }?.value
    }

    private fun InputStream.readBytesLimited(max: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var remaining = max
        while (true) {
            val toRead = minOf(buf.size.toLong(), remaining + 1).toInt()
            if (toRead <= 0) break
            val n = read(buf, 0, toRead)
            if (n == -1) break
            remaining -= n
            if (remaining < 0) throw IOException("Entry too large")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun readFullyLimited(input: InputStream, maxBytes: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n == -1) break
            total += n
            if (total > maxBytes) throw TooLargeException()
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private class TooLargeException : Exception()

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
