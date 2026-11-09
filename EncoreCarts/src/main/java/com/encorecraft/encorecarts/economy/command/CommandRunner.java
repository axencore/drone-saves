package com.encorecraft.encorecarts.economy.command;

import com.encorecraft.encorecarts.EncoreCarts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Runs a server command synchronously with full permissions and captures everything it prints. */
public final class CommandRunner {
    private static final TextColor FAILURE_COLOR = TextColor.fromLegacyFormat(ChatFormatting.RED);

    private CommandRunner() {
    }

    public record Result(boolean success, int returnValue, List<Component> messages) {
        public String text() {
            return messages.stream().map(Component::getString).collect(Collectors.joining("\n"));
        }
    }

    public static Result run(MinecraftServer server, String command) {
        List<Component> messages = new ArrayList<>();
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component component) {
                messages.add(component);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        ServerLevel level = server.overworld();
        CommandSourceStack source = new CommandSourceStack(sink, Vec3.atLowerCornerOf(level.getSharedSpawnPos()), Vec2.ZERO,
                level, Commands.LEVEL_OWNERS, EncoreCarts.MOD_ID, Component.literal("EncoreCarts"), server, null);
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        try {
            ParseResults<CommandSourceStack> parse = dispatcher.parse(command, source);
            CommandSyntaxException parseError = Commands.getParseException(parse);
            if (parseError != null) {
                messages.add(Component.literal(parseError.getMessage()));
                return new Result(false, 0, messages);
            }
            int value = dispatcher.execute(parse);
            boolean failureReported = messages.stream().anyMatch(CommandRunner::isFailureMessage);
            return new Result(!failureReported, value, messages);
        } catch (CommandSyntaxException e) {
            messages.add(Component.literal(e.getMessage()));
            return new Result(false, 0, messages);
        } catch (RuntimeException e) {
            EncoreCarts.LOGGER.error("Wallet command '{}' crashed", command, e);
            return new Result(false, 0, messages);
        }
    }

    private static boolean isFailureMessage(Component component) {
        return FAILURE_COLOR.equals(component.getStyle().getColor());
    }
}
