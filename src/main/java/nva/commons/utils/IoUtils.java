package nva.commons.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class IoUtils {

    private IoUtils() {
    }

    public static InputStream inputStreamFromResources(Path path) {
        String pathString = path.toString();
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(pathString);
    }

    public static String streamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(newInputStreamReader(stream));
        List<String> lines = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            line = reader.readLine();
        }
        String output = String.join("\n", lines);
        return output;
    }

    private static InputStreamReader newInputStreamReader(InputStream stream) {
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    public static String fileAsString(Path path) throws IOException {
        InputStream fileInputStream = Files.newInputStream(path);
        return streamToString(fileInputStream);
    }

    public static List<String> linesfromResource(Path path) throws IOException {
        BufferedReader reader = new BufferedReader(newInputStreamReader(inputStreamFromResources(path)));
        List<String> lines = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            line = reader.readLine();
        }
        return lines;
    }

    public static String resourceAsString(Path path) throws IOException {
        List<String> lines = linesfromResource(path);
        String result = String.join("\n", lines);
        return result;
    }

    public static String removeMultipleWhiteSpaces(String input) {
        String buffer = input.trim();
        String result = buffer.replaceAll("\\s\\s", " ");
        while (!result.equals(buffer)) {
            buffer = result;
            result = buffer.replaceAll("\\s\\s", " ");
        }
        return result;
    }

    public static InputStream stringToStream(String input){
        return  new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }
}