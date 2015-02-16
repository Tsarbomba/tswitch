package apiModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TwitchAPI {
    private final static String ROOT_API_URL = "https://api.twitch.tv/kraken/";
    private final static int UPDATE_INTERVAL_LIMIT = 60000;
    private long timestamp = 0;
    private ConcurrentHashMap<String, StreamData> liveChannels;

    public ConcurrentHashMap<String, StreamData> getChannelData(
            final List<String> channels) {

        // Don't request from API if too early since last update. However add
        // any new channels to the HashMap.
        if ((System.currentTimeMillis() - timestamp) < UPDATE_INTERVAL_LIMIT) {
            timestamp = System.currentTimeMillis();
            for (final String channel : channels) {
                liveChannels.putIfAbsent(channel.toLowerCase(Locale.US),
                        new StreamData(channel.toLowerCase(Locale.US), "",
                                "N/A", "N/A", "N/A", false));
            }
            return liveChannels;
        }

        liveChannels = new ConcurrentHashMap<String, StreamData>();
        final StringBuffer urlString = new StringBuffer(ROOT_API_URL
                + "streams?channel=");

        for (final String channel : channels) {
            urlString.append(channel + ",");
            liveChannels.put(channel.toLowerCase(Locale.US), new StreamData(
                    channel.toLowerCase(Locale.US), "", "N/A", "N/A", "N/A",
                    false));
        }
        // remove last trailing ,
        urlString.deleteCharAt(urlString.length() - 1);

        BufferedReader in = null;
        try {
            final URL url = new URL(urlString.toString());

            // open a connection to the web server and then get the resulting
            // data
            final URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept",
                    "application/vnd.twitchtv.v3+json");

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            final JsonObject jsObj = new JsonParser().parse(in)
                    .getAsJsonObject();
            final JsonArray streams = jsObj.getAsJsonArray("streams");
            for (int i = 0; i < streams.size(); i++) {
                final JsonElement streamData = streams.get(i);
                final JsonObject cJobj = streamData.getAsJsonObject()
                        .get("channel").getAsJsonObject();

                final String name = cJobj.get("display_name").getAsString();
                final StreamData sData = liveChannels.get(name.toLowerCase());

                try {
                    // can apparently be null sometimes.
                    sData.setStatus(cJobj.get("status").getAsString());
                } catch (final Exception e) {
                    // if null the value will not be set and stays "";
                }

                try {
                    // can apparently be null sometimes.
                    sData.setViewers(streamData.getAsJsonObject()
                            .get("viewers").getAsString());
                } catch (final Exception e) {
                    // if null the value will not be set and stays "";
                }

                try {
                    // can apparently be null sometimes.
                    sData.setGame(streamData.getAsJsonObject().get("game")
                            .getAsString());
                } catch (final Exception e) {
                    // if null the value will not be set and stays "";
                }

                try {
                    // can apparently be null sometimes.
                    sData.setLogoUrl(cJobj.get("logo").getAsString());
                } catch (final Exception e) {
                    // if null the value will not be set and stays "";
                }

                sData.setOnline(true);

            }

        } catch (final IOException e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                }
            }
        }
        return liveChannels;
    }

    public List<String> getUserFollowedChannels(final String username) {

        final List<String> followedChannels = new LinkedList<String>();
        BufferedReader in = null;
        try {
            final URL url = new URL(ROOT_API_URL + "users/" + username
                    + "/follows/channels");

            // open a connection to the web server and then get the resulting
            // data
            final URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept",
                    "application/vnd.twitchtv.v3+json");

            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8));
            final JsonObject jsObj = new JsonParser().parse(in)
                    .getAsJsonObject();
            final JsonArray follows = jsObj.getAsJsonArray("follows");
            for (int i = 0; i < follows.size(); i++) {
                final JsonElement followData = follows.get(i);
                final JsonObject cJobj = followData.getAsJsonObject()
                        .get("channel").getAsJsonObject();

                // add the channel name to the followed channels list.
                followedChannels.add(cJobj.get("display_name").getAsString());
            }

        } catch (final IOException e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                }
            }
        }
        return followedChannels;
    }
}