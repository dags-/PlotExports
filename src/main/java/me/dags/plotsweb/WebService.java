package me.dags.plotsweb;

import me.dags.plotsweb.service.DataStore;
import me.dags.plotsweb.service.PlotsWebService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
class WebService implements PlotsWebService {

    private final Config config;
    private final WebServlet servlet;
    private final WebLinkManager linkManager;

    WebService(Path configDir) throws IOException {
        Path path = configDir.resolve("config.conf");
        this.config = new Config(path);
        this.linkManager = new WebLinkManager(config);
        this.servlet = new WebServlet(config, linkManager, configDir);
    }

    void start() {
        servlet.start();
    }

    void stop() {
        servlet.stop();
    }

    @Override
    public Optional<URL> getDataStoreURL(DataStore dataStore) {
        String link = linkManager.registerDataStore(dataStore);
        String address = String.format("%s/exports/%s", config.getBaseUrl(), link);
        try {
            return Optional.of(new URL(address));
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    @Override
    public DataStore newMemoryDataStore(String s, byte[] bytes) {
        return new MemoryDataStore(s, bytes);
    }

    @Override
    public DataStore newFileDataStore(Path path) {
        return new FileDataStore(path);
    }

    @Override
    public boolean running() {
        return servlet.isRunning();
    }
}
