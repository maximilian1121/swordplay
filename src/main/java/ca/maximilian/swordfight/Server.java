package ca.maximilian.swordfight;

import ca.maximilian.swordfight.BlockHandlers.DummyBlockHandler;
import ca.maximilian.swordfight.BlockHandlers.IronBar;
import ca.maximilian.swordfight.Game.GameConstants;
import ca.maximilian.swordfight.Game.GameFunctions;
import ca.maximilian.swordfight.Game.GameLoop;
import ca.maximilian.swordfight.Game.GameState;
import lombok.Getter;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.color.TeamColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.inventory.InventoryOpenEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.registry.Registry;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.world.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static ca.maximilian.swordfight.Game.GameFunctions.isCrippled;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final Tag<Long> LAST_ATTACK_TIME = Tag.Long("last_attack_time").defaultValue(0L);
    private static final long FULL_CHARGE_MS = 2000L;

    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private MinecraftServer server;
    @Getter
    private InstanceManager instanceManager;
    @Getter
    private TeamManager teamManager;
    @Getter
    private SchedulerManager schedulerManager;
    @Getter
    private GlobalEventHandler globalEventHandler;
    @Getter
    private Registry<DamageType> damageTypeRegistry;

    @Getter
    private Team spectatorsTeam;
    @Getter
    private Team redTeam;
    @Getter
    private Team blueTeam;

    @Getter
    private InstanceContainer defaultInstance;

    public Server(String host, int port, Auth auth) {
        logger.info("Initializing server with auth on {}:{}", host, port);
        this.host = host;
        this.port = port;

        server = MinecraftServer.init(auth);
        commonInit();
    }

    public Server(String host, int port) {
        logger.info("Initializing server without auth on {}:{}", host, port);
        this.host = host;
        this.port = port;

        server = MinecraftServer.init();
        commonInit();
    }

    private void commonInit() {
        logger.debug("Initializing Minestom managers...");
        instanceManager = MinecraftServer.getInstanceManager();
        teamManager = MinecraftServer.getTeamManager();
        schedulerManager = MinecraftServer.getSchedulerManager();
        globalEventHandler = MinecraftServer.getGlobalEventHandler();
        damageTypeRegistry = MinecraftServer.getDamageTypeRegistry();

        logger.debug("Creating teams...");
        spectatorsTeam = teamManager.createTeam("spectators");
        spectatorsTeam.setTeamColor(TeamColor.GOLD);
        spectatorsTeam.setTeamDisplayName(Component.text("Spectators"));
        spectatorsTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);
        redTeam = teamManager.createTeam("red");
        redTeam.setTeamColor(TeamColor.RED);
        redTeam.setTeamDisplayName(Component.text("Red"));
        redTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);
        blueTeam = teamManager.createTeam("blue");
        blueTeam.setTeamColor(TeamColor.BLUE);
        blueTeam.setTeamDisplayName(Component.text("Blue"));
        blueTeam.setCollisionRule(TeamsPacket.CollisionRule.NEVER);
        logger.debug("Teams created: spectators, red, blue");

        logger.debug("Registering block handlers...");
        registerBlockHandlers();
        logger.debug("Setting up default instance...");
        setupDefaultInstance();
        logger.debug("Registering global event handlers...");

        globalEventHandler.addListener(PlayerDeathEvent.class, event -> {
            Player victim = event.getPlayer();
            logger.trace("Player death event: {}", victim.getUsername());

            Damage lastDamage = victim.getLastDamageSource();

            if (lastDamage != null && lastDamage.getType() == DamageType.DROWN) {
                if (victim.getTeam() == spectatorsTeam) {
                    event.setDeathText(Component.text("Splish splash!"));
                    event.setChatMessage(Component.text("%s tried to go for a swim!".formatted(victim.getUsername())));
                }
            }
        });

        globalEventHandler.addListener(PlayerChatEvent.class, event -> {
            event.setCancelled(true);
            Player player = event.getPlayer();
            logger.debug("Chat message from {}: {}", player.getUsername(), event.getRawMessage());
            Team team = player.getTeam();
            for (Player recipient : event.getRecipients()) {
                recipient.sendMessage(
                        Component.text("<" + player.getUsername() + " - ").append(team.getTeamDisplayName()).append(Component.text("> ")).color(team.getTeamColor().textColor())
                                .append(Component.text(event.getRawMessage()).color(TextColor.fromHexString("#ffffff")))
                );
            }
        });

        globalEventHandler.addListener(PlayerLoadedEvent.class, event -> {
            Player player = event.getPlayer();
            logger.info("Player loaded: {} ({})", player.getUsername(), player.getUuid());
            GameFunctions.setSpectator(player);
        });

        globalEventHandler.addListener(PlayerMoveEvent.class, event -> {
            if (!isCrippled(event.getPlayer())) return;

            event.setNewPosition(new Pos(event.getPlayer().getPosition(), event.getNewPosition().yaw(), event.getNewPosition().pitch()));
        });

        globalEventHandler.addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player player && event.getTarget() instanceof Player target) {
                if (player.getTeam() == blueTeam || player.getTeam() == redTeam) {
                    if (GameState.Manager.getState() != GameState.PLAYING) return;

                    PlayerHand hand = target.getItemUseHand();
                    if (hand != null) {
                        ItemStack item = target.getItemInHand(hand);
                        if (item.material() == Material.SHIELD) {
                            double blockArcDegrees = 100.0;
                            double halfArcRad = Math.toRadians(blockArcDegrees / 2.0);
                            double dotThreshold = Math.cos(halfArcRad);
                            double toAttackerX = player.getPosition().x() - target.getPosition().x();
                            double toAttackerZ = player.getPosition().z() - target.getPosition().z();
                            double lenSq = toAttackerX * toAttackerX + toAttackerZ * toAttackerZ;
                            if (lenSq > 0) {
                                double invLen = 1.0 / Math.sqrt(lenSq);
                                double dirX = toAttackerX * invLen;
                                double dirZ = toAttackerZ * invLen;
                                float yawRad = (float) Math.toRadians(target.getPosition().yaw());
                                double facingX = -Math.sin(yawRad);
                                double facingZ = Math.cos(yawRad);
                                double dot = dirX * facingX + dirZ * facingZ;
                                if (dot > dotThreshold) {
                                    Audiences.players().playSound(GameConstants.BLOCK_SOUND);
                                    return;
                                }
                            }
                        }
                    }

                    double dx = target.getPosition().x() - player.getPosition().x();
                    double dz = target.getPosition().z() - player.getPosition().z();
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance == 0) return;
                    dx /= distance;
                    dz /= distance;

                    long now = System.currentTimeMillis();
                    long lastAttack = player.getTag(LAST_ATTACK_TIME);
                    double chargeProgress = Math.min(1.0, (now - lastAttack) / (double) FULL_CHARGE_MS);
                    player.setTag(LAST_ATTACK_TIME, now);

                    double horizontalStrength = 8 * chargeProgress;
                    double verticalStrength = 1.5 * chargeProgress;

                    net.minestom.server.coordinate.Vec knockback = new net.minestom.server.coordinate.Vec(
                            dx * horizontalStrength,
                            verticalStrength,
                            dz * horizontalStrength
                    );
                    target.setVelocity(target.getVelocity().add(knockback));
                    target.damage(Damage.fromPlayer(player, 0));
                }
            }
        });

        globalEventHandler.addListener(InventoryOpenEvent.class, event -> {
            event.setCancelled(true);
        });

        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> {
            event.setCancelled(true);
        });
    }

    public void start() {
        logger.info("Starting game loop...");
        GameLoop.run();

        logger.info("Starting Minestom server...");
        getServer().start(getHost(), getPort());
    }

    public Set<Player> getAllPlayers() {
        return defaultInstance.getPlayers();
    }

    public List<Player> getPlayersInTeam(Team team) {
        return getAllPlayers().stream().filter(player -> player.getTeam() == team).toList();
    }

    public void registerBlockHandlers() {
        logger.debug("Registering dummy block handlers...");
        List<String> registerDummiesTo = List.of(
                "minecraft:barrel",
                "minecraft:sign",
                "minecraft:bed",
                "minecraft:chest",
                "minecraft:chain",
                "minecraft:banner",
                "minecraft:mob_spawner",
                "minecraft:blast_furnace",
                "minecraft:furnace",
                "minecraft:campfire",
                "minecraft:decorated_pot",
                "minecraft:skull",
                "minecraft:shulker_box",
                "minecraft:ender_chest",
                "minecraft:chiseled_bookshelf",
                "minecraft:hanging_sign",
                "minecraft:hopper",
                "minecraft:bell",
                "minecraft:smoker",
                "minecraft:brewing_stand",
                "minecraft:dispenser",
                "minecraft:crafter",
                "minecraft:jukebox",
                "minecraft:lectern",
                "minecraft:command_block",
                "minecraft:calibrated_sculk_sensor",
                "minecraft:beehive",
                "minecraft:dropper",
                "minecraft:comparator",
                "minecraft:sculk_shrieker",
                "minecraft:sculk_catalyst",
                "minecraft:sculk_sensor",
                "minecraft:enchanting_table",
                "minecraft:trial_spawner",
                "minecraft:vault",
                "minecraft:brushable_block"
        );

        for (String namespace : registerDummiesTo) {
            MinecraftServer.getBlockManager().registerHandler(namespace, () -> new DummyBlockHandler(namespace));
        }
        logger.debug("Registered {} dummy block handlers", registerDummiesTo.size());

        MinecraftServer.getBlockManager()
                .registerHandler("minecraft:iron_bars", IronBar::new);
        logger.debug("Registered IronBar handler for minecraft:iron_bars");
    }

    public void setupDefaultInstance() {
        logger.info("Setting up default world instance...");
        // Get world path
        Path targetPath = Path.of("wuhu-island");

        if (!Files.exists(targetPath)) {
            logger.error("World directory not found! Please download the world at \"https://www.planetminecraft.com/project/wuhu-island/\" run it in 26.2 then place the updated wuhu-island folder in the working directory!");
            System.exit(-1);
        } else {
            logger.debug("World directory found!");
        }

        logger.debug("Injecting runtime block remappings...");
        RuntimeInjector.inject();

        logger.debug("Creating instance container with AnvilLoader...");
        // Create instance with default dimension type
        defaultInstance = instanceManager.createInstanceContainer(
                new HandledAnvilLoader(targetPath, DimensionType.OVERWORLD.key())
        );

        defaultInstance.setTime(7200);
        logger.debug("Instance created, setting time to 7200 (noon)");

        Point signPos = new Vec(-176, 38, 5);

        ListBinaryTag messagesList = ListBinaryTag.listBinaryTag(
                BinaryTagTypes.STRING,
                java.util.List.of(
                        StringBinaryTag.stringBinaryTag("Made by max"),
                        StringBinaryTag.stringBinaryTag("Made with"),
                        StringBinaryTag.stringBinaryTag("minestom"),
                        StringBinaryTag.stringBinaryTag("Map: danicraft203")
                )
        );

        CompoundBinaryTag frontText = CompoundBinaryTag.builder()
                .put("messages", messagesList)
                .build();

        CompoundBinaryTag rootTag = CompoundBinaryTag.builder()
                .put("front_text", frontText)
                .putBoolean("is_waxed", false)
                .build();

        Block rotatedSign = Block.OAK_SIGN
                .withProperty("rotation", "4")
                .withNbt(rootTag);
        defaultInstance.setBlock(signPos, rotatedSign);

        String verityTexture =
                "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTJlNzEyYWM4MjczZWVhYzQxM2JmMjNlZjFjMTI0ODM5YTQ3YjE1YWU0YzY3N2UzNWQ2YjBmZjU3NDIxNWY2YiJ9fX0=";

        CompoundBinaryTag textureProperty = CompoundBinaryTag.builder()
                .putString("name", "textures")
                .putString("value", verityTexture)
                .build();

        ListBinaryTag properties = ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                .add(textureProperty)
                .build();

        CompoundBinaryTag profile = CompoundBinaryTag.builder()
                .putIntArray("id", new int[]{
                        -966169363,
                        -1675469134,
                        -2012134339,
                        -1512053620
                })
                .put("properties", properties)
                .build();

        Block verity = Block.PLAYER_HEAD
                .withProperty("rotation", "9")
                .withNbt(
                        CompoundBinaryTag.builder()
                                .put("profile", profile)
                                .build()
                );

        defaultInstance.setBlock(
                new Vec(-181, 20, 6),
                verity
        );

        logger.debug("Loading platform chunk (-10, 0)...");
        CompletableFuture<Chunk> platformChunk = defaultInstance.loadChunk(-10, 0);

        platformChunk.thenAccept(chunk -> {
            logger.debug("Platform chunk loaded, filling water and air blocks...");
            int minLocalX = 12;
            int maxLocalX = 14;

            int minLocalZ = 4;
            int maxLocalZ = 6;

            for (int x = minLocalX; x <= maxLocalX; x++) {
                for (int y = 8; y <= 19; y++) {
                    for (int z = minLocalZ; z <= maxLocalZ; z++) {
                        chunk.setBlock(x, y, z, Block.WATER);
                    }
                }
            }

            for (int x = minLocalX; x <= maxLocalX; x++) {
                for (int y = 20; y <= 29; y++) {
                    for (int z = minLocalZ; z <= maxLocalZ; z++) {
                        chunk.setBlock(x, y, z, Block.AIR);
                    }
                }
            }
        });

        defaultInstance.setBlock(new Pos(-144, 26, 6), Block.AIR);
        defaultInstance.setBlock(new Pos(-145, 26, 6), Block.AIR);
        defaultInstance.setBlock(new Pos(-145, 26, 5), Block.AIR);
        defaultInstance.setBlock(new Pos(-147, 26, 8), Block.AIR);
        defaultInstance.setBlock(new Pos(-147, 26, 7), Block.AIR);
        defaultInstance.setBlock(new Pos(-148, 26, 7), Block.AIR);
        defaultInstance.setBlock(new Pos(-149, 26, 5), Block.AIR);
        defaultInstance.setBlock(new Pos(-149, 26, 4), Block.AIR);
        defaultInstance.setBlock(new Pos(-150, 26, 4), Block.AIR);
        defaultInstance.setBlock(new Pos(-147, 26, 3), Block.AIR);
        defaultInstance.setBlock(new Pos(-147, 26, 3), Block.AIR);
        defaultInstance.setBlock(new Pos(-146, 26, 3), Block.AIR);
        defaultInstance.setBlock(new Pos(-146, 26, 2), Block.AIR);

        defaultInstance.setBlock(GameConstants.CENTRE_POINT.sub(0, 1, 0), Block.LIGHT_BLUE_CONCRETE);

        // Calculate lighting
        defaultInstance.setChunkSupplier(LightingChunk::new);
        logger.debug("Lighting chunk supplier set");

        // Add configuration event
        logger.debug("Registering player configuration handler...");
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            logger.info("Configuring player: {} ({})", player.getUsername(), player.getUuid());
            event.setSpawningInstance(defaultInstance);

            player.setRespawnPoint(GameConstants.SPAWN_POINT);
            player.setGameMode(GameMode.ADVENTURE);

            String url = "https://files.latific.click/file/5b1f71d3-4a4d-4153-a96d-f4cc79566e41.zip";

            String hash;

            try {
                hash = ResourceHashUtil.fetchSHA1(url);
            } catch (Exception e) {
                logger.warn("Failed to fetch resource pack hash for {}", url, e);
                return;
            }

            logger.info("Sending resource pack to player: {} ({}). Pack URL: {} ({})", player.getUsername(), player.getUuid(), url, hash);

            ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                    .id(UUID.randomUUID())
                    .uri(URI.create(url))
                    .hash(hash)
                    .build();

            ResourcePackRequest packRequest = ResourcePackRequest.resourcePackRequest()
                    .prompt(Component.text("Please install the resource pack!"))
                    .required(true)
                    .packs(packInfo)
                    .asResourcePackRequest();

            player.sendResourcePacks(packRequest);

            player.sendResourcePacks(packRequest);

            player.getAttribute(Attribute.ATTACK_SPEED).setBaseValue(1024);
        });
    }
}
