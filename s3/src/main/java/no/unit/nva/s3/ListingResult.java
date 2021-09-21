package no.unit.nva.s3;

import java.util.List;
import nva.commons.core.paths.UnixPath;

public class ListingResult {

    private final String listingStartingPoint;
    private final List<UnixPath> files;
    private final boolean truncated;

    public ListingResult(List<UnixPath> files, String listingStartingPoint, boolean isTruncated) {
        this.listingStartingPoint = listingStartingPoint;
        this.files = files;
        this.truncated = isTruncated;
    }

    public String getListingStartingPoint() {
        return listingStartingPoint;
    }

    public List<UnixPath> getFiles() {
        return files;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
