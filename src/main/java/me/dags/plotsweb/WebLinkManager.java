package me.dags.plotsweb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import me.dags.plotsweb.service.DataStore;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author dags <dags@dags.me>
 */
final class WebLinkManager {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Cache<String, DataStore> codeToObject;
    private final Cache<DataStore, String> objectToCode;

    WebLinkManager(Config config) {
        this.codeToObject = CacheBuilder.newBuilder()
                .expireAfterWrite(config.getExpiryTimeSecs(), TimeUnit.SECONDS)
                .removalListener(removeListener())
                .build();
        this.objectToCode = CacheBuilder.newBuilder()
                .expireAfterWrite(config.getExpiryTimeSecs(), TimeUnit.SECONDS)
                .build();
    }

    String registerDataStore(DataStore store) {
        String code = objectToCode.getIfPresent(store);
        if (code == null) {
            code = getCode();
            if (codeToObject.getIfPresent(code) != null) {
                return registerDataStore(store);
            }
            objectToCode.put(store, code);
            codeToObject.put(code, store);
        }
        return code;
    }

    Optional<DataStore> getStore(String shortLink) {
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

    private RemovalListener<String, DataStore> removeListener() {
        return notification -> {
            DataStore store = notification.getValue();
            if (store != null) {
                codeToObject.invalidate(store);
                try {
                    store.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
