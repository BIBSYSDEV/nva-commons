package no.unit.nva.testutils;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Returns a random port that can be used and then released (avoiding issues caused by releasing / not releasing the
 * port).
 */
public final class RandomPortNumber implements Closeable {

    private final ServerSocket socket;
    private final int port;

    /**
     * Creates a new instance of RandomPort.
     * @return RandomPort instance.
     */
    public static RandomPortNumber newPort() {
        return new RandomPortNumber();
    }

    /**
     * This is hidden as the utility class does not behave like a POJO.
     */
    private RandomPortNumber() {
        try (var serverSocket = new ServerSocket(0)) {
            this.socket = serverSocket;
            this.port = socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The requested port number.
     * @return An port number integer.
     */
    public int number() {
        return port;
    }

    /**
     * Release the port number after completing the use — typically in a tear-down function.
     */
    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
