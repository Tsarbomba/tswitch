package apiModule;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.stream.JsonReader;

public class TwitchAPI {
	private final static String ROOT_API_URL = "https://api.twitch.tv/kraken/";
	// private final static int UPDATE_INTERVAL_LIMIT = 60000;
	// private long timestamp = 0;
	private ConcurrentHashMap<String, StreamData> liveChannels;

	public ConcurrentHashMap<String, StreamData> getChannelData(
			final List<String> channels) throws Exception {

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

		final URL url = new URL(urlString.toString());
		// open a connection to the web server and then get the resulting
		// data
		final URLConnection connection = url.openConnection();
		connection.setRequestProperty("Accept",
				"application/vnd.twitchtv.v3+json");

		try (JsonReader reader = new JsonReader(new InputStreamReader(
				connection.getInputStream(), StandardCharsets.UTF_8));) {
			reader.beginObject(); // JSON root
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("streams")) {
					reader.beginArray(); // streams array
					while (reader.hasNext()) {
						reader.beginObject(); // array objects
						String displayName = null;
						String viewers = "N/A";
						String game = "N/A";
						String logoUrl = "";
						String status = "N/A";
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("viewers")) {
								viewers = reader.nextString();
							} else if (name.equals("channel")) {
								reader.beginObject(); // channel object
								while (reader.hasNext()) {
									name = reader.nextName();
									if (name.equals("display_name")) {
										displayName = reader.nextString();
									} else if (name.equals("game")) {
										game = reader.nextString();
									} else if (name.equals("logo")) {
										logoUrl = reader.nextString();
									} else if (name.equals("status")) {
										status = reader.nextString();
									} else {
										reader.skipValue();
									}
								}
								reader.endObject();
							} else {
								reader.skipValue();
							}
						}
						// set the parsed data on the corresponding streamdata
						// object.

						if (displayName != null) {
							final StreamData sData = liveChannels
									.get(displayName.toLowerCase(Locale.US));
							sData.setGame(game);
							sData.setViewers(viewers);
							sData.setLogoUrl(logoUrl);
							sData.setStatus(status);
							sData.setOnline(true);

							reader.endObject();
						}
					}
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
		}
		return liveChannels;
	}

	public List<String> getUserFollowedChannels(final String username)
			throws IOException {

		final List<String> followedChannels = new LinkedList<String>();

		URL url = null;
		try {
			url = new URL(ROOT_API_URL + "users/" + username
					+ "/follows/channels");
		} catch (MalformedURLException e2) {
			// This shouldn't happen unless the username is very odd.
			e2.printStackTrace();
		}

		// open a connection to the web server and then get the resulting
		// data
		final URLConnection connection = url.openConnection();
		connection.setRequestProperty("Accept",
				"application/vnd.twitchtv.v3+json");

		try (JsonReader reader = new JsonReader(new InputStreamReader(
				connection.getInputStream(), StandardCharsets.UTF_8))) {

			reader.beginObject(); // JSON root
			while (reader.hasNext()) { // _links and emoticons
				String name = reader.nextName();
				if (name.equals("follows")) {
					reader.beginArray(); // array of followed channel objects
					while (reader.hasNext()) {
						reader.beginObject(); // followed channel objects
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("channel")) {
								reader.beginObject(); // channel data object
								while (reader.hasNext()) {
									name = reader.nextName();
									if (name.equals("display_name")) {
										followedChannels.add(reader
												.nextString());
									} else {
										reader.skipValue();
									}
								}
								reader.endObject();
							} else {
								reader.skipValue();
							}
						}
						reader.endObject();
					}
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();

		} catch (final IOException e1) {
			e1.printStackTrace();
		}
		return followedChannels;
	}

	public HashMap<String, String> getAllChatEmotes() throws IOException {

		final HashMap<String, String> emotes = new HashMap<String, String>();
		URL url = null;
		try {
			url = new URL("http://twitchemotes.com/api_cache/v2/images.json");
		} catch (MalformedURLException e2) {
			// this should never happen.
			e2.printStackTrace();
		}

		// open a connection to the web server and then get the resulting
		// data

		URLConnection connection = url.openConnection();

		// parse the official emotes from the Twitch API
		try (JsonReader reader = new JsonReader(new InputStreamReader(
				connection.getInputStream(), StandardCharsets.UTF_8))) {

			reader.beginObject(); // JSON root
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("images")) {
					reader.beginObject();
					while (reader.hasNext()) {
						String emoteID = reader.nextName();
						String emoteRegex = null;
						reader.beginObject();
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("code")) {
								emoteRegex = reader.nextString();
							} else {
								reader.skipValue();
							}
						}
						if (emoteRegex != null && emoteID != null) {
							emotes.put(emoteRegex,
									"http://static-cdn.jtvnw.net/emoticons/v1/"
											+ emoteID + "/1.0");
						}
						reader.endObject();
					}
					reader.endObject();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject(); // JSON root
		}

		// add the BetterTTV emotes https://api.betterttv.net/emotes
		url = null;
		try {
			url = new URL(ROOT_API_URL + "chat/emoticons");
		} catch (MalformedURLException e2) {
			// this should never happen.
			e2.printStackTrace();
		}

		connection = url.openConnection();

		try (JsonReader reader = new JsonReader(new InputStreamReader(
				connection.getInputStream(), StandardCharsets.UTF_8))) {

			reader.beginObject(); // JSON root
			while (reader.hasNext()) { // _links and emoticons
				String name = reader.nextName();
				if (name.equals("emotes")) {
					reader.beginArray(); // emotes array
					while (reader.hasNext()) {
						String emoteRegex = null;
						String emoteUrl = null;
						reader.beginObject(); // array object
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("regex")) {
								// object regex variable
								emoteRegex = reader.nextString();
							} else if (name.equals("url")) {
								emoteUrl = "http:" + reader.nextString();
							} else {
								reader.skipValue();
							}
						}
						reader.endObject();
						if (emoteRegex != null && emoteUrl != null) {
							emotes.put(emoteRegex, emoteUrl);
						}
					}
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();

		}
		return emotes;
	}

}