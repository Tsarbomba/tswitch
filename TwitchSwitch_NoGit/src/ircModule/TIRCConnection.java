package ircModule;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.SocketFactory;

public class TIRCConnection {

    private final TIRCServer server;
    private Socket conSocket;
    private final List<TIRCInputListener> inputListeners;
    private TIRCWriter outputThread;
    private TIRCReader inputThread;
    /** The Character set to use for encoding the connection */
    private final Charset CHARSET = Charset.forName("UTF-8");
    private final CountDownLatch connectedLatch = new CountDownLatch(1);
    private final AtomicInteger connectStatus = new AtomicInteger(0);

    public TIRCConnection(final String server, final int port,
            final String password, final String nick) {
        this.server = new TIRCServer(server, port, password, nick);
        inputListeners = new CopyOnWriteArrayList<TIRCInputListener>();

    }

    public int connect() throws Exception {
        connectInit(SocketFactory.getDefault());
        return connectStatus.get();
    }

    private void connectInit(final SocketFactory sfact) throws Exception {

        // connect socket
        if (conSocket == null) {
            conSocket = sfact.createSocket(server.getAddress(),
                    server.getPort());
            connectHandshake(conSocket);
        }
    }

    private void connectHandshake(final Socket conSocket) throws IOException,
            InterruptedException {
        // open streams and attach their worker threads.
        outputThread = new TIRCWriter(new OutputStreamWriter(
                conSocket.getOutputStream(), CHARSET));
        inputThread = new TIRCReader(this, new InputStreamReader(
                conSocket.getInputStream(), CHARSET));
        // start IO threads.
        outputThread.start();
        inputThread.start();
        // send password
        outputThread.sendPass(server.getPassword());

        // send nick
        outputThread.sendNick(server.getNick());

        // When server reply is received, latch is released.
        connectedLatch.await();

        if (connectStatus.get() != 1) {
            // connection failed
            disconnect();
        }
    }

    public void setConnectStatus(final int statusCode) {
        /*
         * code -1 = bad pass, 1 = connect success, 0 = unchanged, -2 = unknown
         * failure state.
         */
        connectStatus.set(statusCode);
        connectedLatch.countDown();
    }

    public boolean isConnected() {
        if (connectStatus.get() == 1) {
            return true;
        }
        return false;
    }

    public void registerInputListener(final TIRCInputListener listener) {
        inputListeners.add(listener);
    }

    public void sendPong(final String response) {
        outputThread.sendPong(response);
    }

    public List<TIRCInputListener> getTIRCInputListeners() {
        return inputListeners;
    }

    public void disconnect() {
        connectStatus.compareAndSet(1, -2);
        inputThread.interrupt();
        outputThread.interrupt();
        try {
            conSocket.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void joinChannel(final String channel) {
        outputThread.sendJoin(channel);
    }

    public void partChannel(final String channel) {
        outputThread.sendPart(channel);
    }

    public void messageChannel(final String channel, final String message) {
        outputThread.sendChannelMessage(channel, message);
    }

}
