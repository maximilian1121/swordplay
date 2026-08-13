package ca.maximilian.swordfight.Game;

import ca.maximilian.swordfight.Util;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EquipmentSlotGroup;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.Random;

import static ca.maximilian.swordfight.Util.calculateLookAt;

public class GameConstants {
    public static final BossBar NOT_ENOUGH_PLAYERS_BOSSBAR = BossBar.bossBar(
            Component.text("PLACEHOLDER!").color(NamedTextColor.RED),
            1.0F,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
    );

    public static final BossBar INTERMISSION_BOSSBAR = BossBar.bossBar(
            Component.text("PLACEHOLDER!").color(NamedTextColor.RED),
            1F,
            BossBar.Color.BLUE,
            BossBar.Overlay.NOTCHED_10
    );

    public static final Sound FALL_OFF_SOUND = Sound.sound(
            SoundEvent.ENTITY_PLAYER_LEVELUP,
            Sound.Source.MASTER,
            1.0F,
            1.0F
    );

    public static final Sound MATCH_START = Sound.sound(
            SoundEvent.BLOCK_NOTE_BLOCK_PLING,
            Sound.Source.MASTER,
            1.0F,
            1.0F
    );

    public static final Sound CLOCK_TICK_SOUND = Sound.sound(
            SoundEvent.BLOCK_NOTE_BLOCK_HAT,
            Sound.Source.MASTER,
            1.0F,
            1.0F
    );

    public static final Sound BLOCK_SOUND = Sound.sound(
            SoundEvent.BLOCK_NOTE_BLOCK_BASS,
            Sound.Source.MASTER,
            1.0F,
            1F
    );

    public static final Sound WIN_SOUND = Sound.sound(
            SoundEvent.UI_TOAST_CHALLENGE_COMPLETE,
            Sound.Source.MASTER,
            1.0F,
            1.0F
    );

    public static final ItemStack RED_BATON = ItemStack.builder(Material.COPPER_SWORD)
            .customName(Component.text("Red Baton"))
            .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(
                    List.of(new AttributeList.Modifier(
                            Attribute.ATTACK_SPEED,
                            new AttributeModifier(
                                    Key.key("minecraft:attack_speed"),
                                    0,
                                    AttributeOperation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.MAIN_HAND
                    ))
            ))
            .build();

    public static final ItemStack BLUE_BATON = ItemStack.builder(Material.COPPER_SWORD)
            .customName(Component.text("Blue Baton"))
            .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(
                    List.of(new AttributeList.Modifier(
                            Attribute.ATTACK_SPEED,
                            new AttributeModifier(
                                    Key.key("minecraft:attack_speed"),
                                    0,
                                    AttributeOperation.ADD_VALUE
                            ),
                            EquipmentSlotGroup.MAIN_HAND
                    ))
            ))
            .build();

    public static final int INTERMISSION_TIME = 100;
    public static final int GET_READY_TIME = 60;

    public static final Pos RED_SPAWN_POS = new Pos(-146.5, 31, 8.5);
    public static final Pos BLUE_SPAWN_POS = new Pos(-146.5, 31, 2.5);

    public static final Pos RED_SPAWN = Util.calculateLookAt(RED_SPAWN_POS, BLUE_SPAWN_POS);
    public static final Pos BLUE_SPAWN = Util.calculateLookAt(BLUE_SPAWN_POS, RED_SPAWN_POS);

    public static final Pos TALL_PODIUM_SPAWN = new Pos(new Pos(-146.5, 33, 6.5), 90, 0);
    public static final Pos SHORT_PODIUM_SPAWN = new Pos(new Pos(-146.5, 33, 4.5), 90, 0);

    public static final Pos CENTRE_POINT = new Pos(-146.5, 31, 5.5);
    public static final Pos SPAWN_POINT = calculateLookAt(new Pos(-176.5, 38, 5.5), CENTRE_POINT);

    public static final Random RANDOM = new Random();
}
