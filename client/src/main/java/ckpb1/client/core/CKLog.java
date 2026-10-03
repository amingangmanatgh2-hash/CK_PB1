package ckpb1.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Central CK_PB1 client logger. */
public final class CKLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("CK_PB1");

    private CKLog() {
    }

    public static void info(String message) {
        LOGGER.info("[CK_PB1] {}", message);
    }

    public static void warn(String message) {
        LOGGER.warn("[CK_PB1] {}", message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.error("[CK_PB1] {}", message, t);
    }
}
