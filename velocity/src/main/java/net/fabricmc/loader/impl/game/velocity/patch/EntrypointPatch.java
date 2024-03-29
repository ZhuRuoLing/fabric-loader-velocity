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

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.game.velocity.Hooks;
import net.fabricmc.loader.impl.launch.FabricLauncher;

public class EntrypointPatch extends GamePatch {
	@Override
	public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
		ClassNode node = classSource.apply("com.velocitypowered.proxy.VelocityServer");
		boolean patched = false;
		if (node != null) {
			for (MethodNode methodNode : node.methods) {
				if (methodNode.name.equals("start") && methodNode.desc.equals("()V")) {
					ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator();
					it.add(new InsnNode(Opcodes.ACONST_NULL));
					it.add(new VarInsnNode(
							Opcodes.ALOAD,
							0
					));
					it.add(new MethodInsnNode(
							Opcodes.INVOKESTATIC,
							Hooks.INTERNAL_NAME,
							"startServer",
							"(Ljava/io/File;Ljava/lang/Object;)V"
					));
					patched = true;
				}
			}
		}
		if (patched){
			classEmitter.accept(node);
		}
	}
}
