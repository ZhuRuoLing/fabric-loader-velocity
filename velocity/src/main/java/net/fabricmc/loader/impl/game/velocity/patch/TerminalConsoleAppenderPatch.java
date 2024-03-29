/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.loader.impl.game.velocity.patch;

import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.function.Consumer;
import java.util.function.Function;

public class TerminalConsoleAppenderPatch extends GamePatch {
	@Override
	public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
		ClassNode node = classSource.apply("net.minecrell.terminalconsole.TerminalConsoleAppender");
		if (node != null){
			boolean patched = false;
			for (MethodNode method : node.methods) {
				if (method.name.equals("createAppender") && method.desc.equals("(Ljava/lang/String;Lorg/apache/logging/log4j/core/Filter;Lorg/apache/logging/log4j/core/Layout;Z)Lnet/minecrell/terminalconsole/TerminalConsoleAppender;")){
					var it = method.instructions.iterator();
					it.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
					it.add(new VarInsnNode(Opcodes.ILOAD, 3));
					it.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream","println","(Z)V"));
					it.add(new InsnNode(Opcodes.ICONST_0));
					it.add(new VarInsnNode(Opcodes.ISTORE, 3));
					patched = true;
				}
				if (method.name.equals("<init>")){
					var it = method.instructions.iterator();
					while (it.hasNext()){
						var insn = it.next();
						if (insn instanceof VarInsnNode varInsnNode){
							if (varInsnNode.getOpcode() == Opcodes.ILOAD){
								it.add(new InsnNode(Opcodes.POP));
								it.add(new InsnNode(Opcodes.ICONST_0));
								break;
							}
						}
					}
					patched = true;
				}
			}
			if (patched){
				classEmitter.accept(node);
			}
		}
	}
}
