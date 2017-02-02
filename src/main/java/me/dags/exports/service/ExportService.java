package me.dags.exports.service;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author dags <dags@dags.me>
 */
public interface ExportService {

    Optional<URL> getExportURL(Path path);
}
