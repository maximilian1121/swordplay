package ca.maximilian.swordfight;

import ca.maximilian.swordfight.BlockHandlers.IronBar;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType; // Ensure this import is present
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class HandledAnvilLoader extends AnvilLoader {
    private static final Logger logger = LoggerFactory.getLogger(HandledAnvilLoader.class);
    private final IronBar ironBar = new IronBar();

    public HandledAnvilLoader(Path path, Key dimension) {
        super(path, dimension);
        logger.debug("HandledAnvilLoader created for path: {}, dimension: {}", path, dimension);
    }

    @Override
    public @Nullable Chunk loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        logger.trace("Loading chunk ({}, {})...", chunkX, chunkZ);
        Chunk chunk = super.loadChunk(instance, chunkX, chunkZ);
        if (chunk == null) {
            logger.trace("Chunk ({}, {}) returned null", chunkX, chunkZ);
            return null;
        }

        DimensionType dimension = instance.getCachedDimensionType();
        int minY = dimension.minY();
        int maxY = dimension.maxY();

        int ironBarsFound = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (block.id() == Block.IRON_BARS.id()) {
                        Block handledBlock = block.withHandler(ironBar);
                        chunk.setBlock(x, y, z, handledBlock);
                        ironBarsFound++;
                    }
                }
            }
        }

        return chunk;
    }
}
