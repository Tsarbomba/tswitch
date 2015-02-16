package ircModule;

public interface TIRCInputListener {
    public void onMessage(final String sender, final String target,
            final String message);
}
