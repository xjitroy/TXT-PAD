package com.secure.textpad.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Plain paragraph editor: exporting DOCX deliberately creates a fresh text-only document. */
object DocxHandler {
    private const val WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    const val MAX_TEXT = 2_000_000

    fun read(input: InputStream): String {
        ZipInputStream(input).use { zip ->
            var count = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++count <= 4096) { "Too many DOCX entries." }
                if (entry.name == "word/document.xml") {
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                        setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                        setInput(LimitedInputStream(zip, 16L * 1024 * 1024), "UTF-8")
                    }
                    val text = StringBuilder()
                    var inText = false
                    var firstParagraph = true
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.DOCDECL) error("Document declarations are not permitted.")
                        if (event == XmlPullParser.START_TAG && parser.namespace == WORD) when (parser.name) {
                            "p" -> { if (!firstParagraph) text.append('\n'); firstParagraph = false }
                            "t" -> inText = true
                            "tab" -> text.append('\t')
                            "br", "cr" -> text.append('\n')
                        }
                        if (inText && (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT ||
                                event == XmlPullParser.ENTITY_REF || event == XmlPullParser.IGNORABLE_WHITESPACE)) {
                            text.append(parser.text ?: error("Unresolved XML entity in document text."))
                        }
                        if (event == XmlPullParser.END_TAG && parser.namespace == WORD && parser.name == "t") inText = false
                        require(text.length <= MAX_TEXT) { "Document exceeds the editor's 2 million character limit." }
                        event = parser.nextToken()
                    }
                    return text.toString()
                }
                // Bound skipped entries as well; never trust ZIP's advertised uncompressed sizes.
                val skip = ByteArray(8192)
                var total = 0L
                while (true) { val n = zip.read(skip); if (n < 0) break; total += n; require(total <= 64L * 1024 * 1024) { "DOCX entry too large." } }
                zip.closeEntry()
            }
        }
        error("DOCX has no word/document.xml.")
    }

    fun write(text: String, output: OutputStream) {
        require(text.length <= MAX_TEXT)
        ZipOutputStream(output).use { zip ->
            fun part(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            part("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
            part("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
            zip.putNextEntry(ZipEntry("word/document.xml"))
            val xml = Xml.newSerializer()
            xml.setOutput(zip, "UTF-8"); xml.startDocument("UTF-8", true)
            xml.setPrefix("w", WORD); xml.startTag(WORD, "document"); xml.startTag(WORD, "body")
            text.splitToSequence('\n').forEach { line ->
                xml.startTag(WORD, "p"); xml.startTag(WORD, "r")
                xml.startTag(WORD, "t"); xml.attribute("http://www.w3.org/XML/1998/namespace", "space", "preserve")
                xml.text(line); xml.endTag(WORD, "t"); xml.endTag(WORD, "r"); xml.endTag(WORD, "p")
            }
            xml.endTag(WORD, "body"); xml.endTag(WORD, "document"); xml.endDocument(); xml.flush(); zip.closeEntry()
        }
    }

    private class LimitedInputStream(input: InputStream, val limit: Long) : FilterInputStream(input) {
        private var total = 0L
        override fun read(): Int = super.read().also { if (it >= 0) { total++; require(total <= limit) { "DOCX XML too large." } } }
        override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len).also {
            if (it > 0) { total += it; require(total <= limit) { "DOCX XML too large." } }
        }
        override fun close() { }
    }
}
