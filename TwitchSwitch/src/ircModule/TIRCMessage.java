package ircModule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TIRCMessage {

	private final static Pattern PARSE_PATTERN = Pattern
			.compile("^(:(?<prefix>\\S+) )?(?<command>\\S+)( (?!:)(?<params>.+?))?( :(?<message>.+))?$");

	/** Parameters separated by a space */
	private String params = null;
	/** The IRC command. */
	private String command = null;
	/** The message (basically anything behind the colon). */
	private String message = null;
	/** The sender. */
	private String prefix = null;
	/** The sender nickname. */
	private String sender = null;
	/** whether the message should be delivered ASAP */
	private boolean prioritized = false;

	private Matcher patternMatcher = null;

	public TIRCMessage(final String prefix, final String command,
			final String params, final String message, final boolean prioritized) {
		this.prefix = prefix;
		this.command = command;
		this.params = params;
		this.message = message;
		this.prioritized = prioritized;

	}

	/**
	 * Creates a TIRCMessage by parsing a raw IRC message string.
	 * 
	 * @param line
	 *            Raw data from the server.
	 */
	public TIRCMessage(final String line) {

		patternMatcher = PARSE_PATTERN.matcher(line);

		patternMatcher.matches();
		prefix = patternMatcher.group("prefix");
		command = patternMatcher.group("command");
		params = patternMatcher.group("params");
		message = patternMatcher.group("message");

		// parse sender from prefix
		if ((prefix != null) && (prefix.indexOf('!') > 0)) {
			final String[] data = prefix.split("@|!");
			if (data.length > 0) {
				// nick is element 0.
				sender = data[0];
			}
		} else if (prefix != null) {
			sender = prefix;
		}

	}

	public String getAsRawIRCString() {
		final StringBuffer buffer = new StringBuffer();

		if ((prefix != null) && (prefix.length() > 0)) {
			buffer.append(":").append(prefix).append(" ");
		}
		buffer.append(command);
		if ((params != null) && (params.length() > 0)) {
			buffer.append(" ").append(params);
		}
		if ((message != null) && (message.length() > 0)) {
			buffer.append(" :").append(message);
		}
		return buffer.toString();
	}

	public boolean getPrioritized() {
		return prioritized;
	}

	public String getCommand() {
		return command;
	}

	public String getParams() {
		return params;
	}

	public String getSender() {
		return sender;
	}

	public String getMessage() {
		return message;
	}

}
