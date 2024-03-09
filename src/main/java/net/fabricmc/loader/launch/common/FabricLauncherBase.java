package net.fabricmc.loader.launch.common;

// Dummy class+interface for Cardinal Components API 5.2.2
public class FabricLauncherBase implements FabricLauncher {
    public static FabricLauncher getLauncher() {
        return new FabricLauncherBase();
    }
}