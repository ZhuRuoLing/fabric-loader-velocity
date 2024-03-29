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

package net.fabricmc.loader.impl.game.velocity;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.VelocityServer;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.game.velocity.patch.EntrypointPatch;
import net.fabricmc.loader.impl.game.velocity.patch.TerminalConsoleAppenderPatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ModDependencyImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogHandler;

public class VelocityGameProvider implements GameProvider {

	//
	private static final String[] ALLOWED_EARLY_CLASS_PREFIXES = {
			"org.apache.logging.log4j.",
			"com.velocitypowered.",
			"com.velocitypowered.",
			"com.lmax.disruptor.",
			"net.minecrell.",
			"org.jline.reader."
	};

	private static final Set<String> SENSITIVE_ARGS = new HashSet<>(Arrays.asList(
			// all lowercase without --
			));
	
	private ProxyVersion proxyVersion;
	private final List<Path> gameJars = new ArrayList<>(2);
	private final Set<Path> logJars = new HashSet<>();
	private final List<Path> miscGameLibraries = new ArrayList<>();
	private Collection<Path> validParentClassPath; // computed parent class path restriction (loader+deps)
	private boolean log4jAvailable;
	private boolean slf4jAvailable;
	private String entrypoint;
	private Arguments arguments;
	private final GameTransformer transformer = new GameTransformer(new EntrypointPatch(), new TerminalConsoleAppenderPatch());

	@Override
	public String getGameId() {
		return "velocity";
	}

	@Override
	public String getGameName() {
		return "Velocity";
	}

	@Override
	public String getRawGameVersion() {
		return proxyVersion.getVersion();
	}

	@Override
	public String getNormalizedGameVersion() {
		return proxyVersion.getVersion();
	}

	@Override
	public Collection<BuiltinMod> getBuiltinMods() {
		BuiltinModMetadata.Builder metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
				.setName(getGameName());
		try {
			metadata.addDependency(new ModDependencyImpl(ModDependency.Kind.DEPENDS, "java", Collections.singletonList(">=17")));
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}

		return Collections.singletonList(new BuiltinMod(gameJars, metadata.build()));
	}

	@Override
	public String getEntrypoint() {
		return entrypoint;
	}

	@Override
	public Path getLaunchDirectory() {
		if (arguments == null) {
			return Paths.get(".");
		}
		
		return Paths.get(arguments.getOrDefault("gameDir", "."));
	}

	@Override
	public boolean isObfuscated() {
		return false;
	}

	@Override
	public boolean requiresUrlClassLoader() {
		return false;
	}

	@Override
	public boolean isEnabled() {
		return System.getProperty("fabric.skipVcProvider") == null;
	}

	@Override
	public boolean locateGame(FabricLauncher launcher, String[] args) {
		this.arguments = new Arguments();
		arguments.parse(args);
		try {
			LibClassifier<VcLibrary> classifier = new LibClassifier<>(VcLibrary.class, EnvType.SERVER, this);
			classifier.process(launcher.getClassPath());
			entrypoint = classifier.getClassName(VcLibrary.VELOCITY);
//			entrypoint = "com.velocitypowered.proxy.Velocity";
			log4jAvailable = classifier.has(VcLibrary.LOG4J_API) && classifier.has(VcLibrary.LOG4J_CORE);
			slf4jAvailable = classifier.has(VcLibrary.SLF4J_API) && classifier.has(VcLibrary.SLF4J_CORE);
			boolean hasLogLib = log4jAvailable || slf4jAvailable;
			gameJars.add(classifier.getOrigin(VcLibrary.VELOCITY));
			Log.configureBuiltin(hasLogLib, !hasLogLib);
			for (VcLibrary lib : VcLibrary.LOGGING) {
				Path path = classifier.getOrigin(lib);

				if (path != null) {
					if (hasLogLib) {
						logJars.add(path);
					} else if (!gameJars.contains(path)) {
						miscGameLibraries.add(path);
					}
				}
			}
			miscGameLibraries.addAll(classifier.getUnmatchedOrigins());
			validParentClassPath = classifier.getSystemLibraries();
		}catch (IOException e) {
			throw ExceptionUtil.wrap(e);
		}
		lookupVersion();
		return true;
	}

