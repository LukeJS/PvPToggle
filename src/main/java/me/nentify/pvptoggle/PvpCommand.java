package me.nentify.pvptoggle;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class PvpCommand implements CommandExecutor {
    private HashMap<UUID, Long> cooldowns = new HashMap<>();

    private int cooldown;

    public PvpCommand(int cooldown) {
        this.cooldown = cooldown;
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        if (source instanceof Player) {
            Player player = (Player) source;
            UUID uuid = player.getUniqueId();

            if (args.getOne("option").isPresent()) {
                String arg = args.<String>getOne("option").get();

                if (arg.equals("on")) {
                    if (PvpToggle.pvp.containsKey(uuid)) {
                        if (!PvpToggle.pvp.get(uuid)) {
                            togglePvp(player);
                        } else {
                            player.sendMessage(Text.of(TextColors.RED, "You already have PvP enabled!"));
                        }

                        return CommandResult.success();
                    }
                }

                if (arg.equals("off")) {
                    if (PvpToggle.pvp.containsKey(uuid)) {
                        if (PvpToggle.pvp.get(uuid)) {
                            togglePvp(player);
                        } else {
                            player.sendMessage(Text.of(TextColors.RED, "You already have PvP disabled!"));
                        }

                        return CommandResult.success();
                    }
                }
            }

            togglePvp(player);
        } else {
            source.sendMessage(Text.of(TextColors.RED, "You must be a player to use this command"));
        }

        return CommandResult.success();
    }

    private void togglePvp(Player player) {
        UUID uuid = player.getUniqueId();

        long time = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            if (cooldowns.get(uuid) > time - (cooldown * 1000) && !player.hasPermission("pvptoggle.nocooldown")) {
                player.sendMessage(Text.of(TextColors.RED, "You must wait " + (cooldown - ((time - cooldowns.get(uuid)) / 1000)) + " seconds before toggling PvP again"));
                return;
            }

            cooldowns.replace(uuid, time);
        } else {
            cooldowns.put(uuid, time);
        }

        boolean newValue = !PvpToggle.pvp.get(uuid);

        PvpToggle.pvp.replace(uuid, newValue);

        Text text;

        if (newValue) {
            text = Text.builder("PvP enabled ").color(TextColors.DARK_RED)
                    .append(Texts.toggleText)
                    .build();
        } else {
            text = Text.builder("PvP disabled ").color(TextColors.DARK_GREEN)
                    .append(Texts.toggleText)
                    .build();
        }

        player.sendMessage(text);
    }
}
