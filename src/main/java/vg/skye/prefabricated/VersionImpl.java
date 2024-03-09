package vg.skye.prefabricated;

import net.fabricmc.loader.api.Version;

public record VersionImpl(String version) implements Version {
    @Override
    public String getFriendlyString() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VersionImpl) {
            return version.equals(((VersionImpl) obj).version);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(Version o) {
        return version.compareTo(o.getFriendlyString());
    }
}
