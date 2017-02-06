package me.dags.plotsweb;

import me.dags.plotsweb.service.DataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author dags <dags@dags.me>
 */
class FileDataStore implements DataStore {

    private final Path path;

    FileDataStore(Path path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path.toString();
    }

    @Override
    public Details getDetails() throws IOException {
        return new Details();
    }

    @Override
    public boolean exists() throws IOException {
        return Files.exists(path);
    }

    @Override
    public byte[] getData() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public void delete() throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o != null && DataStore.class.isInstance(o) && o.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    private class Details implements DataStore.Details {

        private final BasicFileAttributes stat;

        private Details() throws IOException {
            this.stat = Files.readAttributes(path, BasicFileAttributes.class);
        }

        @Override
        public String getName() {
            return path.getFileName().toString();
        }

        @Override
        public String getDate() {
            return stat.creationTime().toString();
        }

        @Override
        public long getLength() {
            return stat.size();
        }
    }
}
