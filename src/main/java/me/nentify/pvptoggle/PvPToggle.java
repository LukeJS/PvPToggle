package me.nentify.pvptoggle;

import com.google.inject.Inject;
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
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

@Plugin(id = "pvptoggle", name = "PvPToggle", version = "1.1.0")
public class PvPToggle {
    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;

    private HashMap<UUID, Boolean> pvp = new HashMap<>();
    private HashMap<UUID, Long> cooldowns = new HashMap<>();

    private Config config;

    private static Text toggleText = Text.builder("[Toggle]")
            .color(TextColors.YELLOW)
            .onHover(TextActions.showText(Text.of("Toggle your PvP status")))
            .onClick(TextActions.runCommand("/pvp"))
            .build();

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        logger.info("Enabling PvPToggle");

        config = new Config(configPath);

        CommandSpec pvpToggleCommandSpec = CommandSpec.builder()
                .description(Text.of("PvP toggle command"))
                .permission("pvptoggle.use")
                .executor((source, context) -> {
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        UUID uuid = player.getUniqueId();

                        long time = System.currentTimeMillis();

                        if (cooldowns.containsKey(uuid)) {
                            if (cooldowns.get(uuid) > time - (config.cooldown * 1000)) {
                                source.sendMessage(Text.of(TextColors.RED, "You must wait " + (config.cooldown - ((time - cooldowns.get(uuid)) / 1000)) + " seconds before toggling PvP again"));
                                return CommandResult.success();
                            }

                            cooldowns.replace(uuid, time);
                        } else {
                            cooldowns.put(uuid, time);
                        }

                        boolean newValue = !pvp.get(uuid);

                        pvp.replace(uuid, newValue);

                        Text text;

                        if (newValue) {
                            text = Text.builder("PvP enabled ").color(TextColors.DARK_RED)
                                    .append(toggleText)
                                    .build();
                        } else {
                            text = Text.builder("PvP disabled ").color(TextColors.DARK_GREEN)
                                    .append(toggleText)
                                    .build();
                        }

                        player.sendMessage(text);
                    } else {
                        source.sendMessage(Text.of(TextColors.RED, "You must be a player to use this command"));
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
            attacker.sendMessage(Text.builder("You have PvP disabled ").color(TextColors.RED).append(toggleText).build());
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