	private void lookupVersion(){
		Package pkg = VelocityServer.class.getPackage();
		String implName;
		String implVersion;
		String implVendor;
		if (pkg != null) {
			implName = MoreObjects.firstNonNull(pkg.getImplementationTitle(), "Velocity");
			implVersion = MoreObjects.firstNonNull(pkg.getImplementationVersion(), "3.3.0");
			implVendor = MoreObjects.firstNonNull(pkg.getImplementationVendor(), "Velocity Contributors");
		} else {
			implName = "Velocity";
			implVersion = "3.3.0";
			implVendor = "Velocity Contributors";
		}
		implVersion = implVersion.substring(0,implVersion.indexOf('-'));
		proxyVersion = new ProxyVersion(implName, implVendor, implVersion);
	}

	@Override
	public void initialize(FabricLauncher launcher) {
		// some weird workarounds
		System.setProperty("fabric.debug.disableClassPathIsolation","");

		launcher.setValidParentClassPath(validParentClassPath);
		if (!logJars.isEmpty() && !Boolean.getBoolean(SystemProperties.UNIT_TEST)) {
			for (Path jar : logJars) {
				if (gameJars.contains(jar)) {
					launcher.addToClassPath(jar, ALLOWED_EARLY_CLASS_PREFIXES);
				} else {
					launcher.addToClassPath(jar);
				}
			}
		}

		setupLogHandler(launcher, true);
		transformer.locateEntrypoints(launcher, gameJars);
	}

	private void setupLogHandler(FabricLauncher launcher, boolean useTargetCl) {
		System.setProperty("log4j2.formatMsgNoLookups", "true"); // lookups are not used by mc and cause issues with older log4j2 versions

		try {
			final String logHandlerClsName;

			if (log4jAvailable) {
				logHandlerClsName = "net.fabricmc.loader.impl.game.velocity.Log4jLogHandler";
			} else if (slf4jAvailable) {
				logHandlerClsName = "net.fabricmc.loader.impl.game.velocity.Slf4jLogHandler";
			} else {
				return;
			}

			ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
			Class<?> logHandlerCls;

			if (useTargetCl) {
				Thread.currentThread().setContextClassLoader(launcher.getTargetClassLoader());
				logHandlerCls = launcher.loadIntoTarget(logHandlerClsName);
			} else {
				logHandlerCls = Class.forName(logHandlerClsName);
			}

			Log.init((LogHandler) logHandlerCls.getConstructor().newInstance());
			Thread.currentThread().setContextClassLoader(prevCl);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GameTransformer getEntrypointTransformer() {
		return transformer;
	}

	@Override
	public void unlockClassPath(FabricLauncher launcher) {
		for (Path gameJar : gameJars) {
			launcher.getKnotClassLoaderDelegate().setAllowedPrefixes(gameJar);
			launcher.addToClassPath(gameJar);
		}
		// how???
//		for (Path lib : miscGameLibraries) {
//			launcher.addToClassPath(lib);
//			launcher.getKnotClassLoaderDelegate().addCodeSource(lib);
//			launcher.getKnotClassLoaderDelegate().setAllowedPrefixes(lib, new String[0]);
//		}
	}

	@Override
	public void launch(ClassLoader loader) {
		String targetClass = entrypoint;
		MethodHandle invoker;

		try {
			Class<?> c = loader.loadClass(targetClass);
			invoker = MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class));
		} catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
			throw FormattedException.ofLocalized("exception.minecraft.invokeFailure", e);
		}

		try {
			invoker.invokeExact(arguments.toArray());
		} catch (Throwable t) {
			throw FormattedException.ofLocalized("exception.minecraft.generic", t);
		}
	}

	@Override
	public Arguments getArguments() {
		return arguments;
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		if (arguments == null) return new String[0];

		String[] ret = arguments.toArray();
		if (!sanitize) return ret;

		int writeIdx = 0;

		for (int i = 0; i < ret.length; i++) {
			String arg = ret[i];

			if (i + 1 < ret.length
					&& arg.startsWith("--")
					&& SENSITIVE_ARGS.contains(arg.substring(2).toLowerCase(Locale.ENGLISH))) {
				i++; // skip value
			} else {
				ret[writeIdx++] = arg;
			}
		}

		if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

		return ret;
	}
}
