package me.dags.exports;

import com.google.inject.Inject;
import me.dags.exports.service.ExportService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author dags <dags@dags.me>
 */
@Plugin(id = "plotexports", name = "PlotExports", version = "1.0", dependencies = @Dependency(id = "plots"), description = "shh")
public class PlotExports {

    private final PlotExportsService core;

    @Inject
    public PlotExports(@ConfigDir(sharedRoot = false) Path dir) {
        PlotExportsService core;

        try {
            core = new PlotExportsService(dir);
        } catch (IOException e) {
            core = null;
        }

        this.core = core;
    }

    @Listener
    public void init(GameInitializationEvent e) {
        if (core != null) {
            core.start();
            Sponge.getServiceManager().setProvider(this, ExportService.class, core);
        }
    }

    @Listener
    public void stopping(GameStoppingServerEvent e) {
        if (core != null) {
            core.stop();
        }
    }
}
