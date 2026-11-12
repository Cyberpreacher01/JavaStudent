import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonUtil.java
 * Builds and parses JSON text by hand so that no external library is needed.
 */
public class JsonUtil {

    /** Escapes special characters so any text is safe inside a JSON string. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n");  break;
                case '\r': out.append("\\r");  break;
                case '\t': out.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    /** {"code":"COS201","title":"Programming I","units":3} */
    public static String courseToJson(Course course) {
        return "{\"code\":\"" + escape(course.getCode()) + "\","
             + "\"title\":\"" + escape(course.getTitle()) + "\","
             + "\"units\":" + course.getUnits() + "}";
    }

    /** [ {...}, {...} ] */
    public static String courseListToJson(List<Course> courses) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < courses.size(); i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append(courseToJson(courses.get(i)));
        }
        return out.append("]").toString();
    }

    /** {"success":true,"message":"..."} plus any extra fields already in JSON form. */
    public static String response(boolean success, String message, String extraFields) {
        String json = "{\"success\":" + success + ",\"message\":\"" + escape(message) + "\"";
        if (extraFields != null && !extraFields.isEmpty()) {
            json += "," + extraFields;
        }
        return json + "}";
    }

    /** Converts a list of text messages to a JSON array of strings. */
    public static String stringListToJson(List<String> items) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append("\"").append(escape(items.get(i))).append("\"");
        }
        return out.append("]").toString();
    }

    // ---------- Parsing (for loading courses back from a JSON file) ----------

    /** Parses a JSON array of flat objects, e.g. the text produced by
     *  courseListToJson: [{"code":"COS201","title":"Programming I","units":3}, ...].
     *  Each object's values are returned as plain text (quotes removed), so the
     *  caller can validate and build its own objects from them. */
    public static List<Map<String, String>> parseObjectArray(String text) {
        Cursor cursor = new Cursor(text);
        cursor.skipWhitespace();
        cursor.expect('[');
        List<Map<String, String>> items = new ArrayList<>();
        cursor.skipWhitespace();
        if (cursor.peek() == ']') {
            cursor.next();
            return items;
        }
        while (true) {
            cursor.skipWhitespace();
            items.add(parseObject(cursor));
            cursor.skipWhitespace();
            char c = cursor.next();
            if (c == ']') break;
            if (c != ',') throw new IllegalArgumentException("Expected ',' or ']' at position " + (cursor.pos - 1));
        }
        return items;
    }

    private static Map<String, String> parseObject(Cursor cursor) {
        Map<String, String> map = new HashMap<>();
        cursor.expect('{');
        cursor.skipWhitespace();
        if (cursor.peek() == '}') {
            cursor.next();
            return map;
        }
        while (true) {
            cursor.skipWhitespace();
            String key = cursor.parseString();
            cursor.skipWhitespace();
            cursor.expect(':');
            cursor.skipWhitespace();
            map.put(key, cursor.parseValue());
            cursor.skipWhitespace();
            char c = cursor.next();
            if (c == '}') break;
            if (c != ',') throw new IllegalArgumentException("Expected ',' or '}' at position " + (cursor.pos - 1));
        }
        return map;
    }

    /** Walks through JSON text one character at a time. */
    private static class Cursor {
        final String text;
        int pos = 0;

        Cursor(String text) { this.text = text; }

        char peek() {
            if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON.");
            return text.charAt(pos);
        }

        char next() {
            char c = peek();
            pos++;
            return c;
        }

        void expect(char expected) {
            char c = next();
            if (c != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + (pos - 1));
            }
        }

        void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default:   sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        /** Parses a string, number, true/false or null and returns it as plain text. */
        String parseValue() {
            if (peek() == '"') {
                return parseString();
            }
            int start = pos;
            while (pos < text.length() && ",}] \t\n\r".indexOf(text.charAt(pos)) < 0) {
                pos++;
            }
            return text.substring(start, pos);
        }
    }
}
