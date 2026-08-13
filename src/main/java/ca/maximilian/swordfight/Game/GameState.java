package ca.maximilian.swordfight.Game;

import lombok.Getter;
import lombok.Setter;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public enum GameState {
    STARTING,
    GETTING_READY,
    PLAYING,
    ENDING,
    INTERMISSION,
    NOT_ENOUGH_PLAYERS;

    public enum TeamColour {
        RED, BLUE
    }

    public static class Manager {
        private static final Logger logger = LoggerFactory.getLogger(Manager.class);

        @Getter
        private static GameState state = GameState.NOT_ENOUGH_PLAYERS;
        @Getter
        @Setter
        private static int intermissionTicksLeft = GameConstants.INTERMISSION_TIME;
        @Getter
        @Setter
        private static int gettingReadyTicksLeft = GameConstants.GET_READY_TIME;
        @Getter
        @Setter
        private static int gettingReadySecondsLeft = GameConstants.GET_READY_TIME/20;
        @Getter
        @Setter
        private static int redPoints = 0;
        @Getter
        @Setter
        private static int bluePoints = 0;

        @Getter
        @Setter
        private static Player redTeamPlayer;
        @Getter
        @Setter
        private static int redTeamPlayerLastAttack;
        @Getter
        @Setter
        private static Player blueTeamPlayer;
        @Getter
        @Setter
        private static int blueTeamPlayerLastAttack;

        @Getter
        private static final List<TeamColour> matchOrder = new CopyOnWriteArrayList<>();
        @Getter
        @Setter
        private static @Nullable Team winningTeam;

        private static final List<BiConsumer<GameState, GameState>> listeners = new ArrayList<>();

        public static void setState(GameState newState) {
            if (state != newState) {
                GameState oldState = state;
                state = newState;
                logger.info("Game state changed: {} -> {}", oldState, newState);

                for (BiConsumer<GameState, GameState> listener : listeners) {
                    listener.accept(oldState, newState);
                }
            }
        }

        public static void onStateChange(BiConsumer<GameState, GameState> listener) {
            listeners.add(listener);
            logger.debug("Registered state change listener (total: {})", listeners.size());
        }

        public static void onStateChange(GameState targetState, BiConsumer<GameState, GameState> listener) {
            listeners.add((oldState, newState) -> {
                if (newState == targetState) {
                    listener.accept(oldState, newState);
                }
            });
            logger.debug("Registered state change listener for {} (total: {})", targetState, listeners.size());
        }
    }
}
