package no.unit.nva.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class StringToGzipInputStream {

    public static final String LINE_SEPARATOR = System.lineSeparator();
    private final List<String> input;

    public StringToGzipInputStream(List<String> input) {
        this.input = input;
    }

    public InputStream compressData() throws IOException {
        byte[] bytes = dataToByteArray();
        return new ByteArrayInputStream(bytes);
    }

    private byte[] dataToByteArray() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            compressData(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void compressData(ByteArrayOutputStream outputStream) throws IOException {
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            for (String line : input) {
                gzipOutputStream.write(line.getBytes(StandardCharsets.UTF_8));
                gzipOutputStream.write(LINE_SEPARATOR.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
