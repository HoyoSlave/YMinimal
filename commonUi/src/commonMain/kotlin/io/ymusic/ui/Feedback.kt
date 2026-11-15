package io.ymusic.ui

internal class FeedbackInfo(
    val deviceId: String,
    val app: String,
    val engine: String?,
    val language: String,
    private val email: String,
    private val issueUrl: String,
) {
    private val lines: String
        get() = listOfNotNull(
            "Device ID: $deviceId",
            "App: $app",
            engine?.let { "Engine: $it" },
            "Language: $language",
        ).joinToString("\n")

    val text: String get() = "```\n$lines\n```"

    fun githubIssueUrl(): String = "$issueUrl${if ('?' in issueUrl) '&' else '?'}body=${percentEncode(prefill(text))}"

    fun mailtoUrl(subject: String): String =
        "mailto:$email?subject=${percentEncode(subject)}&body=${percentEncode(prefill(lines))}"

    private fun prefill(info: String): String = "\n\n---\n$info"
}

internal fun percentEncode(value: String): String = buildString {
    for (byte in value.encodeToByteArray()) {
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char in "-._~") {
            append(char)
        } else {
            append('%')
            append(HEX[code shr 4])
            append(HEX[code and 0x0F])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
