package me.dags.plotsweb;

import me.dags.plotsweb.service.ExportService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
class PlotsWebService implements ExportService {

    private final Config config;
    private final Servlet servlet;
    private final LinkManager linkManager;

    PlotsWebService(Path configDir) throws IOException {
        Path path = configDir.resolve("config.conf");
        this.config = new Config(path);
        this.linkManager = new LinkManager(config);
        this.servlet = new Servlet(config, linkManager, configDir);
    }

    void start() {
        servlet.start();
    }

    void stop() {
        servlet.stop();
    }

    @Override
    public Optional<URL> getExportURL(Path path) {
        if (Files.exists(path)) {
            String shortLink = linkManager.getShortlink(path);
            String address = String.format("%s/exports/%s", config.getBaseUrl(), shortLink);
            try {
                return Optional.of(new URL(address));
            } catch (MalformedURLException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean running() {
        return servlet.isRunning();
    }
}
