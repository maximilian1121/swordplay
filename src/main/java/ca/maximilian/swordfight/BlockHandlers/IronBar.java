package ca.maximilian.swordfight.BlockHandlers;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;

import java.util.Map;

public class IronBar implements BlockHandler {

    private static final Key KEY = Key.key("minecraft:iron_bars");

    @Override
    public void tick(Tick tick) {
        try {
            var instance = tick.getInstance();
            var pos = tick.getBlockPosition();

            int x = pos.blockX();
            int y = pos.blockY();
            int z = pos.blockZ();

            boolean north = isIronBar(instance.getBlock(x, y, z - 1));
            boolean east  = isIronBar(instance.getBlock(x + 1, y, z));
            boolean south = isIronBar(instance.getBlock(x, y, z + 1));
            boolean west  = isIronBar(instance.getBlock(x - 1, y, z));

            Block current = tick.getBlock();

            Block updated = current.withProperties(Map.of(
                    "north", Boolean.toString(north),
                    "east",  Boolean.toString(east),
                    "south", Boolean.toString(south),
                    "west",  Boolean.toString(west)
            ));

            if (!current.equals(updated)) {
                instance.setBlock(pos, updated);
            }
        } catch (NullPointerException e) {
            return;
        }
    }

    private boolean isIronBar(Block block) {
        return block.key().equals(KEY);
    }

    @Override
    public boolean isTickable() {
        return true;
    }

    @Override
    public Key getKey() {
        return KEY;
    }
}