package vg.skye.prefabricated;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.*;

import java.util.*;

public record ModMetadataImpl (
        String id,
        List<String> provides,
        String version,
        String name,
        String description
) implements ModMetadata {

    @Override
    public String getType() {
        return "fabric";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<String> getProvides() {
        return provides;
    }

    @Override
    public Version getVersion() {
        return new VersionImpl(version);
    }

    @Override
    public ModEnvironment getEnvironment() {
        return ModEnvironment.UNIVERSAL;
    }

    @Override
    public Collection<ModDependency> getDependencies() {
        return List.of();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Collection<Person> getAuthors() {
        return List.of();
    }

    @Override
    public Collection<Person> getContributors() {
        return List.of();
    }

    @Override
    public ContactInformation getContact() {
        return ContactInformation.EMPTY;
    }

    @Override
    public Collection<String> getLicense() {
        return List.of();
    }

    @Override
    public Optional<String> getIconPath(int size) {
        return Optional.empty();
    }

    @Override
    public boolean containsCustomValue(String key) {
        return false;
    }

    @Override
    public CustomValue getCustomValue(String key) {
        return null;
    }

    @Override
    public Map<String, CustomValue> getCustomValues() {
        return Collections.emptyMap();
    }

    @Override
    public boolean containsCustomElement(String key) {
        return false;
    }
}
