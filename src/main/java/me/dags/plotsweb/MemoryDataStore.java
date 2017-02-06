package me.dags.plotsweb;

import me.dags.plotsweb.service.DataStore;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author dags <dags@dags.me>
 */
class MemoryDataStore implements DataStore {

    private static final DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy hh:mm:ss");

    private final String name;
    private final String date;
    private final byte[] data;
    private final Details details;

    MemoryDataStore(String name, byte[] data) {
        this.name = name;
        this.date = dateFormat.format(new Date());
        this.data = data;
        this.details = new Details();
    }

    @Override
    public boolean exists() throws IOException {
        return data != null && data.length > 0;
    }

    @Override
    public String getPath() {
        return name;
    }

    @Override
    public Details getDetails() throws IOException {
        return details;
    }

    @Override
    public byte[] getData() throws IOException {
        return data;
    }

    @Override
    public void delete() throws IOException {}

    @Override
    public boolean equals(Object o) {
        return o != null && DataStore.class.isInstance(o) && o.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private class Details implements DataStore.Details {
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDate() {
            return date;
        }

        @Override
        public long getLength() {
            return data.length;
        }
    }
}
