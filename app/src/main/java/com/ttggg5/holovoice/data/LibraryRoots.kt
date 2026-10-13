package com.ttggg5.holovoice.data

import android.content.Context

/**
 * Folders imported with "Import many topics": each sub-folder of a root is its own topic.
 * Sub-folder URIs are tree-based document URIs, i.e. they start with "<root>/document/".
 */
class LibraryRoots(context: Context) {
    private val prefs = context.getSharedPreferences("library_roots", Context.MODE_PRIVATE)

    val roots: Set<String> get() = prefs.getStringSet(KEY_ROOTS, emptySet()).orEmpty()

    /** Sub-folders whose topic the user deleted; sync leaves them out. */
    val excluded: Set<String> get() = prefs.getStringSet(KEY_EXCLUDED, emptySet()).orEmpty()

    fun add(root: String) {
        prefs.edit().putStringSet(KEY_ROOTS, roots + root).apply()
    }

    /** Stops syncing a library folder (its exclusions are forgotten too). */
    fun remove(root: String) {
        prefs.edit()
            .putStringSet(KEY_ROOTS, roots - root)
            .putStringSet(KEY_EXCLUDED, excluded.filterNot { it.startsWith(childPrefix(root)) }.toSet())
            .apply()
    }

    fun exclude(sourceUri: String) {
        prefs.edit().putStringSet(KEY_EXCLUDED, excluded + sourceUri).apply()
    }

    /** Re-importing a sub-folder on its own (or the whole root) brings it back. */
    fun include(sourceUri: String) {
        prefs.edit().putStringSet(KEY_EXCLUDED, excluded.filterNot { it == sourceUri || it.startsWith(sourceUri) }.toSet()).apply()
    }

    fun rootOf(sourceUri: String): String? = roots.firstOrNull { sourceUri.startsWith(childPrefix(it)) }

    private companion object {
        const val KEY_ROOTS = "roots"
        const val KEY_EXCLUDED = "excluded"
    }
}

fun childPrefix(root: String): String = "$root/document/"
