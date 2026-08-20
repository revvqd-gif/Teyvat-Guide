package com.teyvatmap.data

object CookieParser {

    /**
     * Parses cookie from either:
     * 1. Netscape format (tab-separated, as exported by Cookie-Editor)
     * 2. JSON array format (Cookie-Editor export)
     * 3. Simple key=value; format
     */
    fun parseCookie(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        // Try JSON format first (Cookie-Editor export)
        if (trimmed.startsWith("[")) {
            return parseJsonFormat(trimmed)
        }

        // Try Netscape format (tab-separated with tabs)
        if (trimmed.contains("\t") || trimmed.contains("#HttpOnly")) {
            return parseNetscapeFormat(trimmed)
        }

        // Already in key=value; format
        return trimmed
    }

    private fun parseNetscapeFormat(raw: String): String {
        val lines = raw.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
        val pairs = mutableListOf<String>()

        for (line in lines) {
            val parts = line.split("\t")
            if (parts.size >= 7) {
                val name = parts[5]
                val value = parts[6]
                if (name.isNotBlank() && value.isNotBlank()) {
                    pairs.add("$name=$value")
                }
            }
        }
        return pairs.joinToString("; ")
    }

    private fun parseJsonFormat(raw: String): String {
        // Simple JSON parsing without full Moshi for cookie parsing
        // JSON format: [{"name":"...","value":"...","domain":"...",...},...]
        val pairs = mutableListOf<String>()
        try {
            // Simple parsing - extract name/value pairs
            val regex = "\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            val matches = regex.findAll(raw)
            for (match in matches) {
                val name = match.groupValues[1]
                val value = match.groupValues[2]
                if (name.isNotBlank() && value.isNotBlank()) {
                    pairs.add("$name=$value")
                }
            }
        } catch (e: Exception) {
            // Fallback to raw
            return trimmed
        }
        return pairs.joinToString("; ")
    }

    // Key cookies needed for Hoyolab API
    fun extractKeyCookies(cookieString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pairs = cookieString.split(";").map { it.trim() }
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val name = pair.substring(0, idx)
                val value = pair.substring(idx + 1)
                result[name] = value
            }
        }
        return result.toMap()
    }

    // Check if cookie has required tokens
    fun hasValidTokens(cookieString: String): Boolean {
        val cookies = extractKeyCookies(cookieString)
        return cookies.containsKey("ltoken_v2") && cookies.containsKey("cookie_token_v2")
    }
}