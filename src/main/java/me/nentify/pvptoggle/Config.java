package me.nentify.pvptoggle;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private CommentedConfigurationNode config;

    public boolean defaultPvp;
    public int cooldown;

    public Config(Path configPath) {
        loader = HoconConfigurationLoader.builder().setPath(configPath).build();

        if (!Files.exists(configPath)) {
            try {
                Files.createFile(configPath);
                config = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        check("default-pvp", true, "true = PvP is on by default, false = PvP is off by default");
        check("cooldown", 10, "Cooldown between toggling your PvP status in seconds");

        try {
            loader.save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        defaultPvp = config.getNode("default-pvp").getBoolean();
        cooldown = config.getNode("cooldown").getInt();
    }

    public void check(String node, Object defaultValue, String comment) {
        if (config.getNode(node).isVirtual())
            config.getNode(node).setValue(defaultValue).setComment(comment);
    }
}
