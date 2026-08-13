package ca.maximilian.swordfight.BlockHandlers;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import org.jspecify.annotations.NonNull;

public class DummyBlockHandler implements BlockHandler {
    private final Key key;

    public DummyBlockHandler(@NonNull String namespace) {
        this.key = Key.key(namespace);
    }

    @Override
    public @NonNull Key getKey() {
        return key;
    }
}
