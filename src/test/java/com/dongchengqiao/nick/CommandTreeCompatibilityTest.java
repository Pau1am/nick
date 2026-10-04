package com.dongchengqiao.nick;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.flag.FeatureFlags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one invariant that a client without this mod depends on: every argument type in the
 * {@code /nick} command tree has to be one a vanilla client can look up.
 *
 * <p>The server pushes its whole command tree to every client, and argument types travel as numeric
 * registry ids. A type the client does not know decodes to a {@code null} stub, the node degrades
 * into a {@code RootCommandNode}, and its parent silently skips it - so the whole {@code /nick set}
 * branch would just vanish from that client's tree, taking tab-completion and syntax hints with it.
 * Nothing logs, nothing crashes, and the command itself still works, which is exactly why this
 * needs a test rather than a reviewer's eye.
 *
 * <p>The last test is a negative control: it feeds in a payload with an unresolvable argument type
 * and requires the problem to be reported. Without it, the checks above could pass while measuring
 * nothing at all.
 */
class CommandTreeCompatibilityTest {
	@BeforeAll
	static void bootstrapGame() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	@DisplayName("every argument type in /nick is resolvable by a vanilla client")
	void argumentTypesAreResolvableByVanillaClients() {
		RootCommandNode<CommandSourceStack> root = buildNickTree();

		TreeSet<String> types = new TreeSet<>();
		List<String> unresolved = new ArrayList<>();
		for (CommandNode<CommandSourceStack> child : root.getChildren()) {
			collectArgumentTypes(child, types, unresolved);
		}

		assertFalse(types.isEmpty(), "the /nick tree should contain at least one argument node");
		assertTrue(unresolved.isEmpty(),
			"these argument types are not vanilla, so a client without the mod would drop the branch: "
				+ unresolved);
	}

	/** Namespaces that every client has. Anything else only exists where the owning mod is installed. */
	private static final java.util.Set<String> VANILLA_NAMESPACES = java.util.Set.of("minecraft", "brigadier");

	@Test
	@DisplayName("/nick set takes its nickname through vanilla's StringArgumentType")
	void nickSetUsesAVanillaArgumentType() {
		// The direct, deterministic form of the rule. The tests around this one check the
		// consequences (registry namespace, what the client receives); this one states the rule.
		CommandNode<CommandSourceStack> name = followCommand(buildNickTree(), "nick", "set", "name");

		assertNotNull(name, "/nick set <name> should exist");
		assertInstanceOf(ArgumentCommandNode.class, name);
		assertInstanceOf(StringArgumentType.class, ((ArgumentCommandNode<?, ?>) name).getType(),
			"a mod-provided argument type cannot be resolved by a client without the mod");
	}

	@Test
	@DisplayName("a client without the mod rebuilds the full /nick set <name> path")
	void vanillaClientRebuildsTheTree() throws Exception {
		Inspection result = inspect(encode(buildNickTree()));

		assertEquals(0, result.nullStubs(), "no argument node may decode to a null stub");
		assertNull(result.rebuildError(), "rebuilding the client tree must not throw");
		assertTrue(result.nickSetPathPresent(),
			"the rebuilt client tree must still contain nick -> set -> name");
	}

	@Test
	@DisplayName("negative control: an unresolvable argument type is detected")
	void unresolvableArgumentTypeIsDetected() throws Exception {
		Inspection broken = inspect(craftUnresolvablePayload());

		assertTrue(broken.nullStubs() > 0, "the control payload must decode to a null stub");
		assertFalse(broken.branchPresent(),
			"the control payload's argument branch must be missing, which is how the real bug shows up");
	}

	// ---------------------------------------------------------------------------------------

	private static RootCommandNode<CommandSourceStack> buildNickTree() {
		NickSettings.commandNick = true;
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		NickCommand.registerCommands(dispatcher);
		return dispatcher.getRoot();
	}

	private static byte[] encode(RootCommandNode<CommandSourceStack> root) {
		ClientboundCommandsPacket packet = new ClientboundCommandsPacket(root, new Inspector());
		FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
		ClientboundCommandsPacket.STREAM_CODEC.encode(out, packet);
		byte[] payload = new byte[out.readableBytes()];
		out.getBytes(out.readerIndex(), payload);
		return payload;
	}

	private record Inspection(int argumentNodes, int nullStubs, String rebuildError, boolean branchPresent) {
		boolean nickSetPathPresent() {
			return branchPresent;
		}
	}

