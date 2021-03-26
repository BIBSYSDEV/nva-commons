package no.unit.nva.s3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class StringToGzipInputStream {

    private final List<String> input;

    public StringToGzipInputStream(List<String> input) {
        this.input = input;
    }

    public GZIPInputStream getGzipInputStream() throws IOException {
        try (PipedOutputStream pos = new PipedOutputStream()) {
            PipedInputStream inputStream = new PipedInputStream(pos);
            transferDataToInputStream(pos);
            return new GZIPInputStream(inputStream);
        }
    }

    private void transferDataToInputStream(PipedOutputStream pos) throws IOException {
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(pos)) {
            writeDataToZipOutputStream(gzipOutputStream);
        }
    }

    private void writeDataToZipOutputStream(GZIPOutputStream gzipOutputStream) throws IOException {
        try (BufferedWriter writer = newBufferedWriter(gzipOutputStream)) {
            for (String line : input) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private BufferedWriter newBufferedWriter(OutputStream outputStream) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(outputStream)));
    }
}
