package nl.juriantech.tnttag.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nl.juriantech.tnttag.Arena;
import nl.juriantech.tnttag.Tnttag;
import nl.juriantech.tnttag.subcommands.CreateSubCommand;
import nl.juriantech.tnttag.subcommands.DeleteSubCommand;
import nl.juriantech.tnttag.subcommands.DumpSubCommand;
import nl.juriantech.tnttag.subcommands.EditorSubCommand;
import nl.juriantech.tnttag.subcommands.ForceJoinSubCommand;
import nl.juriantech.tnttag.subcommands.ForceLeaveSubCommand;
import nl.juriantech.tnttag.subcommands.HelpSubCommand;
import nl.juriantech.tnttag.subcommands.InfoSubCommand;
import nl.juriantech.tnttag.subcommands.JoinGUISubCommand;
import nl.juriantech.tnttag.subcommands.JoinSubCommand;
import nl.juriantech.tnttag.subcommands.LeaveSubCommand;
import nl.juriantech.tnttag.subcommands.ListSubCommand;
import nl.juriantech.tnttag.subcommands.RandomJoinSubCommand;
import nl.juriantech.tnttag.subcommands.ReloadSubCommand;
import nl.juriantech.tnttag.subcommands.SetLobbySubCommand;
import nl.juriantech.tnttag.subcommands.StartSubCommand;
import nl.juriantech.tnttag.subcommands.StatsSubCommand;
import nl.juriantech.tnttag.subcommands.TopSubCommand;
import nl.juriantech.tnttag.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * Paper-native Brigadier command tree. The action classes remain framework-independent while this
 * class owns command structure, permissions, typed arguments, aliases, and suggestions.
 */
public final class TntTagCommand {

    private final Tnttag plugin;
    private final CreateSubCommand create;
    private final DeleteSubCommand delete;
    private final DumpSubCommand dump;
    private final EditorSubCommand editor;
    private final ForceJoinSubCommand forceJoin;
    private final ForceLeaveSubCommand forceLeave;
    private final HelpSubCommand help;
    private final InfoSubCommand info;
    private final JoinGUISubCommand joinGui;
    private final JoinSubCommand join;
    private final LeaveSubCommand leave;
    private final ListSubCommand list;
    private final RandomJoinSubCommand randomJoin;
    private final ReloadSubCommand reload;
    private final SetLobbySubCommand setLobby;
    private final StartSubCommand start;
    private final StatsSubCommand stats;
    private final TopSubCommand top;

