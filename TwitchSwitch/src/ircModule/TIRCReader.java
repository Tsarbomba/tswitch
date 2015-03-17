package ircModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;

public class TIRCReader extends Thread {
	private final BufferedReader stream;
	private final TIRCConnection tircCon;

	public TIRCReader(final TIRCConnection tircConnection,
			final InputStreamReader inputStreamReader) {
		stream = new BufferedReader(inputStreamReader);
		tircCon = tircConnection;

	}

	@Override
	public void run() {
		String line = null;
		try {
			TIRCMessage msg;
			// wait for lines to come in
			while ((line = stream.readLine()) != null) {
				// debug dump
				// System.out.println(line);
				if (isInterrupted()) {
					try {
						stream.close();
					} catch (final IOException e1) {

					}
					// end thread.
					return;
				}
				msg = new TIRCMessage(line);

				if (line.startsWith("PING ")) {
					tircCon.sendPong(line.substring(5));

				} else if (msg.getCommand().equals("PRIVMSG")
						&& (msg.getParams() != null)) {
					for (final TIRCInputListener listener : tircCon
							.getTIRCInputListeners()) {
						listener.chatMessage(msg.getSender(), msg.getParams(),
								msg.getMessage());
					}

				}
				// Server messages.
				else if (msg.getCommand().equals("004")) {
					// connection established message.
					tircCon.setConnectStatus(1);
				} else if (msg.getCommand().equals("NOTICE")) {
					// bad password message.
					if (msg.getMessage().contains("Error logging in")
							|| msg.getMessage().contains("Login unsuccessful")) {
						tircCon.setConnectStatus(-1);
					}
				}
				// DEBUG UNSUPPORTED PACKET
				// else {
				// System.out.println(line);
				// }
			}
		} catch (final SocketException ex) {
			// socket was closed by shutdown.
		} catch (final Exception ex) {
			ex.printStackTrace();
			// } catch (final IOException ex) {
			// return;
		} finally {
			try {
				tircCon.setConnectStatus(-2);
				stream.close();
			} catch (final IOException e1) {

			}
		}

	}

	public BufferedReader getStreamReader() {
		return stream;
	}
}
