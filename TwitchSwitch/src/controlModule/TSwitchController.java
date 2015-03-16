package controlModule;

import guiModule.TSwitchGUI;
import ircModule.IRCHandler;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import apiModule.StreamData;
import apiModule.TwitchAPI;

import com.google.gson.Gson;

public class TSwitchController {

    private static final long API_UPDATE_INTERVAL_SEC = 5 * 60;
    private static final String SETTINGS_FILE_PATH = System
            .getProperty("user.home") + "/" + "TSwitch-settings.json";
    private ConcurrentHashMap<String, StreamData> channelData;
    private IRCHandler ircHandler;
    private final TwitchAPI apiHandler;
    private final TSwitchController self = this;
    private final Gson gson = new Gson();
    private TSwitchGUI gui;
    private boolean promptForCredentials = false;
    private TSwitchSettings settings;
    private String streamQuality = "best";
    private final ScheduledExecutorService scheduler = Executors
            .newSingleThreadScheduledExecutor();
    private final Runnable apiUpdateWork;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    public TSwitchController() {

        try {
            settings = readSettingsFromJsonFile();
        } catch (final NoSuchFileException e3) {
            // settings file does not exist so we must build it by asking the
            // user for data.
            settings = new TSwitchSettings(null, null,
                    new CopyOnWriteArrayList<>());
            promptForCredentials = true;
        } catch (final IOException e3) {
            e3.printStackTrace();
        }

        apiHandler = new TwitchAPI();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                scheduler.shutdownNow();
                ircHandler.shutdownGracefully();
                TSwitchMain.closeSocket();
            }
        });

        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {

                    gui = new TSwitchGUI(self);
                    gui.setVisible(true);
                    if (promptForCredentials) {
                        final String[] creds = gui.queryLoginCredentials(true,
                                true);
                        if (creds[0] != null) {
                            setCredentials(creds[0], creds[1]);
                        }
                    }
                    initLatch.countDown();

                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // main thread must wait here until EDT finish creating gui.
        try {
            initLatch.await();
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        // if a cred prompt was required then the EDT already initiated the irc
        // connection.
        if (!promptForCredentials) {
            setupIRCConnection();
        }
        gui.logMessage("System", "Starting API update thread. Updates every "
                + API_UPDATE_INTERVAL_SEC + " seconds.");

        // task queries the API for updated stream data every 5min.
        apiUpdateWork = new Runnable() {
            final LinkedList<Object> streamsData = new LinkedList<Object>();

            @Override
            public void run() {
                gui.logMessage("System", "Retrieving API data.");
                final List<String> channels = settings.getChannels();
                try {

                    final ConcurrentHashMap<String, StreamData> newData = apiHandler
                            .getChannelData(channels);
                    if (newData != null) {
                        channelData = newData;
                    }
                } catch (final Exception e1) {
                    // if the twitchAPI request fails we
                    // leave the old data.
                    // return;
                }
                streamsData.clear();
                // we loop channels instead of channelData to preserve order.
                for (final String channelName : channels) {
                    streamsData.add(new Object[] {
                            channelName,
                            channelData.get(channelName.toLowerCase(Locale.US))
                                    .isOnline() });
                }

                gui.logMessage("System", "Refreshing data in GUI.");
                gui.refreshStreamStatus(streamsData);

            }
        };

        scheduler.scheduleAtFixedRate(apiUpdateWork, 0,
                API_UPDATE_INTERVAL_SEC, TimeUnit.SECONDS);

        gui.addStreamSelectionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                // combobox selection change.
                final String selStream = gui.getSelectedStreamName();
                final StreamData data = getChannelData(selStream);
                if (data != null) {
                    gui.setLabelData(data.getStatus(), data.getGame(),
                            data.getViewers(), data.getLogoUrl());
                } else {
                    gui.setLabelData("", "", "", "");
                }
                if (!selStream.isEmpty() && ircHandler.isConnected()) {
                    final String oldChannel = ircHandler.leaveCurrentChannel();
                    final String newChannel = "#"
                            + selStream.toLowerCase(Locale.US);
                    if (oldChannel != null) {
                        gui.logMessage("System", "Left IRC channel > "
                                + oldChannel);
                    }

                    ircHandler.joinChannel("#"
                            + selStream.toLowerCase(Locale.US));
                    gui.logMessage("System", "Joined IRC channel > "
                            + newChannel);
                    gui.clearChat();
                    gui.setChatChannelName(selStream);
                    retrieveAndSetEmoteIcons(selStream);
                }
                return;
            }
        });

        gui.addStreamOpenListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                try {

                    final ProcessBuilder builder = new ProcessBuilder(
                            "livestreamer", "twitch.tv/"
                                    + gui.getSelectedStreamName(),
                            streamQuality);
                    builder.start();
                    gui.logMessage(
                            "System",
                            "Requested Livestreamer to open > twitch.tv/"
                                    + gui.getSelectedStreamName());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

        });

    }

    public StreamData getChannelData(final String string) {

        return channelData.get(string.toLowerCase(Locale.US));
    }

    public String getUsername() {
        return settings.getUsername();
    }

    public void sendMessage(final String sendText) {
        if (sendText != null && !sendText.trim().isEmpty()
                && ircHandler.isConnected()) {
            ircHandler.sendMessage(sendText);
        }
    }

    public void addChannel(final String channelNames) {
        if (channelNames != null && !channelNames.trim().isEmpty()) {
            for (final String cName : channelNames.split(",")) {
                if (!cName.trim().isEmpty()) {
                    settings.addChannel(cName);
                    gui.logMessage("System", "Added channel > " + cName);
                }
            }
            writeSettingsToJsonFile(settings);
            scheduler.execute(apiUpdateWork);
        }
    }

    public void addChannel(final List<String> channelNames) {
        final StringBuffer sBuf = new StringBuffer();
        for (final String channelName : channelNames) {
            if (channelNames != null && !channelNames.isEmpty()) {
                sBuf.append(channelName + ",");
            }
        }
        sBuf.deleteCharAt(sBuf.length() - 1);
        addChannel(sBuf.toString());
    }

    public void addFollowedChannels() {
        // We run the process on a separate thread since it can
        // take a while.
        scheduler.execute(new Thread() {
            @Override
            public void run() {
                final List<String> followedChannels = apiHandler
                        .getUserFollowedChannels(settings.getUsername());
                addChannel(followedChannels);
                gui.logMessage("System", "Added channels followed by > "
                        + settings.getUsername());
            }
        });

    }

    public void retrieveAndSetEmoteIcons(final String channelName) {

        // We run the process on a separate thread since it can
        // take a while.
        scheduler.execute(new Thread() {
            @Override
            public void run() {
                final ConcurrentHashMap<String, String> emotes = apiHandler
                        .getChannelChatEmotes(channelName);
                gui.setChatEmoteIcons(emotes);
                gui.logMessage("System", "Retrieved supported chat emotes.");
            }
        });

    }

    public void removeChannel(final String channelName) {
        if (channelName != null && !channelName.trim().isEmpty()) {
            settings.removeChannel(channelName);
            gui.logMessage("System", "Removed channel > " + channelName);
            writeSettingsToJsonFile(settings);
            scheduler.execute(apiUpdateWork);
        }
    }

    public void setPreferredStreamQuality(final String qualityKeyword) {

        switch (qualityKeyword) {
        case "source":
            streamQuality = qualityKeyword + ",high,medium,low,mobile,worst";
            break;
        case "high":
            streamQuality = qualityKeyword + ",medium,low,mobile,worst";
            break;
        case "medium":
            streamQuality = qualityKeyword + ",low,mobile,worst";
            break;
        case "low":
            streamQuality = qualityKeyword + ",mobile,worst";
            break;
        case "mobile":
            streamQuality = qualityKeyword + ",worst";
            break;
        default:
            streamQuality = "worst";
            break;
        }

        gui.logMessage("System", "Stream quality priority changed to > "
                + streamQuality);
    }

    private void setupIRCConnection() {
        String oldChannel = null;
        if (ircHandler != null) {
            // need to shutdown old irc connection.
            oldChannel = ircHandler.leaveCurrentChannel();
            if (oldChannel != null) {
                gui.logMessage("System", "Left IRC channel > " + oldChannel);
            }
            ircHandler.shutdownGracefully();
        }
        ircHandler = new IRCHandler(settings.getUsername(), settings.getoAuth());
        ircHandler.setGUI(gui);

        // connect the irchandler to irc server.
        gui.logMessage("System", "Connecting to IRC server.");

        try {
            final int resp = ircHandler.connect();
            if (resp == -1) {
                gui.logMessage("System",
                        "Connecting to IRC server failed. > Incorrect username/oauth.");
            } else if (resp != 1) {
                gui.logMessage("System",
                        "Connecting to IRC server failed. > Unknown reason.");
            } else {
                gui.logMessage("System",
                        "Connecting to IRC server was successful.");
                // Rejoin the previous channel if we where in one.
                if (oldChannel != null) {
                    ircHandler.joinChannel(oldChannel);
                    gui.logMessage("System", "Joined IRC channel > "
                            + oldChannel);
                }
            }
        } catch (final Exception e2) {
            e2.printStackTrace();
        }
    }

    public void setCredentials(final String username, final String pass) {
        settings.setUsername(username);
        settings.setoAuth(pass);
        gui.logMessage("System",
                "The credentials(username/oauth) was modified.");
        // We run the rest of the process on a separate thread since it can
        // take a while.
        new Thread() {
            @Override
            public void run() {
                setupIRCConnection();
                writeSettingsToJsonFile(settings);
            }
        }.start();
    }

    private void writeSettingsToJsonFile(final TSwitchSettings settings) {
        final String jsonStr = gson.toJson(settings);
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(SETTINGS_FILE_PATH), StandardCharsets.UTF_8)) {
            writer.write(jsonStr, 0, jsonStr.length());
        } catch (final IOException x) {
            System.err.format("IOException: %s%n", x);
        }

    }

    private TSwitchSettings readSettingsFromJsonFile() throws IOException {
        TSwitchSettings ret = null;
        String jsonStr;

        jsonStr = new String(Files.readAllBytes(Paths.get(SETTINGS_FILE_PATH)),
                StandardCharsets.UTF_8);
        ret = gson.fromJson(jsonStr, TSwitchSettings.class);

        // Necessary since gson forces a no-args construction.
        ret.initIfNeeded();

        return ret;
    }
}
