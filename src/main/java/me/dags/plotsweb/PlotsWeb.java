package me.dags.plotsweb;

import com.google.inject.Inject;
import me.dags.plotsweb.service.ExportService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = "plotsweb", name = "PlotsWeb", version = "1.0", description = "shh")
public class PlotsWeb {

    private final Path configDir;

    @Inject
    public PlotsWeb(@ConfigDir(sharedRoot = false) Path dir) {
        this.configDir = dir;
    }

    @Listener
    public void init(GameInitializationEvent e) {
        try {
            loadAsset("download.html");
            loadAsset("notfound.html");
            PlotsWebService service = new PlotsWebService(configDir);
            Task.builder().async().execute(service::start).submit(this);
            Sponge.getServiceManager().setProvider(this, ExportService.class, service);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Listener
    public void stopping(GameStoppingServerEvent e) {
        Sponge.getServiceManager().provide(ExportService.class)
                .filter(PlotsWebService.class::isInstance)
                .map(PlotsWebService.class::cast)
                .ifPresent(PlotsWebService::stop);
    }

    private void loadAsset(String name) {
        if (Files.exists(configDir.resolve(name))) {
            return;
        }

        Sponge.getAssetManager().getAsset(this, name).ifPresent(asset -> {
            try {
                asset.copyToDirectory(configDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
