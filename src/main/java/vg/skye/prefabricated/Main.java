package vg.skye.prefabricated;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class Main {
    public static void main(String[] args) throws Throwable {
        var loader = FabricLoaderImpl.INSTANCE;
        downBadModsCompat();
        loader.invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
        var c = Class.forName(loader.entrypoint);
        var invoker = MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class));
        invoker.invoke((Object) args);
    }

    private static void downBadModsCompat() {
        // ModernFix - needs ModernFixMixinPlugin initialized
        try {
            var modernFixBad = Class.forName("org.embeddedt.modernfix.core.ModernFixMixinPlugin", true, ClassLoader.getSystemClassLoader());
            modernFixBad.getDeclaredConstructor().newInstance();
            System.err.println("ModernFix compat engaged!");
        } catch (Exception e) {
            if (FabricLoader.getInstance().isModLoaded("modernfix")) {
                System.err.println("ModernFix is loaded but compat failed!");
                e.printStackTrace();
            }
        }
        // ImmediatelyFast - needs ImmediatelyFast.earlyInit called
        try {
            var immediatelyFastBad = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast", true, ClassLoader.getSystemClassLoader());
            immediatelyFastBad.getMethod("earlyInit").invoke(null);
            System.err.println("ImmediatelyFast compat engaged!");
        } catch (Exception e) {
            if (FabricLoader.getInstance().isModLoaded("immediatelyfast")) {
                System.err.println("ImmediatelyFast is loaded but compat failed!");
                e.printStackTrace();
            }
        }
    }
}