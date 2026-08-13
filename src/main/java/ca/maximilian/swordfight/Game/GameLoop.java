package ca.maximilian.swordfight.Game;

import ca.maximilian.swordfight.Server;
import ca.maximilian.swordfight.SwordFight;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public class GameLoop {
    private static final Logger logger = LoggerFactory.getLogger(GameLoop.class);
    private static Server server;

    private static boolean runWinnerCheck() {
        Team winner = GameFunctions.getBestOfFiveWinner(server.getRedTeam(), GameState.Manager.getRedPoints(), server.getBlueTeam(), GameState.Manager.getBluePoints());

        if (winner != null) {
            logger.info("Winner detected: {}", winner == server.getRedTeam() ? "RED" : "BLUE");
            GameState.Manager.setWinningTeam(winner);
            GameState.Manager.setState(GameState.ENDING);
        }

        return winner != null;
    }

    private static void tick() {
        server.getDefaultInstance().setTime(7200);

        List<Player> playersInWater = server.getAllPlayers().stream()
                .filter(player -> player.getPosition().y() < 19)
                .toList();

        for (Player player : playersInWater) {
            if (!player.isDead()) {
                if (player.getTeam() == server.getRedTeam() && GameState.Manager.getState() == GameState.PLAYING) {
                    logger.info("Red team player {} fell off! Blue scores a point.", player.getUsername());
                    GameState.Manager.setBluePoints(GameState.Manager.getBluePoints() + 1);
                    GameState.Manager.getMatchOrder().add(GameState.TeamColour.BLUE);

                    Audiences.players().playSound(GameConstants.FALL_OFF_SOUND);
                    if (runWinnerCheck()) {
                        GameState.Manager.setState(GameState.ENDING);
                    } else {
                        GameState.Manager.setState(GameState.GETTING_READY);
                    }
                } else if (player.getTeam() == server.getBlueTeam() && GameState.Manager.getState() == GameState.PLAYING) {
                    logger.info("Blue team player {} fell off! Red scores a point.", player.getUsername());
                    GameState.Manager.setRedPoints(GameState.Manager.getRedPoints() + 1);
                    GameState.Manager.getMatchOrder().add(GameState.TeamColour.RED);

                    Audiences.players().playSound(GameConstants.FALL_OFF_SOUND);
                    if (runWinnerCheck()) {
                        GameState.Manager.setState(GameState.ENDING);
                    } else {
                        GameState.Manager.setState(GameState.GETTING_READY);
                    }
                } else if (player.getTeam() == server.getSpectatorsTeam()) {
                    Damage voidDamage = new Damage(DamageType.DROWN, null, null, null, 1000.0f);
                    player.damage(voidDamage);
                }
            }
        }

        if (server.getAllPlayers().size() < 2) {
            GameState.Manager.setState(GameState.NOT_ENOUGH_PLAYERS);

            GameConstants.NOT_ENOUGH_PLAYERS_BOSSBAR.name(Component.text("Need %s more player!".formatted(2 - server.getAllPlayers().size())));

            Audiences.players().showBossBar(GameConstants.NOT_ENOUGH_PLAYERS_BOSSBAR);
        } else {
            if (GameState.Manager.getState() == GameState.NOT_ENOUGH_PLAYERS) {
                logger.info("Enough players joined, starting intermission");
                GameState.Manager.setState(GameState.INTERMISSION);
            }
            Audiences.players().hideBossBar(GameConstants.NOT_ENOUGH_PLAYERS_BOSSBAR);
        }

        if (GameState.Manager.getState() == GameState.INTERMISSION) {
            GameState.Manager.setIntermissionTicksLeft(
                    Math.max(0, GameState.Manager.getIntermissionTicksLeft() - 1)
            );
            GameConstants.INTERMISSION_BOSSBAR.name(
                    Component.text("Intermission, %ss".formatted(GameState.Manager.getIntermissionTicksLeft()/20))
            );
            GameConstants.INTERMISSION_BOSSBAR.progress((float) GameState.Manager.getIntermissionTicksLeft() / GameConstants.INTERMISSION_TIME);
            Audiences.players().showBossBar(GameConstants.INTERMISSION_BOSSBAR);

            if (GameState.Manager.getIntermissionTicksLeft() <= 0) {
                logger.info("Intermission ended, transitioning to STARTING");
                GameState.Manager.setState(GameState.STARTING);
            }
        } else {
            Audiences.players().hideBossBar(GameConstants.INTERMISSION_BOSSBAR);
        }

        if (GameState.Manager.getState() == GameState.GETTING_READY) {
            GameState.Manager.setGettingReadyTicksLeft(
                    Math.max(0, GameState.Manager.getGettingReadyTicksLeft() - 1)
            );
            int lastSeconds = GameState.Manager.getGettingReadySecondsLeft();
            GameState.Manager.setGettingReadySecondsLeft(GameState.Manager.getGettingReadyTicksLeft() / 20);
            if (lastSeconds != GameState.Manager.getGettingReadySecondsLeft()) {
                Audiences.players().sendTitlePart(TitlePart.SUBTITLE, Component.text("%ss".formatted(GameState.Manager.getGettingReadySecondsLeft()+1)));
                Audiences.players().playSound(GameConstants.CLOCK_TICK_SOUND);
            }

            if (GameState.Manager.getGettingReadyTicksLeft() <= 0) {
                logger.info("Get ready phase ended, transitioning to PLAYING");
                GameState.Manager.setState(GameState.PLAYING);
            }
        }

        if (GameState.Manager.getState() == GameState.PLAYING || GameState.Manager.getState() == GameState.GETTING_READY || GameState.Manager.getState() == GameState.ENDING) {
            var actionBarBuilder = Component.text();
            var matchOrder = GameState.Manager.getMatchOrder();

            for (int i = 0; i < 5; i++) {
                NamedTextColor textColor;

                if (i < matchOrder.size()) {
                    textColor = (matchOrder.get(i) == GameState.TeamColour.RED)
                            ? NamedTextColor.RED
                            : NamedTextColor.BLUE;
                } else {
                    textColor = NamedTextColor.WHITE;
                }

                actionBarBuilder.append(Component.text("█").color(textColor));
            }

            Audiences.players().sendActionBar(actionBarBuilder.build());

            Player redTeamPlayer = GameState.Manager.getRedTeamPlayer();
            Player blueTeamPlayer = GameState.Manager.getBlueTeamPlayer();

            if (redTeamPlayer != null && blueTeamPlayer != null) {
                redTeamPlayer.setHeldItemSlot((byte) 0);
                blueTeamPlayer.setHeldItemSlot((byte) 0);

                redTeamPlayer.setItemInMainHand(GameConstants.RED_BATON);
                blueTeamPlayer.setItemInMainHand(GameConstants.BLUE_BATON);

                redTeamPlayer.setItemInOffHand(ItemStack.of(Material.SHIELD, 1));
                blueTeamPlayer.setItemInOffHand(ItemStack.of(Material.SHIELD, 1));
            }
        }
    }

    private static void tick_fireworks() {
        if (GameState.Manager.getState() == GameState.ENDING) {
            if (GameConstants.RANDOM.nextInt(30) != 0) return;
            logger.trace("Spawning firework during ENDING phase");

            int rad = 7;
            int height = 10;
            double angle = GameConstants.RANDOM.nextDouble() * 2 * Math.PI;
            double r = rad * Math.sqrt(GameConstants.RANDOM.nextDouble());
            double x = GameConstants.CENTRE_POINT.x() + r * Math.cos(angle);
            double z = GameConstants.CENTRE_POINT.z() + r * Math.sin(angle);
            double y = GameConstants.CENTRE_POINT.y() + GameConstants.RANDOM.nextDouble() * height;
            Pos selectedPos = new Pos(x, y, z);
            GameFunctions.spawnFirework(server.getDefaultInstance(), selectedPos);
        }
    }

    public static void run() {
        logger.info("Initializing game loop...");
        server = SwordFight.getServer();
        SchedulerManager scheduler = server.getSchedulerManager();

        logger.debug("Scheduling tick task (every 1 tick)");
        scheduler.buildTask(GameLoop::tick)
                .repeat(TaskSchedule.tick(1))
                .schedule();

        logger.debug("Scheduling firework tick task (every 1 tick)");
        scheduler.buildTask(GameLoop::tick_fireworks)
                .repeat(TaskSchedule.tick(1))
                .schedule();

        logger.debug("Registering state change listeners...");

        GameState.Manager.onStateChange(GameState.NOT_ENOUGH_PLAYERS, (oldState, newState) -> {
            logger.info("State -> NOT_ENOUGH_PLAYERS: setting all players as spectators, clearing podium");
            GameFunctions.setAllSpectator();
            GameFunctions.clearPodium();
        });

        GameState.Manager.onStateChange(GameState.INTERMISSION, (oldState, newState) -> {
            logger.info("State -> INTERMISSION: resetting intermission timer, setting all spectators");
            GameFunctions.setAllSpectator();
            GameFunctions.clearPodium();
            GameState.Manager.setIntermissionTicksLeft(GameConstants.INTERMISSION_TIME);
        });

        GameState.Manager.onStateChange(GameState.STARTING, (oldState, newState) -> {
            logger.info("State -> STARTING: selecting players for duel");
            GameState.Manager.setRedTeamPlayer(GameFunctions.pickRandomSpectator());
            Player redTeamPlayer = GameState.Manager.getRedTeamPlayer();
            if (redTeamPlayer != null) {
                logger.debug("Red team player selected: {}", redTeamPlayer.getUsername());
                redTeamPlayer.setTeam(server.getRedTeam());
                GameState.Manager.setRedPoints(0);
                redTeamPlayer.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(0.5);
            }
            GameState.Manager.setBlueTeamPlayer(GameFunctions.pickRandomSpectator());
            Player blueTeamPlayer = GameState.Manager.getBlueTeamPlayer();
            if (blueTeamPlayer != null) {
                logger.debug("Blue team player selected: {}", blueTeamPlayer.getUsername());
                blueTeamPlayer.setTeam(server.getBlueTeam());
                GameState.Manager.setBluePoints(0);
                blueTeamPlayer.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(0.5);
            }

            if (redTeamPlayer == null && blueTeamPlayer == null) {
                logger.error("No spectators available to pick for duel! Falling back to INTERMISSION");
                GameState.Manager.setState(GameState.INTERMISSION);
                Audiences.players().showTitle(Title.title(
                        Component.text("Intermission!").color(NamedTextColor.RED),
                        Component.text("An error occurred when selecting players for the duel!").color(NamedTextColor.RED)
                ));
            }

            GameState.Manager.getMatchOrder().clear();
            GameState.Manager.setWinningTeam(null);

            server.getSchedulerManager().buildTask(() -> {
                GameState.Manager.setState(GameState.GETTING_READY);
            }).delay(TaskSchedule.tick(1)).schedule();
        });

        GameState.Manager.onStateChange(GameState.GETTING_READY, (oldState, newState) -> {
            logger.info("State -> GETTING_READY: teleporting players to spawn positions");
            if (runWinnerCheck()) {return;}
            Player redTeamPlayer = GameState.Manager.getRedTeamPlayer();
            Player blueTeamPlayer = GameState.Manager.getBlueTeamPlayer();
            redTeamPlayer.teleport(GameConstants.RED_SPAWN);
            blueTeamPlayer.teleport(GameConstants.BLUE_SPAWN);

            server.getSchedulerManager().buildTask(() -> {
                GameFunctions.cripplePlayer(redTeamPlayer);
                GameFunctions.cripplePlayer(blueTeamPlayer);
            }).delay(TaskSchedule.tick(1)).schedule();

            GameState.Manager.setGettingReadyTicksLeft(GameConstants.GET_READY_TIME);
            GameState.Manager.setGettingReadySecondsLeft(GameConstants.GET_READY_TIME/20);

            Title.Times times = Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofSeconds(5),
                    Duration.ofMillis(500)
            );

            Audiences.players().sendTitlePart(TitlePart.TIMES, times);
            Audiences.players().sendTitlePart(TitlePart.TITLE, Component.text("Get Ready!"));
        });

        GameState.Manager.onStateChange(GameState.PLAYING, (oldState, newState) -> {
            logger.info("State -> PLAYING: match started!");
            Player redTeamPlayer = GameState.Manager.getRedTeamPlayer();
            Player blueTeamPlayer = GameState.Manager.getBlueTeamPlayer();
            GameFunctions.healCrippledPlayer(redTeamPlayer);
            GameFunctions.healCrippledPlayer(blueTeamPlayer);

            Audiences.players().sendTitlePart(TitlePart.TIMES, Title.Times.times(
                    Duration.ofMillis(100),
                    Duration.ofSeconds(1),
                    Duration.ofMillis(100)
            ));
            Audiences.players().sendTitlePart(TitlePart.TITLE, Component.text("Start!"));
            Audiences.players().sendTitlePart(TitlePart.SUBTITLE, Component.text("Knock each other off!"));

            Audiences.players().playSound(GameConstants.MATCH_START);
        });

        GameState.Manager.onStateChange(GameState.ENDING, (oldState, newState) -> {
            logger.info("State -> ENDING: match over, winner: {}", GameState.Manager.getWinningTeam() == server.getRedTeam() ? "RED" : "BLUE");
            server.getSchedulerManager().buildTask(() -> {
                GameState.Manager.setState(GameState.INTERMISSION);
            }).delay(TaskSchedule.seconds(10)).schedule();

            GameFunctions.buildPodium(GameState.Manager.getWinningTeam() == server.getRedTeam());

            Player redTeamPlayer = GameState.Manager.getRedTeamPlayer();
            Player blueTeamPlayer = GameState.Manager.getBlueTeamPlayer();
            boolean redWon = GameState.Manager.getWinningTeam() == server.getRedTeam();

            redTeamPlayer.teleport(redWon ? GameConstants.TALL_PODIUM_SPAWN : GameConstants.SHORT_PODIUM_SPAWN);
            blueTeamPlayer.teleport(!redWon ? GameConstants.TALL_PODIUM_SPAWN : GameConstants.SHORT_PODIUM_SPAWN);

            Audiences.players().sendTitlePart(TitlePart.TITLE, Component.text("%s won!".formatted(redWon ? redTeamPlayer.getUsername() : blueTeamPlayer.getUsername())));
            Audiences.players().sendTitlePart(TitlePart.SUBTITLE, Component.text("Intermission starting!"));

            Audiences.players().playSound(GameConstants.WIN_SOUND);
        });
    }
}
