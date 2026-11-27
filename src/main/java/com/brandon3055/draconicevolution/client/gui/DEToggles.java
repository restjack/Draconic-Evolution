package com.brandon3055.draconicevolution.client.gui;

import com.brandon3055.brandonscore.client.gui.GuiToolkit;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiButton;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiElement;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiItemStack;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiRectangle;
import com.brandon3055.brandonscore.client.gui.modular.elements.GuiTexture;
import com.brandon3055.brandonscore.client.gui.modular.lib.Constraints;
import com.brandon3055.draconicevolution.client.DEGuiTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.literal;
import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.Constraint.relative;
import static com.brandon3055.brandonscore.client.gui.modular.lib.geometry.GeoParam.*;

/**
 * The fork's shared on/off toggle look (restjack fork, unofficial-1.36; was in FusionConductorGui): a 16 px button,
 * green with its icon when on, dimmed with a red corner when off, tooltip = name, On/Off, "&lt;key&gt;.info". Used by the
 * Celestial Manipulator and Fusion Conductor screens; add-ons use it for the same look.
 */
public final class DEToggles {
    private static final GuiToolkit TOOLKIT = new GuiToolkit("gui.draconicevolution");

    private DEToggles() {}

    /** Toggle with an item icon. {@code key} is a full lang key; its ".info" line is the tooltip's last line. */
    public static GuiButton toggle(GuiElement<?> parent, ItemStack icon, Runnable press, Supplier<Boolean> state, String key) {
        return toggle(parent, b -> Constraints.bind(new GuiItemStack(b, icon).enableStackToolTip(false), b, 1), press, state, key);
    }

    /** Toggle with a 16 px gui-atlas icon (e.g. "celestial_manipulator/quiet"). */
    public static GuiButton toggle(GuiElement<?> parent, String texture, Runnable press, Supplier<Boolean> state, String key) {
        return toggle(parent, b -> Constraints.bind(new GuiTexture(b, DEGuiTextures.getter(texture)), b, 0), press, state, key);
    }

    /** "Name: On" (green) / "Name: Off" (red), for the status-box line after a click. */
    public static Component onOff(String key, boolean on) {
        return Component.translatable(key).append(": ").append(Component.translatable(on ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static GuiButton toggle(GuiElement<?> parent, Consumer<GuiButton> icon, Runnable press, Supplier<Boolean> state, String key) {
        GuiButton button = TOOLKIT.createFlat3DButton(parent, null)
                .setTooltip(() -> List.of(Component.translatable(key),
                        Component.translatable(state.get() ? "gui.draconicevolution.toggle.on" : "gui.draconicevolution.toggle.off")
                                .withStyle(state.get() ? ChatFormatting.GREEN : ChatFormatting.RED),
                        Component.translatable(key + ".info").withStyle(ChatFormatting.GRAY)))
                .onPress(press)
                .setToggleMode(state);
        Constraints.size(button, 16, 16);
        new GuiRectangle(button).fill(0xB040C040).setEnabled(state)
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        icon.accept(button);
        new GuiRectangle(button).fill(0x90202020).setEnabled(() -> !state.get())
                .constrain(LEFT, relative(button.get(LEFT), 1)).constrain(TOP, relative(button.get(TOP), 1))
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1));
        new GuiRectangle(button).fill(0xFFD03030).setEnabled(() -> !state.get())
                .constrain(RIGHT, relative(button.get(RIGHT), -1)).constrain(BOTTOM, relative(button.get(BOTTOM), -1))
                .constrain(WIDTH, literal(4)).constrain(HEIGHT, literal(4));
        return button;
    }
}
