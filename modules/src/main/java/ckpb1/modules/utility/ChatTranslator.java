package ckpb1.modules.utility;

import ckpb1.client.core.Category;
import ckpb1.client.core.Module;
import ckpb1.client.core.setting.ModeSetting;
import ckpb1.common.json.MiniJson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Chat Translator: translates incoming chat messages to your language and
 * shows the translation below the original (simplified Wurst
 * "ChatTranslator"). Uses the public Google translate endpoint; translation
 * is best-effort and never blocks or breaks chat.
 */
public final class ChatTranslator extends Module {

    public final ModeSetting language = add(new ModeSetting("Language",
            "Translate chat into this language", "fa", "en", "ar", "tr", "fr", "de", "ru", "es"));

    private static volatile boolean active;
    /** Guards against translating our own translated messages (feedback loop). */
    private static boolean displaying;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ChatTranslator() {
        super("Chat Translator", "Translates incoming chat messages to your language", Category.UTILITY);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    /** Called by ChatHudMixin for every message added to the chat HUD. */
    public static void onChatMessage(Text message) {
        if (!active || displaying) {
            return;
        }
        String text = message.getString();
        // skip our own client messages, empty/short lines and commands
        if (text.length() < 3 || text.startsWith("[CK_PB1]") || text.startsWith("/")) {
            return;
        }
        var modules = ckpb1.client.CKPB1Client.modules();
        if (modules == null) {
            return;
        }
        Module self = modules.byName("Chat Translator");
        if (!(self instanceof ChatTranslator translator) || !self.isEnabled()) {
            return;
        }
        String target = translator.language.get();

        CompletableFuture.runAsync(() -> {
            String translated = translate(text, target);
            if (translated == null || translated.isBlank() || translated.equals(text)) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.inGameHud == null) {
                return;
            }
            mc.execute(() -> {
                if (mc.inGameHud.getChatHud() == null) {
                    return;
                }
                displaying = true;
                try {
                    mc.inGameHud.getChatHud().addMessage(Text.literal(
                            "§8[§b" + target.toUpperCase(Locale.ROOT) + "§8] §f" + translated));
                } finally {
                    displaying = false;
                }
            });
        });
    }

    /**
     * Talks to translate.googleapis.com (gtx client). Returns null on any
     * failure - chat never breaks because of translation problems.
     */
    static String translate(String text, String targetLanguage) {
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                    + URLEncoder.encode(targetLanguage, StandardCharsets.UTF_8)
                    + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", ckpb1.common.CKPB1.NAME + "/" + ckpb1.common.CKPB1.VERSION)
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            // response: [[["translated","original",...],...],...]
            List<Object> root = MiniJson.arr(MiniJson.parse(response.body()));
            if (root.isEmpty()) {
                return null;
            }
            List<Object> segments = MiniJson.arr(root.get(0));
            StringBuilder out = new StringBuilder();
            for (Object segment : segments) {
                List<Object> pair = MiniJson.arr(segment);
                if (!pair.isEmpty()) {
                    out.append(MiniJson.str(pair.get(0), ""));
                }
            }
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
