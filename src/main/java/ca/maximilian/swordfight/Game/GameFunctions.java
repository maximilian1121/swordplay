package ca.maximilian.swordfight.Game;

import ca.maximilian.swordfight.Server;
import ca.maximilian.swordfight.SwordFight;
import net.minestom.server.color.Color;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.scoreboard.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameFunctions {
    private static final Logger logger = LoggerFactory.getLogger(GameFunctions.class);

    public static void setSpectator(Player player) {
        Server server = SwordFight.getServer();
        if (player.getTeam() != server.getSpectatorsTeam()) {
            logger.debug("Setting player {} as spectator", player.getUsername());
            player.getInventory().clear();
            player.setTeam(server.getSpectatorsTeam());
            healCrippledPlayer(player);
            player.teleport(GameConstants.SPAWN_POINT);
            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1024);
        }
    }

    public static void setAllSpectator() {
        Server server = SwordFight.getServer();
        Set<Player> players = server.getDefaultInstance().getPlayers();
        logger.debug("Setting all {} players as spectators", players.size());
        for (Player player : players) {
            setSpectator(player);
        }
    }

    public static Player pickRandomSpectator() {
        Server server = SwordFight.getServer();
        List<Player> playersInTeam = server.getPlayersInTeam(server.getSpectatorsTeam());

        if (playersInTeam == null || playersInTeam.isEmpty()) {
            logger.warn("No spectators available to pick from");
            return null;
        }

        int randomIndex = GameConstants.RANDOM.nextInt(playersInTeam.size());
        Player picked = playersInTeam.get(randomIndex);
        logger.debug("Picked random spectator: {} (from {} candidates)", picked.getUsername(), playersInTeam.size());
        return picked;
    }

    private static final Set<Player> crippledPlayers = ConcurrentHashMap.newKeySet();

    public static void cripplePlayer(Player player) {
        logger.debug("Crippled player: {}", player.getUsername());
        crippledPlayers.add(player);
    }

    public static void healCrippledPlayer(Player player) {
        logger.debug("Healed player: {}", player.getUsername());
        crippledPlayers.remove(player);
    }

    public static boolean isCrippled(Player player) {
        return crippledPlayers.contains(player);
    }

    public static Team getBestOfFiveWinner(Team redTeam, int redTeamPoints, Team blueTeam, int blueTeamPoints) {
        int targetWins = 3;
        logger.trace("Checking winner - Red: {}/3, Blue: {}/3", redTeamPoints, blueTeamPoints);

        if (redTeamPoints >= targetWins) {
            logger.info("Red team wins! ({} points)", redTeamPoints);
            return redTeam;
        } else if (blueTeamPoints >= targetWins) {
            logger.info("Blue team wins! ({} points)", blueTeamPoints);
            return blueTeam;
        }

        return null;
    }

    public static void buildPodium(boolean redWon) {
        logger.info("Building podium, winner: {}", redWon ? "RED" : "BLUE");
        Block RED_BLOCk = Block.RED_CONCRETE;
        Block BLUE_BLOCK = Block.BLUE_CONCRETE;

        Block tallBlock = redWon ? RED_BLOCk : BLUE_BLOCK;
        Block shortBlock = redWon ? BLUE_BLOCK : RED_BLOCk;

        Server server = SwordFight.getServer();
        InstanceContainer defaultInstance = server.getDefaultInstance();

        defaultInstance.setBlock(-147, 31, 6, tallBlock);
        defaultInstance.setBlock(-147, 32, 6, tallBlock);

        defaultInstance.setBlock(-147, 31, 4, shortBlock);
    }

    public static void clearPodium() {
        logger.debug("Clearing podium");
        Server server = SwordFight.getServer();
        InstanceContainer defaultInstance = server.getDefaultInstance();
        defaultInstance.setBlock(-147, 31, 6, Block.AIR);
        defaultInstance.setBlock(-147, 32, 6, Block.AIR);
        defaultInstance.setBlock(-147, 31, 4, Block.AIR);
    }

    public static void spawnFirework(Instance instance, Pos position) {
        logger.trace("Spawning firework at ({}, {}, {})", String.format("%.1f", position.x()), String.format("%.1f", position.y()), String.format("%.1f", position.z()));
        Entity firework = new Entity(EntityType.FIREWORK_ROCKET);

        FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();
        int randomIndex = GameConstants.RANDOM.nextInt(shapes.length);
        FireworkExplosion.Shape shape = shapes[randomIndex];

        FireworkExplosion explosion = new FireworkExplosion(
                shape,
                List.of(
                        new Color(255, 0, 0),
                        new Color(255, 69, 0),
                        new Color(255, 165, 0),
                        new Color(255, 255, 0),
                        new Color(50, 255, 50),
                        new Color(0, 255, 127),
                        new Color(0, 255, 255),
                        new Color(0, 128, 255),
                        new Color(75, 0, 255),
                        new Color(138, 43, 226),
                        new Color(255, 0, 255),
                        new Color(255, 20, 147),
                        new Color(255, 105, 180),
                        new Color(255, 255, 255)
                ),
                List.of(
                        new Color(255, 255, 0),
                        new Color(255, 140, 0),
                        new Color(255, 0, 0),
                        new Color(255, 0, 255),
                        new Color(138, 43, 226),
                        new Color(0, 0, 255),
                        new Color(0, 255, 255),
                        new Color(0, 255, 0),
                        new Color(255, 20, 147),
                        new Color(255, 255, 255)
                ),
                true,
                true
        );

        ItemStack fireworkItem = ItemStack.builder(Material.FIREWORK_ROCKET)
                .set(DataComponents.FIREWORKS, new FireworkList(0, List.of(explosion)))
                .build();

        FireworkRocketMeta meta = (FireworkRocketMeta) firework.getEntityMeta();
        meta.setFireworkInfo(fireworkItem);
        firework.setInstance(instance, position);
        firework.triggerStatus((byte) 17);
        firework.remove();
    }
}
