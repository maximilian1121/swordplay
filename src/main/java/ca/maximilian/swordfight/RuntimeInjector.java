package ca.maximilian.swordfight;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeInjector {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeInjector.class);

    public static void inject() {
        logger.info("Installing ByteBuddy agent for runtime instrumentation...");
        var instrumentation = ByteBuddyAgent.install();

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION) // Modifies already-loaded classes
                .type(ElementMatchers.named("net.minestom.server.instance.block.Block"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(BlockInterceptorAdvice.class)
                                .on(ElementMatchers.named("fromKey")
                                        .and(ElementMatchers.takesArguments(String.class))))
                )
                .installOn(instrumentation);
        logger.info("ByteBuddy instrumentation installed successfully");
        logger.debug("Block remappings: minecraft:chain -> minecraft:iron_chain, minecraft:grass -> minecraft:short_grass");
    }

    public static class BlockInterceptorAdvice {
        @Advice.OnMethodEnter
        public static void onEnter(
                @Advice.Argument(value = 0, readOnly = false) String key
        ) {
            if ("minecraft:chain".equals(key)) {
                logger.trace("Remapping block: minecraft:chain -> minecraft:iron_chain");
                key = "minecraft:iron_chain";
            }
            if ("minecraft:grass".equals(key)) {
                logger.trace("Remapping block: minecraft:grass -> minecraft:short_grass");
                key = "minecraft:short_grass";
            }
        }
    }
}
