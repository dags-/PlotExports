package me.dags.plotsweb;

import me.dags.plotsweb.service.DataStore;
import org.webbitserver.*;
import org.webbitserver.handler.AbstractResourceHandler;
import org.webbitserver.rest.Rest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * @author dags <dags@dags.me>
 */
class WebServlet {

    private final WebServer server;
    private final Template download;
    private final Template notfound;

    private boolean running = false;

    WebServlet(Config config, WebLinkManager linkManager, Path configDir) throws IOException {
        this.download = Template.parse(configDir.resolve("download.html"));
        this.notfound = Template.parse(configDir.resolve("notfound.html"));
        this.server = WebServers.createWebServer(config.getPort());

        Rest rest = new Rest(server);

        rest.GET("/exports/file/{shortlink}", handlePathAlias("shortlink", linkManager::getStore));

        rest.GET("/exports/{shortlink}", ((request, response, control) -> {
            String shortlink = Rest.stringParam(request, "shortlink");
            Optional<DataStore> store = linkManager.getStore(shortlink);

            if (store.isPresent()) {
                DataStore.Details details = store.get().getDetails();
                String name = details.getName();
                String date = details.getDate();
                String size = String.format("%.2fKb", details.getLength() / 1024F);
                String href = "/exports/file/" + shortlink;

                String html = download.with("file.name", name)
                        .with("file.date", date)
                        .with("file.size", size)
                        .with("file.href", href)
                        .apply();

                response.header("Content-Type", "text/html").content(html).end();
            } else {
                String html = notfound.with("link", shortlink).apply();
                response.header("Content-Type", "text/html").content(html).end();
            }
        }));
    }

    boolean isRunning() {
        return running;
    }

    void start() {
        try {
            server.start();
            running = true;
        } catch (Exception e) {
            running = false;
            e.printStackTrace();
        }
    }

    void stop() {
        server.stop();
    }

    private AliasHandler handlePathAlias(String key, Function<String, Optional<DataStore>> function) {
        return new AliasHandler(key, function);
    }

    private class AliasHandler extends AbstractResourceHandler {

        private final Function<String, Optional<DataStore>> adapter;
        private final String key;

        private AliasHandler(String key, Function<String, Optional<DataStore>> adapter) {
            super(Executors.newFixedThreadPool(4));
            this.adapter = adapter;
            this.key = key;
        }

        @Override
        public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control) throws Exception {
            String link = Rest.stringParam(request, key);
            Optional<DataStore> lookup = adapter.apply(link);
            if (lookup.isPresent()) {
                DataStore store = lookup.get();
                response.header("Content-Type", "application/octet-stream");
                response.header("Content-Disposition", String.format("attachment; filename=\"%s\"", store.getDetails().getName()));
                super.ioThread.execute(new PathIOWorker(store, request, response, control));
            } else {
                response.header("Content-Type", "text/html").content(notfound.with("shortlink", link).apply()).end();
            }
        }

        @Override
        protected AbstractResourceHandler.IOWorker createIOWorker(HttpRequest request, HttpResponse response, HttpControl control) {
            throw new UnsupportedOperationException("This should not get called!");
        }

        private class PathIOWorker extends AbstractResourceHandler.IOWorker {

            private final DataStore store;

            private PathIOWorker(DataStore store, HttpRequest request, HttpResponse response, HttpControl control) {
                super(store.getPath(), request, response, control);
                this.store = store;
            }

            @Override
            protected boolean exists() throws IOException {
                return store.exists();
            }

            @Override
            protected boolean isDirectory() throws IOException {
                return false;
            }

            @Override
            protected byte[] fileBytes() throws IOException {
                return store.getData();
            }

            @Override
            protected byte[] welcomeBytes() throws IOException {
                return new byte[0];
            }

            @Override
            protected byte[] directoryListingBytes() throws IOException {
                return new byte[0];
            }
        }
    }
}
