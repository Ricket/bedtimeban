package ricket.bedtimeban.common.localization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BedtimeTranslator {
    public static final String DEFAULT_LOCALE = "en_us";

    private final Map<String, Map<String, String>> translationsByLocale = new ConcurrentHashMap<>();

    public String render(String localeCode, LocalizedMessage message) {
        return render(localeCode, message.key(), message.args());
    }

    public String render(String localeCode, String key, Object... args) {
        String normalizedLocale = normalizeLocale(localeCode);
        String template = translationMap(normalizedLocale).get(key);
        if (template == null) {
            template = translationMap(DEFAULT_LOCALE).getOrDefault(key, key);
        }
        return formatTemplate(template, args, toJavaLocale(normalizedLocale));
    }

    public String normalizeLocale(String localeCode) {
        if (localeCode == null || localeCode.isBlank()) {
            return DEFAULT_LOCALE;
        }
        return localeCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public Locale toJavaLocale(String localeCode) {
        String normalizedLocale = normalizeLocale(localeCode);
        String[] parts = normalizedLocale.split("_", 2);
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1].toUpperCase(Locale.ROOT));
        }
        return new Locale(parts[0]);
    }

    private Map<String, String> translationMap(String localeCode) {
        return translationsByLocale.computeIfAbsent(localeCode, this::loadTranslations);
    }

    private Map<String, String> loadTranslations(String localeCode) {
        String resourcePath = "assets/bedtimeban/lang/" + localeCode + ".json";
        try (InputStream inputStream = BedtimeTranslator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return Map.of();
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, String> translations = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Map.copyOf(translations);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String formatTemplate(String template, Object[] args, Locale locale) {
        StringBuilder result = new StringBuilder();
        int sequentialIndex = 0;

        for (int i = 0; i < template.length(); i++) {
            char current = template.charAt(i);
            if (current != '%') {
                result.append(current);
                continue;
            }
            if (i + 1 >= template.length()) {
                result.append('%');
                continue;
            }

            char next = template.charAt(i + 1);
            if (next == '%') {
                result.append('%');
                i++;
                continue;
            }

            int argumentIndex = sequentialIndex;
            int scan = i + 1;
            int explicitIndex = 0;
            while (scan < template.length() && Character.isDigit(template.charAt(scan))) {
                explicitIndex = (explicitIndex * 10) + (template.charAt(scan) - '0');
                scan++;
            }
            if (scan < template.length() && template.charAt(scan) == '$') {
                argumentIndex = Math.max(0, explicitIndex - 1);
                scan++;
            } else {
                scan = i + 1;
            }

            if (scan < template.length() && template.charAt(scan) == 's') {
                result.append(formatArgument(args, argumentIndex, locale));
                sequentialIndex++;
                i = scan;
                continue;
            }

            result.append('%');
        }

        return result.toString();
    }

    private String formatArgument(Object[] args, int argumentIndex, Locale locale) {
        if (argumentIndex < 0 || argumentIndex >= args.length) {
            return "";
        }
        Object value = args[argumentIndex];
        if (value instanceof Number number) {
            return NumberFormat.getNumberInstance(locale).format(number);
        }
        return String.valueOf(value);
    }
}
