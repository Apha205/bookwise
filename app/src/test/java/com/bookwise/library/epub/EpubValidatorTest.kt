package com.bookwise.library.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubValidatorTest {

    private val validator = EpubValidator()

    @Test
    fun validate_notZip_returnsNotZip() {
        val result = validator.validate("not a zip".byteInputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.NotZip)
    }

    @Test
    fun validate_empty_returnsNotZip() {
        val result = validator.validate(ByteArray(0).inputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.NotZip)
    }

    @Test
    fun validate_wrongMimetype_returnsBadMimetype() {
        val epub = buildEpub(mimetype = "application/pdf")
        val result = validator.validate(epub.inputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.BadMimetype)
    }

    @Test
    fun validate_missingContainer_returnsMissingContainer() {
        val epub = buildEpub(includeContainer = false)
        val result = validator.validate(epub.inputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.MissingContainer)
    }

    @Test
    fun validate_missingOpf_returnsMissingOpf() {
        val epub = buildEpub(includeOpf = false)
        val result = validator.validate(epub.inputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.MissingOpf)
    }

    @Test
    fun validate_missingTitle_returnsMissingTitle() {
        val epub = buildEpub(title = null)
        val result = validator.validate(epub.inputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.MissingTitle)
    }

    @Test
    fun validate_validEpub_returnsSuccess() {
        val epub = buildEpub(title = "My Book")
        val result = validator.validate(epub.inputStream())
        assertTrue(result is ImportResult.Success)
        val success = (result as ImportResult.Success).validated
        assertEquals("My Book", success.metadata.title)
        assertEquals(64, success.sha256.length) // SHA-256 hex is 64 chars
        assertTrue(success.fileSizeBytes > 0)
    }

    @Test
    fun validate_zipSlip_returnsZipSlip() {
        val epub = buildEpub(extraEntries = mapOf("../evil.txt" to "evil".toByteArray()))
        val result = validator.validate(epub.inputStream())
        assertTrue(result is ImportResult.Failure && result.reason == FailureReason.ZipSlip)
    }

    private fun buildEpub(
        title: String? = "My Book",
        mimetype: String = "application/epub+zip",
        includeContainer: Boolean = true,
        includeOpf: Boolean = true,
        extraEntries: Map<String, ByteArray> = emptyMap()
    ): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("mimetype"))
            zos.write(mimetype.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            if (includeContainer) {
                val container = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".toByteArray(Charsets.UTF_8)
                zos.putNextEntry(java.util.zip.ZipEntry("META-INF/container.xml"))
                zos.write(container)
                zos.closeEntry()
            }
            if (includeOpf) {
                val titleTag = if (title != null) "<dc:title>$title</dc:title>" else ""
                val opf = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="BookID">urn:uuid:test</dc:identifier>
    <dc:language>en</dc:language>
    $titleTag
    <dc:creator>Test Author</dc:creator>
    <meta property="dcterms:modified">2024-01-01T00:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="toc" href="toc.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="nav"/>
  </spine>
</package>""".toByteArray(Charsets.UTF_8)
                zos.putNextEntry(java.util.zip.ZipEntry("OEBPS/content.opf"))
                zos.write(opf)
                zos.closeEntry()
            }
            extraEntries.forEach { (name, bytes) ->
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
