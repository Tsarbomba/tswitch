package ircModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.LinkedBlockingDeque;

public class TIRCWriter extends Thread {

    private final static int MAX_LINE_LENGTH = 512;
    private final static String LINE_ENDING = "\n";

    private final BufferedWriter stream;
    private final LinkedBlockingDeque<String> msgQueue = new LinkedBlockingDeque<String>();

    public TIRCWriter(final OutputStreamWriter outputStreamWriter) {
        stream = new BufferedWriter(outputStreamWriter);
    }

    @Override
    public void run() {
        try {
            String msg;
            while (true) {
                msg = msgQueue.take();
                writeData(msg);
                if (isInterrupted()) {
                    try {
                        stream.close();
                    } catch (final IOException e1) {

                    }
                    // end thread.
                    return;
                }

            }

        } catch (final InterruptedException | IOException e) {
            try {
                stream.close();
            } catch (final IOException e1) {

            }
            // end thread.
            return;

        }
    }

    public void send(final TIRCMessage message) {
        if (message.getPrioritized()) {
            msgQueue.addFirst(message.getAsRawIRCString());
        } else {
            msgQueue.addLast(message.getAsRawIRCString());
        }
    }

    public void sendChannelMessage(final String channel, final String message) {
        final TIRCMessage msg = new TIRCMessage(null, "PRIVMSG", channel,
                message, false);
        send(msg);
    }

    public void sendNick(final String nick) {
        final TIRCMessage msg = new TIRCMessage(null, "NICK", nick, null, false);
        send(msg);
    }

    public void sendPass(final String password) {
        final TIRCMessage msg = new TIRCMessage(null, "PASS", password, null,
                false);
        send(msg);
    }

    public void sendJoin(final String channelName) {
        final TIRCMessage msg = new TIRCMessage(null, "JOIN", channelName,
                null, false);
        send(msg);
    }

    public void sendPart(final String channelName) {
        final TIRCMessage msg = new TIRCMessage(null, "PART", channelName,
                null, false);
        send(msg);
    }

    public void sendQuit(final String message) {
        final TIRCMessage msg = new TIRCMessage(null, "QUIT", null, message,
                false);
        send(msg);
    }

    public void sendPong(final String code) {
        final TIRCMessage msg = new TIRCMessage(null, "PONG", code, null, true);
        send(msg);
    }

    public BufferedWriter getStreamWriter() {
        return stream;
    }

    private void writeData(String line) throws IOException {
        if (line.length() > (MAX_LINE_LENGTH - 2)) {
            line = line.substring(0, MAX_LINE_LENGTH - 2);
        }
        stream.write(line + LINE_ENDING);
        stream.flush();
    }
}
