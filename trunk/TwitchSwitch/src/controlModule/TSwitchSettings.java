package controlModule;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TSwitchSettings {

    private String username;
    private String oAuth;
    private final CopyOnWriteArrayList<String> channels;
    private transient ConcurrentHashMap<String, Integer> cNameLookup;

    public TSwitchSettings(final String username, final String oAuth,
            final CopyOnWriteArrayList<String> channels) {
        super();
        this.username = username;
        this.oAuth = oAuth;
        this.channels = channels;
        initLookup();
    }

    private void initLookup() {
        cNameLookup = new ConcurrentHashMap<String, Integer>();
        final LinkedList<String> duplicates = new LinkedList<String>();
        for (final String channel : channels) {
            // adds to the lookup and collect duplicates.
            if (cNameLookup.putIfAbsent(channel.toLowerCase(Locale.US), 0) != null) {
                duplicates.add(channel);
            }
        }
        // remove duplicates from list
        for (final String dupe : duplicates) {
            channels.remove(dupe);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getoAuth() {
        return oAuth;
    }

    public List<String> getChannels() {
        final LinkedList<String> ret = new LinkedList<String>();
        for (final String channel : channels) {
            ret.add(channel);
        }
        return ret;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setoAuth(final String oAuth) {
        this.oAuth = oAuth;
    }

    public void initIfNeeded() {
        if (cNameLookup == null) {
            initLookup();
        }
    }

    public void addChannel(final String channelName) {
        initIfNeeded();
        if (channelName != null && !channelName.trim().isEmpty()) {
            if (!cNameLookup.containsKey(channelName.toLowerCase(Locale.US))) {
                cNameLookup.put(channelName.toLowerCase(Locale.US), 0);
                channels.add(channelName);
            }
        }
    }

    public void removeChannel(final String channelName) {
        initIfNeeded();
        if (channelName != null && !channelName.trim().isEmpty()) {
            if (cNameLookup.containsKey(channelName.toLowerCase(Locale.US))) {
                cNameLookup.remove(channelName.toLowerCase(Locale.US));
                channels.remove(channelName);
            }
        }
    }

}
