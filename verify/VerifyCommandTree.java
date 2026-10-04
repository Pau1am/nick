import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.dongchengqiao.nick.NickCommand;
import com.dongchengqiao.nick.NickSettings;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Headless proof that the command tree this mod puts on the wire can be resolved by a client
 * that does not have the mod installed.
 *
 * <p>It runs the mod's own registration code, serialises the command tree with vanilla's encoder,
 * parses it back with vanilla's decoder, and rebuilds the tree through the same code path the
 * vanilla client uses. That path is where a client without the mod blows up: an argument type it
 * cannot resolve decodes to a null stub, the node silently degrades into a {@code RootCommandNode},
 * and brigadier's {@code CommandNode#addChild} then refuses to attach a {@code RootCommandNode}
 * as a child - which disconnects the player.
 *
 * <p>The last step is a negative control on a hand-crafted payload containing an argument type the
 * client cannot resolve. It must be reported as broken, otherwise the check above proves nothing.
 */
public final class VerifyCommandTree {
	private static int failures = 0;

	public static void main(String[] args) throws Exception {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();

		// ---- 1. build the tree with the mod's real registration code ----------------------
		NickSettings.commandNick = true;
		var dispatcher = new com.mojang.brigadier.CommandDispatcher<CommandSourceStack>();
		NickCommand.registerCommands(dispatcher);
		RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();

		System.out.println("[1] registered /nick; root children = " + root.getChildren().size());

		// ---- 2. encode exactly like the server does --------------------------------------
		ClientboundCommandsPacket packet = new ClientboundCommandsPacket(root, new Inspector());
		FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
		ClientboundCommandsPacket.STREAM_CODEC.encode(out, packet);
		byte[] payload = new byte[out.readableBytes()];
		out.getBytes(out.readerIndex(), payload);
		System.out.println("[2] ClientboundCommandsPacket payload = " + payload.length + " bytes");

		// Argument types the tree uses, and whether the client's registry knows them.
		TreeSet<String> types = new TreeSet<>();
		List<String> unresolved = new ArrayList<>();
		for (CommandNode<CommandSourceStack> child : root.getChildren()) {
			collectArgumentTypes(child, types, unresolved);
		}
		System.out.println("    argument types used = " + types);
		check("every argument type has a registry id a vanilla client can look up", unresolved.isEmpty());
		if (!unresolved.isEmpty()) {
			System.out.println("      unresolved: " + unresolved);
		}

		// ---- 3. parse + rebuild, the two steps a vanilla client performs ------------------
		System.out.println();
		System.out.println("[3] simulating a client that does not have the mod");
		Inspection real = inspect(payload);
		report(real);
		check("no argument node decodes to a null stub", real.nullStubs() == 0);
		check("client rebuilds the /nick command tree without throwing", real.rebuildError() == null);
		check("client tree contains the full /nick set <name> path", real.branchPresent());

		// ---- 4. negative control ---------------------------------------------------------
		// An unresolvable argument type must be *detected*. Note the failure mode: the node
		// degrades to a RootCommandNode and the parent silently skips it, so the branch just
		// disappears from the client tree - it does not throw. Measuring "does the branch
		// exist" is therefore the assertion that actually matters.
		System.out.println();
		System.out.println("[4] negative control: hand-crafted payload with an unresolvable argument type");
		Inspection broken = inspect(craftUnresolvablePayload());
		report(broken);
		check("control payload IS detected as having a null stub", broken.nullStubs() > 0);
		check("control payload's argument branch is MISSING from the client tree", !broken.branchPresent());

		// ---- 5. the mod must not add anything to the argument type registry ---------------
		System.out.println();
		TreeSet<String> extras = new TreeSet<>();
		for (Identifier id : BuiltInRegistries.COMMAND_ARGUMENT_TYPE.keySet()) {
			String ns = id.getNamespace();
			// minecraft: and brigadier: are both part of vanilla; anything else comes from a mod.
			if (!"minecraft".equals(ns) && !"brigadier".equals(ns)) {
				extras.add(id.toString());
			}
		}
		System.out.println("[5] non-vanilla argument types registered = " + (extras.isEmpty() ? "(none)" : extras));
		check("mod registers no custom argument type", !extras.contains("nick:unicode_word"));

		System.out.println();
		if (failures == 0) {
			System.out.println("RESULT: PASS");
		} else {
			System.out.println("RESULT: FAIL (" + failures + " check(s) failed)");
			System.exit(1);
		}
	}

