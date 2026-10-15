package com.ttggg5.holovoice.alarm

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A short record of what happened when alarms went off (started, refused by Android, which clip,
 * player errors…), shown on the Alarms page so a silent alarm can be explained and reported.
 */
object AlarmLog {
    private const val MAX_LINES = 30
    private val time = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

    private fun file(context: Context) = File(context.applicationContext.filesDir, "alarm_log.txt")

    @Synchronized
    fun record(context: Context, message: String) {
        runCatching {
            val f = file(context)
            val lines = (if (f.isFile) f.readLines() else emptyList()) + "${LocalDateTime.now().format(time)}  $message"
            f.writeText(lines.takeLast(MAX_LINES).joinToString("\n"))
        }
    }

    /** [message] plus the exception's type and message. */
    fun record(context: Context, message: String, error: Throwable) =
        record(context, "$message: ${error::class.java.simpleName}: ${error.message}")

    fun read(context: Context): String = runCatching { file(context).takeIf { it.isFile }?.readText() }.getOrNull().orEmpty()

    fun clear(context: Context) {
        file(context).delete()
    }
}
