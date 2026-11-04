import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

// pack.mcmeta in the 1.20.1 format (pack_format 15), keeping the description
class LegacyPackMeta(input: Reader) : FilterReader(StringReader(convert(input.readText()))) {
    companion object {
        fun convert(text: String): String {
            val desc = Regex("\"description\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1) ?: ""
            return "{\n    \"pack\": {\n        \"description\": \"$desc\",\n        \"pack_format\": 15\n    }\n}\n"
        }
    }
}
