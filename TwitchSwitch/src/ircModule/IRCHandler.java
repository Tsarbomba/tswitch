package ircModule;

public class IRCHandler {
	private final static String TWITCH_IRC_SERVER = "irc.twitch.tv";
	private final static int TWITCH_IRC_SERVER_PORT = 6667;
	private TIRCConnection tircCon;
	private String currentChannel;
	private TIRCInputListener listener;
	private String nickname;
	private String serverPass;

	public IRCHandler(final String nick, final String serverPass,
			TIRCInputListener listener) {
		nickname = nick;
		this.serverPass = serverPass;
		this.listener = listener;
	}

	public int connect() throws Exception {
		int resp = 0;
		if (nickname == null || serverPass == null) {
			throw new Exception(
					"Nickname and Password must be set prior to running this method.");
		}
		tircCon = new TIRCConnection(TWITCH_IRC_SERVER, TWITCH_IRC_SERVER_PORT,
				serverPass, nickname);

		tircCon.registerInputListener(listener);

		try {
			resp = tircCon.connect();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return resp;
	}

	public void shutdownGracefully() {
		if (isConnected()) {
			tircCon.disconnect();
		}
	}

	public void sendMessage(final String message) {
		if (currentChannel != null && isConnected()) {
			tircCon.messageChannel(currentChannel, message);
		}

	}

	public String leaveCurrentChannel() {
		String ctmp = null;
		if (currentChannel != null && isConnected()) {
			tircCon.partChannel(currentChannel);
			ctmp = currentChannel;
			currentChannel = null;
		}
		return ctmp;
	}

	public void joinChannel(final String channelName) {
		if (isConnected()) {
			currentChannel = channelName;
			tircCon.joinChannel(currentChannel);
		}
	}

	public void setNickname(final String nickname) {
		this.nickname = nickname;
	}

	public void setServerPass(final String serverPass) {
		this.serverPass = serverPass;
	}

	public boolean isConnected() {
		return (tircCon != null && tircCon.isConnected());
	}

}
