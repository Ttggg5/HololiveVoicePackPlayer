package com.ttggg5.holovoice.data

/** Case-insensitive comparison that orders embedded numbers by value ("Clip 2" before "Clip 10"). */
object NaturalOrder : Comparator<String> {
    private val CHUNK = Regex("""\d+|\D+""")

    override fun compare(a: String, b: String): Int {
        val x = CHUNK.findAll(a.lowercase()).map { it.value }.toList()
        val y = CHUNK.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(x.size, y.size)) {
            val p = x[i]
            val q = y[i]
            val c = if (p[0].isDigit() && q[0].isDigit()) {
                val n = p.trimStart('0').length.compareTo(q.trimStart('0').length)
                if (n != 0) n else p.trimStart('0').compareTo(q.trimStart('0'))
            } else {
                p.compareTo(q)
            }
            if (c != 0) return c
        }
        return x.size.compareTo(y.size)
    }
}
