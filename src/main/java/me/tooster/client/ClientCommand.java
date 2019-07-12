package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.CommandException;
import org.jetbrains.annotations.NotNull;

public enum ClientCommand implements Command {
    @Alias("connect") CONNECT,
    @Alias("nick") NICK;


    public static final ClientCommand[] cachedValues = ClientCommand.values();


    public static Compiled parse(@NotNull String input) throws CommandException {
        return Command._parse(ClientCommand.class, input);
    }
}
