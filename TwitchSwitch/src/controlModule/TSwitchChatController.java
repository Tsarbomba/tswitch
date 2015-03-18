package controlModule;

import guiModule.FXDialogs;
import guiModule.FXGUI;
import ircModule.TIRCInputListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class TSwitchChatController implements TIRCInputListener {
	private static final int CHAT_MAX_MESSAGE_BUFFER = 100;
	private ScrollPane chatScroller;
	private TextFlow chatArea;

	private boolean autoScroll = true;
	private final DoubleProperty chatHeightProperty = new SimpleDoubleProperty();
	private final List<Integer> chatParagraphs = new LinkedList<Integer>();
	private HashMap<String, Image> chatURLToImageMapping;
	private HashMap<String, String> chatEmoteToURLMapping;
	private LinkedList<Pattern> chatEmoteRegexPatterns;
	private Matcher emotesRegXpMatcher = Pattern.compile("").matcher("");
	private ReentrantLock emoteLock = new ReentrantLock();
	private final Matcher linksRegXpMatcher;
	private final EventHandler<ActionEvent> linkListener;
	private final TSwitchSettings settings;

	public TSwitchChatController(ScrollPane chatScroller, TextFlow chatArea,
			TSwitchSettings settings) {
		super();
		this.chatScroller = chatScroller;
		this.chatArea = chatArea;
		this.settings = settings;
		linksRegXpMatcher = Pattern
				.compile(
						"\\b(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
				.matcher("");
		// event handler that binds the scroll to the chat height. Scrolls to
		// buttom if autoscroll is true.
		chatHeightProperty.bind(chatArea.heightProperty());
		chatHeightProperty.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> ov, Number t,
					Number t1) {
				if (autoScroll) {
					chatScroller.setVvalue(chatScroller.getVmax());
				}
			}
		});
		// defines the hyperlink listener;
		linkListener = new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				String url = ((Hyperlink) arg0.getSource()).getText();

				if (FXDialogs.showConfirmHyperlinkDialog(url)) {
					// gets the available services from our fx application then
					// shows the url in the default platform browser.´
					FXGUI.instance.getHostServices().showDocument(url);
				}

			}
		};
	}

	@Override
	public void chatMessage(String sender, String target, String message) {

		LinkedList<Node> nodes = new LinkedList<Node>();

		Text senderNode = new Text(sender + ": ");

		if (sender.equalsIgnoreCase(settings.getUsername())) {
			// this is our own message.
			senderNode.getStyleClass().addAll("senderOwn");
		} else if (sender.equalsIgnoreCase("jtv")) {
			// this is a twitch server message.
			senderNode.getStyleClass().addAll("senderTwitch");
		} else if (sender.equalsIgnoreCase("twitchnotify")
				|| sender.equalsIgnoreCase("moobot")) {
			// this is a twitch bot message
			senderNode.getStyleClass().addAll("senderBot");
		} else {
			// this is a regular user message.
			senderNode.getStyleClass().addAll("senderUser");
		}

		nodes.add(senderNode);
		// to make parsing easier we search for links/emotes word by word.
		StringBuilder textSegment = new StringBuilder();
		for (String word : message.split("\\s+")) {
			Node node;
			if ((node = parseEmotes(word)) != null) {
				// add any leading text
				if (textSegment.length() > 0) {
					nodes.add(new Text(textSegment.toString()));
					textSegment.setLength(0);
				}
				// add the emote image node
				nodes.add(node);
			} else if ((node = parseLinks(word)) != null) {
				// add any leading text
				if (textSegment.length() > 0) {
					nodes.add(new Text(textSegment.toString()));
					textSegment.setLength(0);
				}
				// add the hyperlink node
				nodes.add(node);
			} else {
				textSegment.append(word + " ");
			}
		}
		// Every message must end with a newline.
		textSegment.append("\n");
		nodes.add(new Text(textSegment.toString()));

		/*
		 * Every time a message is processed we store the number of Node objects
		 * it constitutes. When we want to clamp the chat buffer we can remove
		 * whole paragraphs(sender:message) because we know how many nodes they
		 * contain.
		 */
		chatParagraphs.add(nodes.size());

		Runnable guijob = () -> {
			// clamp the chat/textFlow buffer to CHAT_MAX_MESSAGE_BUFFER
			if (chatParagraphs.size() >= CHAT_MAX_MESSAGE_BUFFER) {
				chatArea.getChildren().remove(0, chatParagraphs.remove(0));
			}
			// if the scroll is at the bottom prior to adding text we
			// autoscroll to bottom otherwise we leave it.
			autoScroll = ((chatScroller.getVvalue() - chatScroller.getVmax()) == 0);

			chatArea.getChildren().addAll(nodes);
		};
		Platform.runLater(guijob);

	}

	private Node parseEmotes(String text) {

		emoteLock.lock();
		if (chatEmoteRegexPatterns == null) {
			emoteLock.unlock();
			return null;
		}
		emotesRegXpMatcher.reset(text);
		for (Pattern emotePat : chatEmoteRegexPatterns) {
			emotesRegXpMatcher.usePattern(emotePat);
			if (emotesRegXpMatcher.matches()) {
				String emoteName = emotePat.pattern();
				emoteName = emoteName.substring(2, emoteName.length() - 2);

				String url = chatEmoteToURLMapping.get(emoteName);
				if (url == null) {
					break;
				}
				Image emoteImg = chatURLToImageMapping.get(url);
				if (emoteImg != null) {
					emoteLock.unlock();
					return new ImageView(emoteImg);
				} else {
					emoteImg = new Image(url, true);
					chatURLToImageMapping.put(url, emoteImg);
					emoteLock.unlock();
					return new ImageView(emoteImg);
				}
			}
			emotesRegXpMatcher.reset();
		}

		emoteLock.unlock();

		return null;
	}

	private Node parseLinks(String text) {

		if (linksRegXpMatcher != null) {
			linksRegXpMatcher.reset(text);
			if (linksRegXpMatcher.matches()) {
				Hyperlink ret = new Hyperlink(text);
				ret.setOnAction(linkListener);
				ret.setWrapText(true);
				return ret;
			}
		}
		return null;

	}

	public void setChatEmotes(final HashMap<String, String> emoteSet) {

		// build the regexp pattern for matching emotes.
		if (chatEmoteRegexPatterns == null) {
			chatEmoteRegexPatterns = new LinkedList<Pattern>();
		} else {
			chatEmoteRegexPatterns.clear();
		}

		for (final Entry<String, String> emoteData : emoteSet.entrySet()) {
			chatEmoteRegexPatterns.add(Pattern.compile("\\b"
					+ emoteData.getKey() + "\\b"));
		}

		emoteLock.lock();
		chatEmoteToURLMapping = emoteSet;

		if (chatURLToImageMapping == null) {
			chatURLToImageMapping = new HashMap<String, Image>();
		} else {
			chatURLToImageMapping.clear();
		}

		emoteLock.unlock();

	}

	public void clearChat() {
		Runnable guijob = () -> {
			chatArea.getChildren().clear();
			chatParagraphs.clear();
		};
		Platform.runLater(guijob);
	}

}
