package vg.skye.prefabricated;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import vg.skye.prefabricated.metadata.DependencyOverrides;
import vg.skye.prefabricated.metadata.ModMetadataParser;
import vg.skye.prefabricated.metadata.VersionOverrides;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ModContainerImpl implements ModContainer {
    private final Path root;
    private ModMetadata metadata;

    public ModContainerImpl(Path root) {
        this.root = root;
    }

    private static ModMetadata parseMetadata(Path root) {
        try {
            Path path = root.resolve("fabric.mod.json");
            try (InputStream is = Files.newInputStream(path)) {
                return ModMetadataParser.parseMetadata(
                        is,
                        root.toString(),
                        List.of(),
                        new VersionOverrides(),
                        new DependencyOverrides(FabricLoader.getInstance().getConfigDir()),
                        false
                );
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ModMetadata getMetadata() {
        if (metadata == null) {
            metadata = parseMetadata(root);
        }
        return metadata;
    }

    @Override
    public List<Path> getRootPaths() {
        return List.of(root);
    }

    @Override
    public ModOrigin getOrigin() {
        return new ModOriginImpl();
    }

    @Override
    public Optional<ModContainer> getContainingMod() {
        return Optional.empty();
    }

    @Override
    public Collection<ModContainer> getContainedMods() {
        return Collections.emptyList();
    }

    @Override
    public Path getRootPath() {
        return root;
    }

    @Override
    public Path getPath(String file) {
        return root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
    }
}
