package vg.skye.prefabricated;

import com.google.gson.Gson;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.*;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class FabricLoaderImpl implements FabricLoader {
    private record Entrypoint(String mod, String adapter, String value) {}
    private final Map<String, List<Entrypoint>> entrypoints;
    private final Map<String, LanguageAdapter> languageAdapters;
    private final Map<Entrypoint, Object> entrypointsCache = new HashMap<>();
    private final ObjectShare objectShare = new ObjectShareImpl();
    private final MemoryMappingTree mappings = new MemoryMappingTree();
    private final List<ModContainer> mods;
    private final EnvType envType;
    private MappingResolver mappingResolver;
    private Path gameDir = Path.of(".").toAbsolutePath();
    private Object gameInstance;

    private static final Gson gson = new Gson();
    private record FrozenMeta(
            Map<String, List<Entrypoint>> entrypoints,
            Map<String, String> languageAdapters,
            List<String> mods,
            String envType,
            String entrypoint
    ) {}

    public static FabricLoaderImpl INSTANCE = deserialize();

    public final String entrypoint;

    private final Map<String, String> frozenLanguageAdapters;
    private boolean languageAdaptersInitialized = false;

    private static FabricLoaderImpl deserialize() {
        try {
            var frozen = gson.fromJson(Files.newBufferedReader(Path.of("prefabricated_frozen.json"), StandardCharsets.UTF_8), FrozenMeta.class);
            List<ModContainer> mods = new ArrayList<>(frozen.mods.size());
            for (var entry : frozen.mods) {
                var path = Path.of("mod_assets", entry).toAbsolutePath();
                mods.add(new ModContainerImpl(path));
            }
            if (frozen.envType.equals("client")) {
                return new FabricLoaderImpl(frozen.entrypoints, frozen.languageAdapters, mods, EnvType.CLIENT, frozen.entrypoint);
            } else {
                return new FabricLoaderImpl(frozen.entrypoints, frozen.languageAdapters, mods, EnvType.SERVER, frozen.entrypoint);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FabricLoaderImpl(
            Map<String, List<Entrypoint>> entrypoints,
            Map<String, String> frozenLanguageAdapters,
            List<ModContainer> mods,
            EnvType envType,
            String entrypoint
    ) {
        this.entrypoints = entrypoints;
        this.languageAdapters = new HashMap<>(frozenLanguageAdapters.size());
        this.mods = mods;
        this.envType = envType;
        this.entrypoint = entrypoint;
        this.frozenLanguageAdapters = frozenLanguageAdapters;
        try {
            try (BufferedReader reader = Files.newBufferedReader(Path.of("mappings.tiny"))) {
                Tiny2FileReader.read(reader, mappings);
            }
        } catch (Exception e) {
            System.err.println("Failed to read frozen mappings!");
        }
    }

    private void maybeInitLanguageAdapters() {
        if (!languageAdaptersInitialized) {
            for (var entry : frozenLanguageAdapters.entrySet()) {
                try {
                    if (entry.getKey().equals("default")) {
                        languageAdapters.put(entry.getKey(), DefaultLanguageAdapter.INSTANCE);
                        continue;
                    }
                    var clazz = Class.forName(entry.getValue(), true, ClassLoader.getSystemClassLoader());
                    LanguageAdapter instance = (LanguageAdapter) clazz.getDeclaredConstructor().newInstance();
                    languageAdapters.put(entry.getKey(), instance);
                } catch (Exception e) {
                    System.err.println("Failed to create language adapter " + entry.getKey());
                }
            }
            languageAdaptersInitialized = true;
        }
    }

    @Override
    public <T> List<T> getEntrypoints(String key, Class<T> type) {
        maybeInitLanguageAdapters();
        var entries = entrypoints.get(key);
        if (entries == null) {
            entries = List.of();
        }
        var res = new ArrayList<T>(entries.size());
        for (var entrypoint: entries) {
            var cached = entrypointsCache.get(entrypoint);
            if (cached != null) {
                res.add((T) cached);
                continue;
            }
            var adapter = languageAdapters.get(entrypoint.adapter);
            if (adapter != null) {
                var mod = getModContainer(entrypoint.mod);
                if (mod.isEmpty()) {
                    continue;
                }
                try {
                    var adapted = adapter.create(mod.get(), entrypoint.value, type);
                    entrypointsCache.put(entrypoint, adapted);
                    res.add(adapted);
                } catch (Exception e) {
                    System.err.println("Exception while adapting entrypoint: " + e);
                }
            } else {
                System.err.println("No adapter found: " + entrypoint.adapter);
            }
        }
        return res;
    }

    @Override
    public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
        maybeInitLanguageAdapters();
        var entries = entrypoints.get(key);
        if (entries == null) {
            entries = List.of();
        }
        var res = new ArrayList<EntrypointContainer<T>>(entries.size());
        for (var entrypoint: entries) {
            var mod = getModContainer(entrypoint.mod);
            if (mod.isEmpty()) {
                continue;
            }
            var cached = entrypointsCache.get(entrypoint);
            if (cached != null) {
                res.add(new EntrypointContainer<>() {
                    @Override
                    public T getEntrypoint() {
                        return (T) cached;
                    }

                    @Override
                    public ModContainer getProvider() {
                        return mod.get();
                    }
                });
                continue;
            }
            var adapter = languageAdapters.get(entrypoint.adapter);
            if (adapter != null) {
                try {
                    var adapted = adapter.create(mod.get(), entrypoint.value, type);
                    entrypointsCache.put(entrypoint, adapted);
                    res.add(new EntrypointContainer<>() {
                        @Override
                        public T getEntrypoint() {
                            return adapted;
                        }

                        @Override
                        public ModContainer getProvider() {
                            return mod.get();
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Exception while adapting entrypoint: " + e);
                }
            } else {
                System.err.println("No adapter found: " + entrypoint.adapter);
            }
        }
        return res;
    }

    @Override
    public <T> void invokeEntrypoints(String key, Class<T> type, Consumer<? super T> invoker) {
        for (var entrypoint: getEntrypointContainers(key, type)) {
            System.err.println("Running entrypoint " + key + " from " + entrypoint.getProvider().getMetadata().getName());
            try {
                invoker.accept(entrypoint.getEntrypoint());
            } catch (Throwable e) {
                System.err.println("Entrypoint invocation failed: " + e);
            }
        }
    }

    @Override
    public ObjectShare getObjectShare() {
        return objectShare;
    }

    @Override
    public MappingResolver getMappingResolver() {
        if (mappingResolver == null) {
            final String targetNamespace = "intermediary";

            mappingResolver = new LazyMappingResolver(() -> new MappingResolverImpl(
                    mappings,
                    targetNamespace
            ), targetNamespace);
        }

        return mappingResolver;
    }

    @Override
    public Optional<ModContainer> getModContainer(String id) {
        return mods
                .stream()
                .filter(mod ->
                    mod.getMetadata().getId().equals(id)
                            || mod.getMetadata().getProvides().contains(id)
               ).findAny();
    }

    @Override
    public Collection<ModContainer> getAllMods() {
        return mods;
    }

    @Override
    public boolean isModLoaded(String id) {
        return mods
                .stream()
                .anyMatch(mod ->
                        mod.getMetadata().getId().equals(id)
                                || mod.getMetadata().getProvides().contains(id)
                );
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return false;
    }

    @Override
    public EnvType getEnvironmentType() {
        return envType;
    }

    @Override
    public Object getGameInstance() {
        return gameInstance;
    }

    @Override
    public Path getGameDir() {
        return gameDir;
    }

    @Override
    public File getGameDirectory() {
        return gameDir.toFile();
    }

    @Override
    public Path getConfigDir() {
        return gameDir.resolve("config");
    }

    @Override
    public File getConfigDirectory() {
        return gameDir.resolve("config").toFile();
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        // Lie brazenly. Nobody really had a reason to know.
        return new String[0];
    }

    public void prepareModInit(Path newRunDir, Object gameInstance) {
        this.gameInstance = gameInstance;
        this.gameDir = newRunDir;
    }

    public void setGameInstance(Object gameInstance) {
        this.gameInstance = gameInstance;
    }
}
