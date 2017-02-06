package me.dags.plotsweb;

import me.dags.plotsweb.service.DataStore;

import java.io.IOException;

/**
 * @author dags <dags@dags.me>
 */
class LookupDataStore implements DataStore {

    private final Object lookup;

    LookupDataStore(Object lookup) {
        this.lookup = lookup;
    }

    @Override
    public String getPath() {
        return lookup.toString();
    }

    @Override
    public Details getDetails() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getData() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && DataStore.class.isInstance(o) && o.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        return lookup.hashCode();
    }
}