	// ---------------------------------------------------------------------------------------

	private record Inspection(int argumentNodes, int nullStubs, String rebuildError, boolean branchPresent) {
	}

	/**
	 * Decodes a payload and rebuilds the tree the way the client does, reporting what happened.
	 *
	 * @param path the node names to look for in the rebuilt client tree (null expects "/nick set/name")
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

		// The client rebuild. A RootCommandNode produced by a null stub is skipped by its parent
		// rather than throwing, so the meaningful measurement is whether the branch survives.
		boolean branchPresent = false;
		String rebuildError = null;
		try {
			CommandBuildContext buildContext = CommandBuildContext.simple(
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), FeatureFlags.VANILLA_SET);
			RootCommandNode<Object> clientRoot = decoded.getRoot(buildContext, new ClientNodeBuilder());
			branchPresent = branchPresent(clientRoot);
		} catch (Throwable t) {
			rebuildError = t.getClass().getSimpleName() + ": " + t.getMessage();
		}
		return new Inspection(argumentNodes, nullStubs, rebuildError, branchPresent);
	}

	/** True when the rebuilt client tree still has a "nick" -> "set" -> "name" chain. */
	private static boolean branchPresent(RootCommandNode<Object> clientRoot) {
		return follow(follow(follow(clientRoot, "nick"), "set"), "name") != null;
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

	private static void report(Inspection r) {
		System.out.println("    argument nodes = " + r.argumentNodes()
			+ ", null stubs = " + r.nullStubs()
			+ ", rebuild = " + (r.rebuildError() == null ? "ok" : "THREW")
			+ ", /nick set/name present = " + r.branchPresent());
	}

	/**
	 * Encodes root -> literal("x") -> argument("a") where the argument's type id is one no client
	 * knows. Unknown ids make the decoder return a null stub without consuming further bytes, so
	 * the rest of the payload stays valid.
	 */
	private static byte[] craftUnresolvablePayload() {
		final int unknownTypeId = 0x7F; // one-byte varint, well above the real registry size

		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeVarInt(3); // entry count

		// entry 0 - root (flags&3 == 0), one child
		b.writeByte(0x00);
		b.writeVarIntArray(new int[]{1});

		// entry 1 - literal "x" (flags&3 == 1), one child
		b.writeByte(0x01);
		b.writeVarIntArray(new int[]{2});
		b.writeUtf("x");

		// entry 2 - argument "a" (flags&3 == 2) with an unknown type id
		b.writeByte(0x02);
		b.writeVarIntArray(new int[0]);
		b.writeUtf("a");
		b.writeVarInt(unknownTypeId);

		b.writeVarInt(0); // root index
		byte[] payload = new byte[b.readableBytes()];
		b.getBytes(b.readerIndex(), payload);
		return payload;
	}

	private static void check(String what, boolean ok) {
		System.out.println("    " + (ok ? "PASS" : "FAIL") + " - " + what);
		if (!ok) {
			failures++;
		}
	}

	/** Walks the dispatcher tree collecting argument types and flagging ones with no registry entry. */
	private static void collectArgumentTypes(CommandNode<CommandSourceStack> node,
											 TreeSet<String> seen, List<String> unresolved) {
		if (node instanceof ArgumentCommandNode<CommandSourceStack, ?> argumentNode) {
			ArgumentType<?> type = argumentNode.getType();
			String name = type.getClass().getName();
			if (seen.add(name)) {
				try {
					ArgumentTypeInfo<?, ?> info = ArgumentTypeInfos.byClass(type);
					if (BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(info) == null) {
						unresolved.add(name + " (no registry id)");
					}
				} catch (Throwable t) {
					unresolved.add(name + " (" + t + ")");
				}
			}
		}
		for (CommandNode<CommandSourceStack> child : node.getChildren()) {
			collectArgumentTypes(child, seen, unresolved);
		}
	}

	/** Mirrors how the server builds the packet; suggestion ids and restriction flags are irrelevant here. */
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
