package controlModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class TSwitchMain {

    private final static int PORT = 9999;
    private static ServerSocket socket;

    /**
     * Launch the application.
     * 
     * @throws FileNotFoundException
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws FileNotFoundException,
            IOException {
        try {
            checkIfRunning();
        } catch (final RuntimeException e) {
            System.out.println(e.getMessage());
            return;
        }

        new TSwitchController();
    }

    private static void checkIfRunning() throws RuntimeException, IOException {
        try {
            // Bind to localhost adapter with a zero connection queue
            socket = new ServerSocket(PORT, 0,
                    InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
        } catch (final BindException e) {
            throw new RuntimeException("Already running.");
        } catch (final IOException e) {
            throw new RuntimeException("Failed to open local socket.");
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
