import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

// fabric.mod.json without the accessWidener line, for versions that need none
class StripAccessWidener(input: Reader) : FilterReader(
    StringReader(input.readText().lines().filterNot { it.contains("\"accessWidener\"") }.joinToString("\n"))
)
