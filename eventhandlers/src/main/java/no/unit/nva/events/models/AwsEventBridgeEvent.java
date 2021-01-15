package no.unit.nva.events.models;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class AwsEventBridgeEvent<I> implements JsonSerializable {

    @JsonProperty("version")
    private String version;
    @JsonProperty("id")
    private String id;

    @JsonProperty("detail-type")
    private String detailType;
    @JsonProperty("source")
    private String source;
    @JsonProperty("account")
    private String account;
    @JsonProperty("time")
    private Instant time;
    // look at getter.
    private Regions region;
    @JsonProperty("resources")
    private Collection<String> resources;

    @JsonProperty("detail")
    private I detail;

    @JacocoGenerated
    public AwsEventBridgeEvent() {
        super();
    }

    @JacocoGenerated
    @JsonProperty("region")
    public String getRegion() {
        return region == null ? null : region.getName();
    }

    @JacocoGenerated
    public void setRegion(String region) {
        this.region = region == null ? null : Regions.fromName(region);
    }

    @JacocoGenerated
    public void setRegion(Regions region) {
        this.region = region;
    }

    @JacocoGenerated
    public String getVersion() {
        return version;
    }

    @JacocoGenerated
    public void setVersion(String version) {
        this.version = version;
    }

    @JacocoGenerated
    public String getId() {
        return id;
    }

    @JacocoGenerated
    public void setId(String id) {
        this.id = id;
    }

    @JacocoGenerated
    public String getDetailType() {
        return detailType;
    }

    @JacocoGenerated
    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    @JacocoGenerated
    public String getSource() {
        return source;
    }

    @JacocoGenerated
    public void setSource(String source) {
        this.source = source;
    }

    @JacocoGenerated
    public String getAccount() {
        return account;
    }

    @JacocoGenerated
    public void setAccount(String account) {
        this.account = account;
    }

    @JacocoGenerated
    public Instant getTime() {
        return time;
    }

    @JacocoGenerated
    public void setTime(Instant time) {
        this.time = time;
    }

    @JacocoGenerated
    public Collection<String> getResources() {
        return resources;
    }

    @JacocoGenerated
    public void setResources(Collection<String> resources) {
        this.resources = resources;
    }

    @JacocoGenerated
    public I getDetail() {
        return detail;
    }

    @JacocoGenerated
    public void setDetail(I detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsEventBridgeEvent<?> that = (AwsEventBridgeEvent<?>) o;
        return Objects.equals(getVersion(), that.getVersion())
            && Objects.equals(getId(), that.getId())
            && Objects.equals(getDetailType(), that.getDetailType())
            && Objects.equals(getSource(), that.getSource())
            && Objects.equals(getAccount(), that.getAccount())
            && Objects.equals(getTime(), that.getTime())
            && getRegion() == that.getRegion()
            && Objects.equals(getResources(), that.getResources())
            && Objects.equals(getDetail(), that.getDetail());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getVersion(), getId(), getDetailType(), getSource(), getAccount(), getTime(), getRegion(),
            getResources(), getDetail());
    }
}
