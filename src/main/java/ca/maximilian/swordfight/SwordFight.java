package ca.maximilian.swordfight;

import lombok.Getter;
import net.minestom.server.Auth;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

public class SwordFight {
    private static final Logger logger = LoggerFactory.getLogger(SwordFight.class);
    @Getter
    private static Server server;

    enum AuthArgumentTypes {
        OFFLINE,
        ONLINE,
        VELOCITY,
        BUNGEE
    }

    void main(String[] args) {
        logger.debug("Parsing command line arguments: {}", String.join(" ", args));

        ArgumentParser parser = ArgumentParsers.newFor("Sword fight").build();

        parser.addArgument("-p", "--port")
                .type(Integer.class)
                .help("The port to bind to.")
                .setDefault(25565);

        parser.addArgument("-i", "--ip")
                .type(String.class)
                .help("The ip address to bind to.")
                .setDefault("0.0.0.0");

        parser.addArgument("-a", "--auth")
                .type(AuthArgumentTypes.class)
                .help("The authentication method to use.")
                .setDefault(AuthArgumentTypes.ONLINE);

        parser.addArgument("-k", "--key")
                .type(String.class)
                .help("The velocity security key/bungee token to use.")
                .setDefault("no");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
            logger.debug("Arguments parsed successfully");
        } catch (ArgumentParserException e) {
            logger.error("Failed to parse command line arguments", e);
            parser.handleError(e);
            System.exit(1);
        }
        assert ns != null;

        // Initialize the server
        logger.info("Initializing server...");
        System.setProperty("minestom.terminal.disabled", "true");
        System.setProperty("minestom.chunk-view-distance", "96");
        System.setProperty("minestom.accept-transfers", "true");

        AuthArgumentTypes auth = ns.get("auth");
        String authKey = ns.get("key");
        logger.debug("Auth type: {}, key provided: {}", auth, authKey != null && !authKey.isBlank());

        if ((auth == AuthArgumentTypes.BUNGEE || auth == AuthArgumentTypes.VELOCITY)
                && (authKey == null || authKey.isBlank())) {
            logger.error("--key is required when --auth is BUNGEE or VELOCITY");
            parser.printUsage();
            throw new IllegalArgumentException(
                    "--key is required when --auth is BUNGEE or VELOCITY"
            );
        }

        Auth authType = switch (auth) {
            case ONLINE -> new Auth.Online();
            case BUNGEE -> new Auth.Bungee(Set.of(authKey));
            case VELOCITY -> new Auth.Velocity(authKey);
            case OFFLINE -> new Auth.Offline();
        };

        logger.info("Starting server on " + ns.getString("ip") + ":" + ns.getInt("port") + " with auth: " + authType.getClass().getSimpleName());

        logger.info("Creating server instance...");
        server = new Server(ns.getString("ip"), ns.getInt("port"), authType);

        logger.info("Server initialized, starting...");
        server.start();
        logger.info("Server started and listening on {}:{}", ns.getString("ip"), ns.getInt("port"));
    }
}