	/**
	 * Decodes a payload and rebuilds the tree the way a client does, reporting what happened.
	 * <p>
	 * A null stub does not throw: the parent skips the degraded node. So the meaningful measurement
	 * is whether the branch survives, not whether an exception was raised.
	 */
	private static Inspection inspect(byte[] payload) throws Exception {
		FriendlyByteBuf in = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
		ClientboundCommandsPacket decoded = ClientboundCommandsPacket.STREAM_CODEC.decode(in);

		Field entriesField = ClientboundCommandsPacket.class.getDeclaredField("entries");
		entriesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<Object> entries = (List<Object>) entriesField.get(decoded);

		int argumentNodes = 0;
		int nullStubs = 0;
		for (Object entry : entries) {
			Class<?> entryClass = entry.getClass();
			Field flagsField = entryClass.getDeclaredField("flags");
			flagsField.setAccessible(true);
			if ((flagsField.getInt(entry) & 3) != 2) {
				continue; // not an argument node
			}
			argumentNodes++;
			Field stubField = entryClass.getDeclaredField("stub");
			stubField.setAccessible(true);
			if (stubField.get(entry) == null) {
				nullStubs++;
			}
		}

		boolean branchPresent = false;
		String rebuildError = null;
		try {
			CommandBuildContext buildContext = CommandBuildContext.simple(
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), FeatureFlags.VANILLA_SET);
			RootCommandNode<Object> clientRoot = decoded.getRoot(buildContext, new ClientNodeBuilder());
			branchPresent = follow(follow(follow(clientRoot, "nick"), "set"), "name") != null;
		} catch (Throwable t) {
			rebuildError = t.getClass().getSimpleName() + ": " + t.getMessage();
		}
		return new Inspection(argumentNodes, nullStubs, rebuildError, branchPresent);
	}

	private static CommandNode<Object> follow(CommandNode<Object> node, String name) {
		if (node == null) {
			return null;
		}
		for (CommandNode<Object> child : node.getChildren()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}
		return null;
	}

	/** Walks a path of node names, returning {@code null} as soon as any step is missing. */
	private static CommandNode<CommandSourceStack> followCommand(RootCommandNode<CommandSourceStack> root,
																String... path) {
		CommandNode<CommandSourceStack> node = root;
		for (String step : path) {
			if (node == null) {
				return null;
			}
			CommandNode<CommandSourceStack> next = null;
			for (CommandNode<CommandSourceStack> child : node.getChildren()) {
				if (child.getName().equals(step)) {
					next = child;
					break;
				}
			}
			node = next;
		}
		return node;
	}

	/**
	 * Encodes root -> literal("x") -> argument("a") whose type id no client knows. An unknown id
	 * makes the decoder return a null stub without consuming further bytes, so the rest of the
	 * payload stays valid.
	 */
	private static byte[] craftUnresolvablePayload() {
		final int unknownTypeId = 0x7F; // one-byte varint, well above the real registry size

		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeVarInt(3); // entry count

		b.writeByte(0x00);                       // entry 0 - root, one child
		b.writeVarIntArray(new int[]{1});

		b.writeByte(0x01);                       // entry 1 - literal "x", one child
		b.writeVarIntArray(new int[]{2});
		b.writeUtf("x");

		b.writeByte(0x02);                       // entry 2 - argument "a", unknown type
		b.writeVarIntArray(new int[0]);
		b.writeUtf("a");
		b.writeVarInt(unknownTypeId);

		b.writeVarInt(0);                        // root index
		byte[] payload = new byte[b.readableBytes()];
		b.getBytes(b.readerIndex(), payload);
		return payload;
	}

	/**
	 * Collects the argument types the tree uses, flagging any that is not vanilla.
	 * <p>
	 * The namespace matters, not merely the presence of a registry id: a mod-registered type does
	 * have an id on the server, and it is exactly that id a vanilla client cannot resolve.
	 */
	private static void collectArgumentTypes(CommandNode<CommandSourceStack> node,
											 TreeSet<String> seen, List<String> unresolved) {
		if (node instanceof ArgumentCommandNode<CommandSourceStack, ?> argumentNode) {
			ArgumentType<?> type = argumentNode.getType();
			if (seen.add(type.getClass().getName())) {
				try {
					ArgumentTypeInfo<?, ?> info = ArgumentTypeInfos.byClass(type);
					Identifier key = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(info);
					if (key == null || !VANILLA_NAMESPACES.contains(key.getNamespace())) {
						unresolved.add(type.getClass().getName() + " (registered as " + key + ")");
					}
				} catch (Throwable t) {
					unresolved.add(type.getClass().getName() + " (" + t + ")");
				}
			}
		}
		for (CommandNode<CommandSourceStack> child : node.getChildren()) {
			collectArgumentTypes(child, seen, unresolved);
		}
	}

	/** Mirrors how the server builds the packet; suggestion ids and restriction flags are irrelevant. */
	private static final class Inspector implements ClientboundCommandsPacket.NodeInspector<CommandSourceStack> {
		@Override
		public Identifier suggestionId(ArgumentCommandNode<CommandSourceStack, ?> node) {
			return null;
		}

		@Override
		public boolean isExecutable(CommandNode<CommandSourceStack> node) {
			return node.getCommand() != null;
		}

		@Override
		public boolean isRestricted(CommandNode<CommandSourceStack> node) {
			return false;
		}
	}

	/** Mirrors the vanilla client's NodeBuilder. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static final class ClientNodeBuilder implements ClientboundCommandsPacket.NodeBuilder<Object> {
		@Override
		public ArgumentBuilder<Object, ?> createLiteral(String id) {
			return (ArgumentBuilder) Commands.literal(id);
		}

		@Override
		public ArgumentBuilder<Object, ?> createArgument(String id, ArgumentType<?> type, Identifier suggestionId) {
			return (ArgumentBuilder) Commands.argument(id, (ArgumentType) type);
		}

		@Override
		public ArgumentBuilder<Object, ?> configure(ArgumentBuilder<Object, ?> input, boolean executable, boolean restricted) {
			if (executable) {
				((ArgumentBuilder) input).executes(ctx -> 1);
			}
			return input;
		}
	}
}
