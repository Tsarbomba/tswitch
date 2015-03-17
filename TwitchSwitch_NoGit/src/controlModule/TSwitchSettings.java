package controlModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TSwitchSettings {

	private transient final Path settingsFilePath;
	private transient final Gson gson = new Gson();
	private String username;
	private String oAuth;
	private LinkedList<String> channels;
	private transient HashMap<String, Integer> cNameLookup;
	private transient ReentrantLock channelsLock = new ReentrantLock();

	public TSwitchSettings(Path settingsFilePath) throws IOException {
		this.settingsFilePath = settingsFilePath;
		readSettingsFromJsonFile();
	}

	public TSwitchSettings(String username, String oAuth, Path settingsFilePath)
			throws IOException {
		this.settingsFilePath = settingsFilePath;
		channels = new LinkedList<String>();
		cNameLookup = new HashMap<String, Integer>();
		this.username = username;
		this.oAuth = oAuth;
	}

	public String getUsername() {
		return username;
	}

	public String getoAuth() {
		return oAuth;
	}

	@SuppressWarnings("unchecked")
	public List<String> getChannels() {
		channelsLock.lock();
		List<String> ret = (List<String>) channels.clone();
		channelsLock.unlock();
		return ret;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public void setoAuth(final String oAuth) {
		this.oAuth = oAuth;
	}

	public void addChannel(final String channelName) {
		channelsLock.lock();
		if (channelName != null && !channelName.trim().isEmpty()) {
			if (!cNameLookup.containsKey(channelName.toLowerCase(Locale.US))) {
				cNameLookup.put(channelName.toLowerCase(Locale.US), null);
				channels.add(channelName.toLowerCase(Locale.US));
			}
		}
		channelsLock.unlock();
	}

	public void removeChannel(final String channelName) {
		channelsLock.lock();
		if (channelName != null && !channelName.trim().isEmpty()) {
			if (cNameLookup.containsKey(channelName.toLowerCase(Locale.US))) {
				cNameLookup.remove(channelName.toLowerCase(Locale.US));
				channels.remove(channelName.toLowerCase(Locale.US));
			}
		}
		channelsLock.unlock();
	}

	public void writeSettingsToJsonFile() {
		channelsLock.lock();
		final String jsonStr = gson.toJson(this);
		channelsLock.unlock();
		try (BufferedWriter writer = Files.newBufferedWriter(settingsFilePath,
				StandardCharsets.UTF_8)) {
			writer.write(jsonStr, 0, jsonStr.length());
		} catch (final IOException x) {
			System.err.format("IOException: %s%n", x);
		}

	}

	private void readSettingsFromJsonFile() throws IOException {
		channelsLock.lock();
		channels = new LinkedList<String>();
		cNameLookup = new HashMap<String, Integer>();
		String jsonStr;
		boolean wasCleaned = false;

		jsonStr = new String(Files.readAllBytes(settingsFilePath),
				StandardCharsets.UTF_8);
		final JsonObject jsObj = new JsonParser().parse(jsonStr)
				.getAsJsonObject();
		username = jsObj.get("username").getAsString();
		oAuth = jsObj.get("oAuth").getAsString();
		JsonArray channelArray = jsObj.get("channels").getAsJsonArray();
		for (int i = 0; i < channelArray.size(); i++) {
			final String channelName = channelArray.get(i).getAsString();
			// filter potential duplicates with the help of the lookup table.
			if (!cNameLookup.containsKey(channelName.toLowerCase(Locale.US))) {
				cNameLookup.put(channelName.toLowerCase(Locale.US), null);
				channels.add(channelName.toLowerCase(Locale.US));
			} else {
				wasCleaned = true;
			}
		}
		channelsLock.unlock();
		// if we removed duplicates we want to save the clean version as well.
		if (wasCleaned) {
			writeSettingsToJsonFile();
		}
		;
	}
}
