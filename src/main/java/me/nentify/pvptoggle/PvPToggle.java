package me.nentify.pvptoggle;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.UUID;

@Plugin(id = "pvptoggle", name = "PvPToggle", version = "1.0.0")
public class PvPToggle {
    @Inject
    private Logger logger;

    // true = pvp is on, false = pvp is off
    private HashMap<UUID, Boolean> pvp = new HashMap<UUID, Boolean>();

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Hello from PvPToggle");

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
        pvp.put(uuid, false); // PvP off by default
    }

    @Listener
    public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
        UUID uuid = event.getTargetEntity().getUniqueId();
        pvp.remove(uuid);
    }
}