    public TntTagCommand(Tnttag plugin, JoinSubCommand joinSubCommand) {
        this.plugin = plugin;
        this.create = new CreateSubCommand(plugin);
        this.delete = new DeleteSubCommand(plugin);
        this.dump = new DumpSubCommand(plugin);
        this.editor = new EditorSubCommand(plugin);
        this.forceJoin = new ForceJoinSubCommand(plugin);
        this.forceLeave = new ForceLeaveSubCommand(plugin);
        this.help = new HelpSubCommand();
        this.info = new InfoSubCommand(plugin);
        this.joinGui = new JoinGUISubCommand(plugin);
        this.join = joinSubCommand;
        this.leave = new LeaveSubCommand(plugin);
        this.list = new ListSubCommand(plugin);
        this.randomJoin = new RandomJoinSubCommand(plugin);
        this.reload = new ReloadSubCommand(plugin);
        this.setLobby = new SetLobbySubCommand(plugin);
        this.start = new StartSubCommand(plugin);
        this.stats = new StatsSubCommand(plugin);
        this.top = new TopSubCommand(plugin);
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("tnttag")
                .executes(context -> executeWithPermission(context, "tnttag.help", help::execute))
                .then(Commands.literal("help")
                        .requires(permission("tnttag.help"))
                        .executes(context -> executePlayer(context, help::execute)))
                .then(Commands.literal("create")
                        .requires(permission("tnttag.create"))
                        .executes(context -> executePlayer(context, create::onCreate)))
                .then(Commands.literal("delete")
                        .requires(permission("tnttag.delete"))
                        .then(arenaArgument("arena")
                                .executes(context -> executePlayer(context, player -> {
                                    Arena arena = findArena(context, "arena", player);
                                    if (arena != null) delete.onDelete(player, arena);
                                }))))
                .then(Commands.literal("dump")
                        .requires(anyPermission("tnttag.dump.all", "tnttag.dump.log"))
                        .then(Commands.literal("all")
                                .requires(permission("tnttag.dump.all"))
                                .executes(context -> executePlayer(context, dump::onDumpAll)))
                        .then(Commands.literal("log")
                                .requires(permission("tnttag.dump.log"))
                                .executes(context -> executePlayer(context, dump::onDumpLog))))
                .then(Commands.literal("editor")
                        .requires(permission("tnttag.editor"))
                        .then(arenaArgument("arena")
                                .executes(context -> executePlayer(context,
                                        player -> editor.onEditor(player, stringArgument(context, "arena"))))))
                .then(optionalArenaCommand("forcejoin", "tnttag.forcejoin", forceJoin::onJoin))
                .then(optionalArenaCommand("forceleave", "tnttag.forceleave", forceLeave::onLeave))
                .then(Commands.literal("info")
                        .requires(permission("tnttag.info"))
                        .then(arenaArgument("arena")
                                .executes(context -> executePlayer(context,
                                        player -> info.onInfo(player, stringArgument(context, "arena"))))))
                .then(Commands.literal("joingui")
                        .requires(permission("tnttag.gui.join"))
                        .executes(context -> executePlayer(context, joinGui::onGUIJoin)))
                .then(optionalArenaCommand("join", "tnttag.join", join::onJoin))
                .then(Commands.literal("leave")
                        .executes(context -> executePlayer(context, leave::onLeave)))
                .then(Commands.literal("list")
                        .requires(permission("tnttag.list"))
                        .executes(context -> executePlayer(context, list::onList)))
                .then(Commands.literal("randomjoin")
                        .requires(permission("tnttag.join"))
                        .executes(context -> executePlayer(context, randomJoin::onJoin)))
                .then(Commands.literal("autojoin")
                        .requires(permission("tnttag.join"))
                        .executes(context -> executePlayer(context, randomJoin::onJoin)))
                .then(Commands.literal("reload")
                        .requires(permission("tnttag.reload"))
                        .executes(context -> executePlayer(context, reload::onReload)))
                .then(Commands.literal("setlobby")
                        .requires(permission("tnttag.setlobby"))
                        .executes(context -> executePlayer(context, setLobby::onSetLobby)))
                .then(Commands.literal("start")
                        .requires(permission("tnttag.start"))
                        .then(arenaArgument("arena")
                                .executes(context -> executePlayer(context,
                                        player -> start.onStart(player, stringArgument(context, "arena"), false)))
                                .then(Commands.argument("forced", BoolArgumentType.bool())
                                        .executes(context -> executePlayer(context,
                                                player -> start.onStart(
                                                        player,
                                                        stringArgument(context, "arena"),
                                                        BoolArgumentType.getBool(context, "forced")
                                                ))))))
                .then(Commands.literal("stats")
                        .requires(permission("tnttag.stats"))
                        .executes(context -> executePlayer(context, stats::onStats)))
                .then(Commands.literal("top")
                        .requires(permission("tnttag.top"))
                        .then(topType("wins"))
                        .then(topType("timestagged"))
                        .then(topType("tags")))
                .build();
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> optionalArenaCommand(
            String literal,
            String requiredPermission,
            ArenaNameAction action
    ) {
        return Commands.literal(literal)
                .requires(permission(requiredPermission))
                .executes(context -> executePlayer(context, player -> action.run(player, null)))
                .then(arenaArgument("arena")
                        .executes(context -> executePlayer(context,
                                player -> action.run(player, stringArgument(context, "arena")))));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> topType(String type) {
        return Commands.literal(type)
                .executes(context -> executePlayer(context, player -> top.onTop(player, type)));
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> arenaArgument(
            String name
    ) {
        return Commands.argument(name, StringArgumentType.string()).suggests(this::suggestArenas);
    }

    private CompletableFuture<Suggestions> suggestArenas(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        String remaining = builder.getRemainingLowerCase();
        String filterText = remaining.startsWith("\"") ? remaining.substring(1) : remaining;
        plugin.getArenaManager().getArenaObjects().stream()
                .map(Arena::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(filterText))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(StringArgumentType::escapeIfRequired)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private Predicate<CommandSourceStack> permission(String permission) {
        return source -> source.getSender().hasPermission(permission);
    }

    private Predicate<CommandSourceStack> anyPermission(String... permissions) {
        return source -> {
            for (String permission : permissions) {
                if (source.getSender().hasPermission(permission)) return true;
            }
            return false;
        };
    }

    private int executeWithPermission(
            CommandContext<CommandSourceStack> context,
            String permission,
            PlayerAction action
    ) {
        if (!context.getSource().getSender().hasPermission(permission)) {
            CommandSender sender = context.getSource().getSender();
            sender.sendMessage(ChatUtils.component(ChatUtils.getRaw("general.no-permission")));
            return 0;
        }
        return executePlayer(context, action);
    }

    private int executePlayer(CommandContext<CommandSourceStack> context, PlayerAction action) {
        Player player = resolvePlayer(context.getSource());
        if (player == null) {
            context.getSource().getSender().sendMessage(
                    ChatUtils.component("<red>This command can only be used by a player.")
            );
            return 0;
        }

        try {
            action.run(player);
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to execute TNT-Tag command for " + player.getName() + ".",
                    exception);
            player.sendMessage(ChatUtils.component(
                    "<red>The command could not be completed. Check the server console for details."
            ));
            return 0;
        }
    }

    private Player resolvePlayer(CommandSourceStack source) {
        Entity executor = source.getExecutor();
        if (executor instanceof Player player) return player;
        if (source.getSender() instanceof Player player) return player;
        return null;
    }

    private Arena findArena(CommandContext<CommandSourceStack> context, String argumentName, Player player) {
        Arena arena = plugin.getArenaManager().getArena(stringArgument(context, argumentName));
        if (arena == null) {
            ChatUtils.sendMessage(player, "commands.invalid-arena");
        }
        return arena;
    }

    private String stringArgument(CommandContext<CommandSourceStack> context, String argumentName) {
        return StringArgumentType.getString(context, argumentName);
    }

    @FunctionalInterface
    private interface PlayerAction {
        void run(Player player) throws Exception;
    }

    @FunctionalInterface
    private interface ArenaNameAction {
        void run(Player player, String arenaName) throws Exception;
    }
}
