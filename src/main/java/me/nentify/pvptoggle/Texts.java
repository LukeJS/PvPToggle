package me.nentify.pvptoggle;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

public class Texts {
    public static Text toggleText = Text.builder("[Toggle]")
            .color(TextColors.YELLOW)
            .onHover(TextActions.showText(Text.of("Toggle your PvP status")))
            .onClick(TextActions.runCommand("/pvp"))
            .build();
}
