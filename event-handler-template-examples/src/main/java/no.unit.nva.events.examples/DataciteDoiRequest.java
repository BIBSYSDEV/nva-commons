package no.unit.nva.events.examples;

import java.net.URI;
import java.util.Objects;
import no.unit.nva.events.models.JsonSerializable;
import nva.commons.utils.JacocoGenerated;

/**
 * Example class of a "details" object.
 */
public class DataciteDoiRequest implements JsonSerializable {

    private URI publicationId;
    private URI existingDoi;
    private String someData;
    private String type;

    @JacocoGenerated
    public DataciteDoiRequest() {
    }

    @JacocoGenerated
    private DataciteDoiRequest(Builder builder) {
        setPublicationId(builder.publicationId);
        setExistingDoi(builder.existingDoi);
        setSomeData(builder.xml);
        setType(builder.type);
    }

    @JacocoGenerated
    public static Builder newBuilder() {
        return new Builder();
    }

    @JacocoGenerated
    public String getType() {
        return type;
    }

    @JacocoGenerated
    public void setType(String type) {
        this.type = type;
    }

    @JacocoGenerated
    public URI getPublicationId() {
        return publicationId;
    }

    @JacocoGenerated
    public void setPublicationId(URI publicationId) {
        this.publicationId = publicationId;
    }

    @JacocoGenerated
    public URI getExistingDoi() {
        return existingDoi;
    }

    @JacocoGenerated
    public void setExistingDoi(URI existingDoi) {
        this.existingDoi = existingDoi;
    }

    @JacocoGenerated
    public String getSomeData() {
        return someData;
    }

    @JacocoGenerated
    public void setSomeData(String someData) {
        this.someData = someData;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataciteDoiRequest that = (DataciteDoiRequest) o;
        return Objects.equals(getPublicationId(), that.getPublicationId())
            && Objects.equals(getExistingDoi(), that.getExistingDoi())
            && Objects.equals(getSomeData(), that.getSomeData())
            && Objects.equals(getType(), that.getType());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublicationId(), getExistingDoi(), getSomeData(), getType());
    }

    /**
     * Deep copy of object.
     *
     * @return a builder.
     */
    @JacocoGenerated
    public DataciteDoiRequest.Builder copy() {
        return DataciteDoiRequest.newBuilder()
            .withExistingDoi(getExistingDoi())
            .withPublicationId(getPublicationId())
            .withXml(getSomeData())
            .withType(getType());
    }

    @JacocoGenerated
    public static final class Builder {

        private URI publicationId;
        private URI existingDoi;
        private String xml;
        private String type;

        private Builder() {
        }

        @JacocoGenerated
        public Builder withPublicationId(URI publicationId) {
            this.publicationId = publicationId;
            return this;
        }

        @JacocoGenerated
        public Builder withExistingDoi(URI existingDoi) {
            this.existingDoi = existingDoi;
            return this;
        }

        @JacocoGenerated
        public Builder withXml(String xml) {
            this.xml = xml;
            return this;
        }

        @JacocoGenerated
        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        @JacocoGenerated
        public DataciteDoiRequest build() {
            return new DataciteDoiRequest(this);
        }
    }
}
