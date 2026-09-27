package dev.eftrainer.data.generated

/**
 * Крошечный JSON без зависимостей: разбор ответа ИИ и payload в Room.
 * org.json на JVM-тестах — заглушка Android, а kotlinx-serialization-json не подключён.
 * Объект → Map<String, Any?>, массив → List<Any?>, числа → Double.
 */
object Json {

    class ParseException(message: String) : Exception(message)

    fun parse(text: String): Any? {
        val p = Parser(text)
        val value = p.value()
        p.skipSpaces()
        if (!p.atEnd()) throw ParseException("лишнее после JSON на ${p.pos}")
        return value
    }

    fun string(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }

    /** Запись: Map / List / String / Number / Boolean / null. */
    fun write(value: Any?): String = when (value) {
        null -> "null"
        is String -> string(value)
        is Boolean, is Number -> value.toString()
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (k, v) -> string(k.toString()) + ":" + write(v) }
        is List<*> -> value.joinToString(",", "[", "]") { write(it) }
        else -> throw IllegalArgumentException("не JSON: $value")
    }

    private class Parser(val s: String) {
        var pos = 0

        fun atEnd() = pos >= s.length

        fun skipSpaces() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun value(): Any? {
            skipSpaces()
            if (atEnd()) throw ParseException("JSON оборвался")
            return when (val c = s[pos]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> word("true", true)
                'f' -> word("false", false)
                'n' -> word("null", null)
                else -> if (c == '-' || c.isDigit()) num() else throw ParseException("неожиданный '$c' на $pos")
            }
        }

        private fun word(w: String, v: Any?): Any? {
            if (!s.startsWith(w, pos)) throw ParseException("ожидали $w на $pos")
            pos += w.length
            return v
        }

        private fun num(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] in "+-.eE")) pos++
            return s.substring(start, pos).toDoubleOrNull() ?: throw ParseException("плохое число на $start")
        }

        private fun expect(c: Char) {
            skipSpaces()
            if (atEnd() || s[pos] != c) throw ParseException("ожидали '$c' на $pos")
            pos++
        }

        private fun obj(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipSpaces()
            if (!atEnd() && s[pos] == '}') { pos++; return map }
            while (true) {
                skipSpaces()
                val key = str()
                expect(':')
                map[key] = value()
                skipSpaces()
                if (atEnd()) throw ParseException("JSON оборвался")
                when (s[pos++]) {
                    ',' -> continue
                    '}' -> return map
                    else -> throw ParseException("ожидали , или } на ${pos - 1}")
                }
            }
        }

        private fun arr(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipSpaces()
            if (!atEnd() && s[pos] == ']') { pos++; return list }
            while (true) {
                list += value()
                skipSpaces()
                if (atEnd()) throw ParseException("JSON оборвался")
                when (s[pos++]) {
                    ',' -> continue
                    ']' -> return list
                    else -> throw ParseException("ожидали , или ] на ${pos - 1}")
                }
            }
        }

        private fun str(): String {
            if (atEnd() || s[pos] != '"') throw ParseException("ожидали строку на $pos")
            pos++
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw ParseException("строка не закрыта")
                when (val c = s[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd()) throw ParseException("строка не закрыта")
                        when (val e = s[pos++]) {
                            'n' -> sb.append('\n')
                            't' -> sb.append('\t')
                            'r' -> sb.append('\r')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (pos + 4 > s.length) throw ParseException("плохой \\u")
                                sb.append(s.substring(pos, pos + 4).toIntOrNull(16)?.toChar() ?: throw ParseException("плохой \\u"))
                                pos += 4
                            }
                            else -> sb.append(e)   // \" \\ \/
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }
    }
}
