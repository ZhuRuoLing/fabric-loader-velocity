fabric-loader-velocity
===========

The loader for mods under Fabric. It provides mod loading facilities and useful abstractions for other mods to use.

## License

Licensed under the Apache License 2.0.

## Velocity

### How To Use

- Download latest release from [release page](https://github.com/ZhuRuoLing/fabric-loader-velocity/releases) of this project
- Download a velocity release from [here](https://papermc.io/downloads/velocity)
- Decompress release zip of this project
- Copy velocity jar into `./libs/`
- Configure velocity jar name in `start.bat` or `start.sh`
- Start velocity server using `start.bat` or `start.sh`

The finished directory structure may look like the following dir tree

```text
.
├── libs
│   ├── access-widener-2.1.0.jar
│   ├── asm-9.6.jar
│   ├── asm-analysis-9.6.jar
│   ├── asm-commons-9.6.jar
│   ├── asm-tree-9.6.jar
│   ├── asm-util-9.6.jar
│   ├── fabric-loader-0.15.7+local-fat.jar
│   ├── velocity-3.3.0-SNAPSHOT-371.jar
│   ├── gson-2.2.4.jar
│   ├── guava-21.0.jar
│   ├── mapping-io-0.5.0.jar
│   ├── mixinextras-fabric-0.3.5.jar
│   ├── org.ow2.sat4j.core-2.3.6.jar
│   ├── org.ow2.sat4j.pb-2.3.6.jar
│   ├── sponge-mixin-0.12.5+mixin.0.8.5.jar
│   └── tiny-remapper-0.10.1.jar
├── README.md
├── start.bat
└── start.sh

1 directory, 19 files
```

Set your velocity jar name into system property `fabric.systemLibraryExclusion` as jvm argument

The content of start.bat may looks like following:
```batch
@echo off
java "-Dfabric.systemLibraryExclusion=velocity-3.3.0-SNAPSHOT-371.jar" "-Dfabric.debug.disableClassPathIsolation" -cp "./libs/*" net.fabricmc.loader.impl.launch.knot.KnotServer
pause
```

The content of start.sh may looks like following:

```shell
#!/bin/sh
java "-Dfabric.systemLibraryExclusion=velocity-3.3.0-SNAPSHOT-371.jar" "-Dfabric.debug.disableClassPathIsolation" -cp "./libs/*" net.fabricmc.loader.impl.launch.knot.KnotServer
```

### How To Development Mods for velocity

You may use the mod template from [here](https://github.com/ZhuRuoLing/fabric-example-mod-velocity)

### Problems
- Detect velocity version

> Velocity determines its version from `VelocityServer.class.getPackage().getImplementationVersion()`   
> In fabric environment, the retrieved version may not accurate.  
> Set system property `fabric.velocityVersionOverride` can override the detected version in both velocity side and fabric side.

