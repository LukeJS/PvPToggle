package me.nentify.pvptoggle;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

@Plugin(id = "pvptoggle", name = "PvPToggle", version = "1.0.0")
public class PvPToggle {
    @Inject
    private Logger logger;

    // true = pvp is on, false = pvp is off
    private boolean defaultPvp;
    private HashMap<UUID, Boolean> pvp = new HashMap<UUID, Boolean>();

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path config;

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        logger.info("Enabling PvPToggle");

        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(config).build();
        CommentedConfigurationNode rootNode = loader.createEmptyNode(ConfigurationOptions.defaults());

        if (!Files.exists(config)) {
            try {
                Files.createFile(config);
                rootNode.getNode("default-pvp").setValue(true).setComment("true = PvP is on by default, false = PvP is off by default");
                loader.save(rootNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        defaultPvp = rootNode.getNode("default-pvp").getBoolean();

        CommandSpec pvpToggleCommandSpec = CommandSpec.builder()
                .description(Text.of("PvP toggle command"))
                .permission("pvptoggle.use")
                .executor((source, context) -> {
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        UUID uuid = player.getUniqueId();

                        boolean newValue = !pvp.get(uuid);

                        pvp.replace(uuid, newValue);
                        player.sendMessage(Text.of("PvP " + (newValue ? "enabled" : "disabled")));
                    } else {
                        source.sendMessage(Text.of("You must be a player to use this command"));
                    }

                    return CommandResult.success();
                })
                .build();

        Sponge.getCommandManager().register(this, pvpToggleCommandSpec, "pvp");

        logger.info("PvPToggle enabled");
    }

    @Listener
    public void onDamageEntity(DamageEntityEvent event) {
        Player target;

        if (event.getTargetEntity() instanceof Player)
            target = (Player) event.getTargetEntity();
        else
            return;

        Player attacker;

        if (event.getCause().root() instanceof EntityDamageSource) {
            EntityDamageSource source = (EntityDamageSource) event.getCause().root();
            if (source.getSource() instanceof Player)
                attacker = (Player) source.getSource();
            else
                return;
        } else
            return;

        if (!pvp.get(attacker.getUniqueId())) {
            attacker.sendMessage(Text.of("You have PvP disabled, use /pvp to enable it"));
            event.setCancelled(true);
            return;
        }

        if (!pvp.get(target.getUniqueId())) {
            attacker.sendMessage(Text.of("Your target has PvP disabled"));
            event.setCancelled(true);
            return;
        }
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join event) {
        UUID uuid = event.getTargetEntity().getUniqueId();
        pvp.put(uuid, defaultPvp);
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        UUID uuid = event.getTargetEntity().getUniqueId();
        pvp.remove(uuid);
    }
}
