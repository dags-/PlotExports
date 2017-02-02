package me.dags.plotsweb;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import me.dags.plotsweb.template.Template;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
class Servlet {

    private final Undertow server;
    private final LinkManager linkManager;
    private final Template download;
    private final Template expired;

    private boolean running = false;

    Servlet(Config config, LinkManager linkManager, Path configDir) throws IOException {
        this.linkManager = linkManager;
        this.download = Template.parse(configDir.resolve("download.html"));
        this.expired = Template.parse(configDir.resolve("expired.html"));
        this.server = Undertow.builder()
                .addHttpListener(config.getPort(), "localhost")
                .setHandler(Handlers.pathTemplate(true)
                        .add("/exports/{shortlink}", SERVE_PAGE)
                        .add("/exports/file/{shortlink}", SERVE_FILE)
                ).build();
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

    private final HttpHandler SERVE_PAGE = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws IOException {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
            String shortlink = exchange.getQueryParameters().get("shortlink").getFirst();
            Optional<Path> path = linkManager.getPath(shortlink);

            if (path.isPresent()) {
                BasicFileAttributes stat = Files.readAttributes(path.get(), BasicFileAttributes.class);
                String name = path.get().getFileName().toString();
                String date = stat.creationTime().toString();
                String size = String.format("%.2fKb", stat.size() / 1024F);
                String href = "/exports/file/" + shortlink;

                String html = download.with("file.name", name)
                        .with("file.date", date)
                        .with("file.size", size)
                        .with("file.href", href)
                        .apply();

                exchange.getResponseSender().send(html);
            } else {
                String html = expired.with("link", shortlink).apply();
                exchange.getResponseSender().send(html);
            }
        }
    };

    private final HttpHandler SERVE_FILE = new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            String shortlink = exchange.getQueryParameters().get("shortlink").getFirst();
            Optional<Path> path = linkManager.getPath(shortlink);

            if (path.isPresent()) {
                BasicFileAttributes stat = Files.readAttributes(path.get(), BasicFileAttributes.class);
                String name = path.get().getFileName().toString();
                String size = "" + stat.size();
                String disposition = String.format("attachment; filename=\"%s\"", name);

                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
                exchange.getResponseHeaders().put(Headers.CONTENT_DISPOSITION, disposition);
                exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, size);

                try (FileChannel in = FileChannel.open(path.get())) {
                    exchange.getResponseChannel().transferFrom(in, 0, Long.MAX_VALUE);
                }
            } else {
                String html = expired.with("link", shortlink).apply();
                exchange.getResponseSender().send(html);
            }
        }
    };

    private static final IoCallback IGNORE_ERR = new IoCallback() {
        @Override
        public void onComplete(HttpServerExchange exchange, Sender sender) {
        }

        @Override
        public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
        }
    };
}
