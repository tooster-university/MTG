package me.tooster.server;

import me.tooster.common.Command;

import java.util.EnumSet;

enum ServerCommand implements Command {

    // generic - always enabled
    // -----------------------------------------------------

    @Alias({"/help", "/?"}) @Help("/help [cmd] displays help [for command].") HELP,
    @Alias({"/whisper", "/w", "/m"}) @Help("/m <player> <msg> sends private message to player.") WHISPER,
    @Alias("/say") @Help("/say <msg> sends message to local hub chat.") SAY,
    @Alias({"/shout", "!"}) @Help("/shout <msg> sends message to server chat.") SHOUT,

    HUB_ADD_USER, HUB_REMOVE_USER,

    @Alias("/ready") @Help("switches the ready/not ready state.") READY,

    ;

    public static final EnumSet<ServerCommand> commands = EnumSet.allOf(ServerCommand.class);
}
