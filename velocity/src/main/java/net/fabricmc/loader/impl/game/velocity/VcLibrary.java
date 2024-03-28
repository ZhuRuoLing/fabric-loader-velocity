package net.fabricmc.loader.impl.game.velocity;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier.LibraryType;

public enum VcLibrary implements LibraryType {
	GAME_PROVIDER("net/fabricmc/loader/impl/game/velocity/VelocityGameProvider.class"),
	VELOCITY("com/velocitypowered/proxy/Velocity.class"),
	LOG4J_API("org/apache/logging/log4j/LogManager.class"),
	LOG4J_CORE("META-INF/services/org.apache.logging.log4j.spi.Provider", "META-INF/log4j-provider.properties"),
	LOG4J_CONFIG("log4j2.xml"),
//	LOG4J_PLUGIN_2("com/mojang/patchy/LegacyXMLLayout.class"), // in patchy
	LOG4J_PLUGIN_3("net/minecrell/terminalconsole/util/LoggerNamePatternSelector.class"), // in terminalconsoleappender, used by loom's log4j config
	GSON("com/google/gson/TypeAdapter.class"), // used by log4j plugins
	SLF4J_API("org/slf4j/Logger.class"),
	SLF4J_CORE("META-INF/services/org.slf4j.spi.SLF4JServiceProvider");

	static final VcLibrary[] LOGGING = { LOG4J_API, LOG4J_CORE, LOG4J_CONFIG,/* LOG4J_PLUGIN_2,*/ LOG4J_PLUGIN_3, GSON, SLF4J_API, SLF4J_CORE };
	private final String[] paths;

	VcLibrary(String path) {
		this(new String[] { path });
	}

	VcLibrary(String... paths) {
		this.paths = paths;
	}

	@Override
	public boolean isApplicable(EnvType env) {
		return true;
	}

	@Override
	public String[] getPaths() {
		return paths;
	}
}
