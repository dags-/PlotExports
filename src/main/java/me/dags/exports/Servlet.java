package me.dags.exports;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import me.dags.exports.template.Template;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
class Servlet {

    private final Undertow server;
    private final Config config;
    private final LinkManager linkManager;
    private final Template download;
    private final Template expired;

    Servlet(Config config, LinkManager linkManager, Path configDir) throws IOException {
        Path download = getOrExtract(configDir, "download.html");
        Path expired = getOrExtract(configDir, "expired.html");
        this.config = config;
        this.linkManager = linkManager;
        this.download = Template.parse(download);
        this.expired = Template.parse(expired);
        this.server = Undertow.builder()
                .addHttpListener(config.getPort(), "localhost")
                .setHandler(Handlers.pathTemplate(true)
                        .add("/exports/{shortlink}", servePage())
                        .add("/exports/file/{shortlink}", serveFile())
                ).build();

    }

    void start() {
        server.start();
    }

    void stop() {
        server.stop();
    }

    private HttpHandler servePage() {
        return exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");

            String shortlink = exchange.getPathParameters().get("shortlink").getFirst();
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
        };
    }

    private HttpHandler serveFile() {
        return exchange -> {
            String shortlink = exchange.getPathParameters().get("shortlink").getFirst();
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
                    exchange.getResponseSender().transferFrom(in, IGNORE_ERR);
                }
            } else {
                String html = expired.with("link", shortlink).apply();
                exchange.getResponseSender().send(html);
            }
        };
    }

    private static Path getOrExtract(Path dir, String resource) throws IOException {
        Path path = dir.resolve(resource);
        if (Files.exists(path)) {
            return path;
        }

        Files.createDirectories(dir);
        Files.createFile(path);
        try (InputStream inputStream = Servlet.class.getResourceAsStream("/" + resource)) {
            try (FileChannel out = FileChannel.open(path); ReadableByteChannel in = Channels.newChannel(inputStream)) {
                out.transferFrom(in, 0L, Long.MAX_VALUE);
                return path;
            }
        }
    }

    private static final IoCallback IGNORE_ERR = new IoCallback() {
        @Override
        public void onComplete(HttpServerExchange exchange, Sender sender) {
        }

        @Override
        public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
        }
    };
}
