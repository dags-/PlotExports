package me.dags.exports;

import me.dags.exports.template.Template;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
class Servlet {

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
    }

    void start() {
        Spark.port(config.getPort());
        Spark.get("/exports/:shortlink", this::serveDownloadPage);
        Spark.get("/exports/file/:shortlink", this::serveFile);
    }

    void stop() {
        Spark.halt();
    }

    private Object serveDownloadPage(Request request, Response response) throws Exception {
        String shortlink = request.params(":shortlink");
        Optional<Path> path = linkManager.getPath(shortlink);
        if (path.isPresent()) {
            BasicFileAttributes stat = Files.readAttributes(path.get(), BasicFileAttributes.class);
            String name = path.get().getFileName().toString();
            String date = stat.creationTime().toString();
            String size = String.format("%.2fKb", stat.size() / 1024F);
            String href = "/exports/file/" + shortlink;

            return download.with("file.name", name)
                    .with("file.date", date)
                    .with("file.size", size)
                    .with("file.href", href)
                    .apply();
        }

        return expired.with("link", shortlink).apply();
    }

    private Object serveFile(Request request, Response response) throws Exception {
        String shortlink = request.params(":shortlink");
        Optional<Path> path = linkManager.getPath(shortlink);
        if (path.isPresent()) {
            BasicFileAttributes stat = Files.readAttributes(path.get(), BasicFileAttributes.class);
            String name = path.get().getFileName().toString();
            String size = "" + stat.size();

            response.header("Content-Type", "application/octet-stream");
            response.header("Content-Disposition", String.format("attachment; filename=\"%s\"", name));
            response.header("Content-Length", size);

            try (FileChannel in = FileChannel.open(path.get())) {
                try (WritableByteChannel out = Channels.newChannel(response.raw().getOutputStream())) {
                    in.transferTo(0, Long.MAX_VALUE, out);
                }
            }
        }

        return expired.with("link", shortlink).apply();
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
}
