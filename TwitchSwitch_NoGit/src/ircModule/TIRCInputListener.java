package ircModule;

public interface TIRCInputListener {
	public void chatMessage(final String sender, final String target,
			final String message);
}
