package vg.skye.prefabricated;

import net.fabricmc.loader.api.metadata.ModOrigin;

import java.nio.file.Path;
import java.util.List;

public class ModOriginImpl implements ModOrigin {
    @Override
    public Kind getKind() {
        return Kind.UNKNOWN;
    }

    @Override
    public List<Path> getPaths() {
        throw new UnsupportedOperationException("We don't have proper ModOrigin information");
    }

    @Override
    public String getParentModId() {
        throw new UnsupportedOperationException("We don't have proper ModOrigin information");
    }

    @Override
    public String getParentSubLocation() {
        throw new UnsupportedOperationException("We don't have proper ModOrigin information");
    }
}
