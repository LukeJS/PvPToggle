package me.nentify.pvptoggle;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.config.DefaultConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;

    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private CommentedConfigurationNode config;

    public boolean defaultPvp;
    public int cooldown;

    public Config() {
        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(configPath).build();
        CommentedConfigurationNode rootNode = loader.createEmptyNode(ConfigurationOptions.defaults());

        if (!Files.exists(configPath)) {
            try {
                Files.createFile(configPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        check(config.getNode("default-pvp"), true, "true = PvP is on by default, false = PvP is off by default");
        check(config.getNode("cooldown"), 10, "Cooldown between toggling your PvP status in seconds");

        try {
            loader.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        defaultPvp = config.getNode("default-pvp").getBoolean();
        cooldown = config.getNode("cooldown").getInt();
    }

    public void check(CommentedConfigurationNode node, Object defaultValue, String comment) {
        if (node.isVirtual())
            config.setValue(defaultValue).setComment(comment);
    }
}
