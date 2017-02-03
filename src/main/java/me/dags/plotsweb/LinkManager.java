package me.dags.plotsweb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author dags <dags@dags.me>
 */
final class LinkManager {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Cache<String, Path> codeToObject;
    private final Cache<Path, String> objectToCode;

    LinkManager(Config config) {
        this.codeToObject = CacheBuilder.newBuilder()
                .expireAfterWrite(config.getExpiryTimeSecs(), TimeUnit.SECONDS)
                .removalListener(removeListener())
                .build();
        this.objectToCode = CacheBuilder.newBuilder()
                .expireAfterWrite(config.getExpiryTimeSecs(), TimeUnit.SECONDS)
                .build();
    }

    String getShortlink(Path path) {
        String code = objectToCode.getIfPresent(path);
        if (code == null) {
            code = getCode();
            if (codeToObject.getIfPresent(code) != null) {
                return getShortlink(path);
            }
            objectToCode.put(path, code);
            codeToObject.put(code, path);
        }
        return code;
    }

    Optional<Path> getPath(String shortLink) {
        return shortLink != null ? Optional.ofNullable(codeToObject.getIfPresent(shortLink)) : Optional.empty();
    }

    private static String getCode() {
        byte[] data = getRandom(4);
        String str = Base64.getEncoder().encodeToString(data);
        String code = str.indexOf('=') > -1 ? str.substring(0, str.indexOf('=')) : str;
        return code.replaceAll("[^A-Za-z0-9 -]", "" + RANDOM.nextInt(9));
    }

    private static byte[] getRandom(int size) {
        byte[] buffer = new byte[size];
        RANDOM.nextBytes(buffer);
        return buffer;
    }

    private RemovalListener<String, Path> removeListener() {
        return notification -> {
            Path path = notification.getValue();
            if (path != null) {
                codeToObject.invalidate(path);

                if (Files.exists(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }
}
