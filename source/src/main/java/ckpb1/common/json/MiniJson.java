package ckpb1.common.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency-free JSON reader/writer used by CK_PB1 (launcher manifests,
 * release checks and client configs).
 *
 * Parsed values map to: Map&lt;String,Object&gt; (objects, insertion order kept),
 * List&lt;Object&gt; (arrays), String, Long / Double (numbers), Boolean and null.
 */
public final class MiniJson {

    private MiniJson() {
    }

    // ------------------------------------------------------------------ read

    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object v = p.parseValue();
        p.skipWs();
        if (!p.eof()) {
            throw p.err("Trailing characters after JSON value");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        if (o instanceof List) {
            return (List<Object>) o;
        }
        return new ArrayList<>();
    }

    public static String str(Object o, String def) {
        if (o instanceof String s) return s;
        return def;
    }

    public static double num(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        return def;
    }

    public static long lng(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        return def;
    }

    public static boolean bool(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        return def;
    }

    // ----------------------------------------------------------------- write

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(sb, s);
        } else if (v instanceof Boolean b) {
            sb.append(b.booleanValue() ? "true" : "false");
        } else if (v instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) {
                sb.append('0');
            } else if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1e15) {
                sb.append((long) (double) d);
            } else {
                sb.append(d.doubleValue());
            }
        } else if (v instanceof Float f) {
            writeValue(sb, (double) f);
        } else if (v instanceof Number || v instanceof Character) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object e : it) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, e);
            }
            sb.append(']');
        } else {
            writeString(sb, String.valueOf(v));
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // --------------------------------------------------------------- parser

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src == null ? "" : src;
        }

        boolean eof() {
            return pos >= src.length();
        }

        IllegalArgumentException err(String msg) {
            return new IllegalArgumentException("JSON error at " + pos + ": " + msg);
        }

        void skipWs() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        char peek() {
            if (eof()) throw err("Unexpected end of input");
            return src.charAt(pos);
        }

        void expect(char c) {
            if (eof() || src.charAt(pos) != c) {
                throw err("Expected '" + c + "'");
            }
            pos++;
        }

        Object parseValue() {
            skipWs();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseKeyword("true", Boolean.TRUE);
                case 'f' -> parseKeyword("false", Boolean.FALSE);
                case 'n' -> parseKeyword("null", null);
                default -> parseNumber();
            };
        }

        private Object parseKeyword(String kw, Object value) {
            if (!src.startsWith(kw, pos)) {
                throw err("Invalid literal");
            }
            pos += kw.length();
            return value;
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> out = new LinkedHashMap<>();
            skipWs();
            if (!eof() && peek() == '}') {
                pos++;
                return out;
            }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                expect(':');
                out.put(key, parseValue());
                skipWs();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == '}') {
                    pos++;
                    return out;
                } else {
                    throw err("Expected ',' or '}'");
                }
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> out = new ArrayList<>();
            skipWs();
            if (!eof() && peek() == ']') {
                pos++;
                return out;
            }
            while (true) {
                out.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') {
                    pos++;
                } else if (c == ']') {
                    pos++;
                    return out;
                } else {
                    throw err("Expected ',' or ']'");
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) throw err("Unterminated string");
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (eof()) throw err("Unterminated escape");
                    char e = src.charAt(pos++);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 > src.length()) throw err("Invalid \\u escape");
                            sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw err("Invalid escape '\\" + e + "'");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Object parseNumber() {
            int start = pos;
            if (!eof() && (peek() == '-' || peek() == '+')) {
                pos++;
            }
            boolean fp = false;
            while (!eof()) {
                char c = src.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    fp = fp || c == '.' || c == 'e' || c == 'E';
                    pos++;
                } else {
                    break;
                }
            }
            String num = src.substring(start, pos);
            if (num.isEmpty() || num.equals("-")) {
                throw err("Invalid number");
            }
            try {
                if (fp) {
                    return Double.parseDouble(num);
                }
                return Long.parseLong(num);
            } catch (NumberFormatException ex) {
                try {
                    return Double.parseDouble(num);
                } catch (NumberFormatException ex2) {
                    throw err("Invalid number '" + num + "'");
                }
            }
        }
    }
}
