package me.dags.plotsweb;

import me.dags.data.NodeAdapter;
import me.dags.data.node.Node;
import me.dags.data.node.NodeObject;

import java.nio.file.Path;

/**
 * @author dags <dags@dags.me>
 */
class Config {

    private final Path path;
    private int port = 8127;
    private String base_url = "http://localhost:8127";
    private long expiry_time_secs = 60L * 5L;

    Config(Path path) {
        this.path = path;
        Node node = NodeAdapter.hocon().from(path);
        if (node.isPresent() && node.isNodeObject()) {
            NodeObject object = node.asNodeObject();
            this.port = object.map("port", n -> n.asNumber().intValue(), port);
            this.base_url = object.map("base_url", Node::asString, base_url);
            this.expiry_time_secs = object.map("expiry_time_secs", n -> n.asNumber().longValue(), expiry_time_secs);
        }
        checkUrl();
        save();
    }

    private void checkUrl() {
        String url = base_url.toLowerCase();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "hHttp://" + url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.base_url = url;
    }

    int getPort() {
        return port;
    }

    String getBaseUrl() {
        return base_url;
    }

    long getExpiryTimeSecs() {
        return expiry_time_secs;
    }

    private void save() {
        NodeObject object = new NodeObject();
        object.put("port", port);
        object.put("base_url", base_url);
        object.put("expiry_time_secs", expiry_time_secs);
        NodeAdapter.hocon().to(object, path);
    }
}
