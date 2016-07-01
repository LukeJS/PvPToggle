package me.nentify.pvptoggle;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
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
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

@Plugin(id = "pvptoggle", name = "PvpToggle", version = "1.2.0")
public class PvpToggle {
    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;
    private Config config;

    public static HashMap<UUID, Boolean> pvp = new HashMap<>();

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        logger.info("Enabling PvpToggle");

        try {
            config = new Config(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        CommandSpec pvpToggleCommandSpec = CommandSpec.builder()
                .description(Text.of("PvP toggle command"))
                .permission("pvptoggle.use")
                .arguments(GenericArguments.optional(GenericArguments.string(Text.of("option"))))
                .executor(new PvpCommand(config.cooldown))
                .build();

        Sponge.getCommandManager().register(this, pvpToggleCommandSpec, "pvp");

        logger.info("PvpToggle enabled");
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

        if (attacker.hasPermission("pvptoggle.bypass"))
            return;

        if (!pvp.get(attacker.getUniqueId())) {
            attacker.sendMessage(Text.builder("You have PvP disabled ").color(TextColors.RED).append(Texts.toggleText).build());
            event.setCancelled(true);
            return;
        }

        if (!pvp.get(target.getUniqueId())) {
            attacker.sendMessage(Text.of(TextColors.RED, "Your target has PvP disabled"));
            event.setCancelled(true);
            return;
        }
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join event) {
        UUID uuid = event.getTargetEntity().getUniqueId();
        pvp.put(uuid, config.defaultPvp);
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        UUID uuid = event.getTargetEntity().getUniqueId();
        pvp.remove(uuid);
    }
}
