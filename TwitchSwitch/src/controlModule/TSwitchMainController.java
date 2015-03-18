package controlModule;

import guiModule.FXDialogs;
import ircModule.IRCHandler;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import javafx.util.Pair;
import apiModule.StreamData;
import apiModule.TwitchAPI;

public class TSwitchMainController implements Initializable {

	@FXML
	private TextArea logArea;
	@FXML
	private TextFlow chatArea;
	@FXML
	private Tab streamTab;
	@FXML
	private Tab chatTab;
	@FXML
	private ScrollPane chatScroller;
	@FXML
	private ImageView streamLogoImg;
	@FXML
	private ComboBox<StreamData> streamSelectionBox;
	@FXML
	private Label streamViewersLabel;
	@FXML
	private Label streamTitleLabel;
	@FXML
	private Label streamPlayingLabel;
	@FXML
	private TextField chatInput;
	@FXML
	private Button streamWatchBtn;
	@FXML
	private Button chatInputSendBtn;
	@FXML
	private ToggleGroup qualityToggleGroup;

	private static final long API_UPDATE_INTERVAL_SEC = 5 * 60;
	private static ServerSocket UNIQUE_INSTANCE_SOCKET;
	private static final Path SETTINGS_FILE_PATH = Paths.get(System
			.getProperty("user.home") + "/" + "TSwitch-settings.json");
	private IRCHandler ircHandler;
	private TwitchAPI apiHandler;
	private TSwitchSettings settings;
	private String streamQuality = "source,high,medium,low,mobile,worst";
	private ScheduledExecutorService scheduler;
	private final Date date = new Date();
	private final DateFormat dateFormat = new SimpleDateFormat(
			"[yy-MM-dd HH:mm:ss]");
	private Runnable apiUpdateWork;
	private TSwitchChatController chatController;

	@Override
	public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

		try {
			/*
			 * To check that no other instance of this application is running we
			 * bind to a localhost socket. Any subsequent instance will get a
			 * BindException. The socket has no other purpose then to block
			 * multiple instances from running concurrently.
			 */
			UNIQUE_INSTANCE_SOCKET = new ServerSocket(9999, 0,
					InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
			UNIQUE_INSTANCE_SOCKET.isBound();
		} catch (BindException e) {
			System.out
					.println("An instance of this application is already running.");
			System.exit(1);
		} catch (IOException e) {
			System.out
					.println("Failed to bind to local socket > 127.0.0.1:9999");
			System.exit(1);
		}

		try {
			if (Files.isRegularFile(SETTINGS_FILE_PATH)
					&& Files.isReadable(SETTINGS_FILE_PATH)
					&& Files.isWritable(SETTINGS_FILE_PATH)) {
				// a workable settings file does exist so we load from it.
				settings = new TSwitchSettings(SETTINGS_FILE_PATH);
			} else {
				// a workable settings file does not exist so we must create it.
				settings = new TSwitchSettings(null, null, SETTINGS_FILE_PATH);
				changeAuth();
				if (settings.getUsername() == null
						|| settings.getoAuth() == null) {
					System.exit(1);
				}
			}

		} catch (final IOException e3) {
			e3.printStackTrace();
		}

		scheduler = Executors.newSingleThreadScheduledExecutor();
		apiHandler = new TwitchAPI();
		chatController = new TSwitchChatController(chatScroller, chatArea,
				settings);
		retrieveAndSetEmoteIcons();

		// the shutdownhook thread will clean up resources upon exit.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				if (scheduler != null) {
					scheduler.shutdownNow();
				}
				if (ircHandler != null) {
					ircHandler.shutdownGracefully();
				}
				Platform.exit();
			}
		});

		// connect to irc-server for chat.
		setupIRCConnection();

		logMessage("System", "Starting API update thread. Updates every "
				+ API_UPDATE_INTERVAL_SEC + " seconds.");

		// task queries the API for updated stream data every 5min.
		apiUpdateWork = () -> {
			/*
			 * This whole runnable is wrapped in a catch-all try. This is ofc
			 * bad practice however we can't let this runnable throw an
			 * exception because the Java scheduler cancels recurring tasks(our
			 * scheduled api updates) if an exception is ever thrown.
			 */
			try {
				logMessage("System", "Retrieving API data.");
				final List<String> channels = settings.getChannels();
				final ConcurrentHashMap<String, StreamData> newData;

				newData = apiHandler.getChannelData(channels);

				final ObservableList<StreamData> streamsData = FXCollections
						.observableList(new LinkedList<StreamData>());

				// we loop channels instead of newData to preserve order.
				for (final String channelName : channels) {
					streamsData.add(newData.get(channelName));
				}

				logMessage("System", "Refreshing data in GUI.");
				Runnable guijob = () -> {
					StreamData oldSelection = streamSelectionBox
							.getSelectionModel().getSelectedItem();
					// detach the event dispatcher to prevent a selection
					// change event to fire.
					EventHandler<ActionEvent> edTmp = streamSelectionBox
							.getOnAction();
					streamSelectionBox.setOnAction(null);
					streamSelectionBox.setItems(streamsData);
					streamSelectionBox.getSelectionModel().select(oldSelection);
					// set the event dispatcher back now that we've updated
					// the combobox.
					streamSelectionBox.setOnAction(edTmp);
				};
				Platform.runLater(guijob);
			} catch (Throwable e) {
				// we can't allow exceptions to be thrown out of this runnable.
				logMessage("System", "Failed to retrieve API data.");
				return;
			}
		};

		// schedule the periodic updates.
		scheduler.scheduleAtFixedRate(apiUpdateWork, 0,
				API_UPDATE_INTERVAL_SEC, TimeUnit.SECONDS);

		customizeGUIComponents();

	}

	@FXML
	private void streamSelection() {
		// combobox selection changed.
		final StreamData sdata = streamSelectionBox.getValue();
		if (sdata == null) {
			return;
		}
		Runnable guijob = () -> {
			if (!sdata.getLogoUrl().isEmpty()) {
				streamLogoImg.setImage(new Image(sdata.getLogoUrl()));
			} else {
				streamLogoImg.setImage(null);
			}
			streamLogoImg.setEffect(new DropShadow(20, Color.BLACK));
			streamPlayingLabel.setText(sdata.getGame());
			streamTitleLabel.setText(sdata.getStatus());
			streamViewersLabel.setText(sdata.getViewers());
			// set the gui tab text to include the name of the currently
			// selected channel.
			String name = sdata.getName();
			if (name.length() > 10) {
				name = name.substring(0, 10) + "..";
			}
			chatTab.setText("Chat - " + name);
			streamTab.setText("Stream - " + name);
		};
		Platform.runLater(guijob);

		if (ircHandler.isConnected()) {
			final String oldChannel = ircHandler.leaveCurrentChannel();
			final String newChannel = "#"
					+ sdata.getName().toLowerCase(Locale.US);
			if (oldChannel != null) {
				logMessage("System", "Left IRC channel > " + oldChannel);
			}
			ircHandler.joinChannel(newChannel);
			logMessage("System", "Joined IRC channel > " + newChannel);
			// clear the chat
			chatController.clearChat();

		}
	}

	@FXML
	private void watchSelectedStream() {
		try {

			final ProcessBuilder builder = new ProcessBuilder("livestreamer",
					"twitch.tv/" + streamSelectionBox.getValue(), streamQuality);
			builder.inheritIO();
			builder.start();
			logMessage("System", "Requested Livestreamer to open > twitch.tv/"
					+ streamSelectionBox.getValue());

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void logMessage(String sender, String message) {
		date.setTime(System.currentTimeMillis());
		if (Platform.isFxApplicationThread()) {
			logArea.appendText(dateFormat.format(date) + " " + sender + ": "
					+ message + "\n");
		} else {
			Runnable guijob = () -> {
				logArea.appendText(dateFormat.format(date) + " " + sender
						+ ": " + message + "\n");
			};
			Platform.runLater(guijob);
		}
	}

	private void setupIRCConnection() {
		String oldChannel = null;
		if (ircHandler != null) {
			// need to shutdown old irc connection.
			oldChannel = ircHandler.leaveCurrentChannel();
			if (oldChannel != null) {
				logMessage("System", "Left IRC channel > " + oldChannel);
			}
			ircHandler.shutdownGracefully();
			logMessage("System", "Disconnected from IRC server.");
		}
		ircHandler = new IRCHandler(settings.getUsername(),
				settings.getoAuth(), chatController);

		// connect the irchandler to irc server.
		logMessage("System", "Connecting to IRC server.");

		try {
			final int resp = ircHandler.connect();
			if (resp == -1) {
				logMessage("System",
						"Connecting to IRC server failed. > Incorrect username/oauth.");
			} else if (resp != 1) {
				logMessage("System",
						"Connecting to IRC server failed. > Unknown reason.");
			} else {
				logMessage("System", "Connecting to IRC server was successful.");
				// Rejoin the previous channel if we where in one.
				if (oldChannel != null) {
					ircHandler.joinChannel(oldChannel);
					logMessage("System", "Joined IRC channel > " + oldChannel);
				}
			}
		} catch (final Exception e2) {
			e2.printStackTrace();
		}
	}

	@FXML
	public void sendMessage() {
		String sendText = chatInput.getText();
		if (sendText != null && !sendText.trim().isEmpty()
				&& ircHandler.isConnected()) {
			ircHandler.sendMessage(sendText);
			chatController.chatMessage(settings.getUsername(), null, sendText);
			Runnable guijob = () -> {
				chatInput.clear();
			};
			Platform.runLater(guijob);
		}
	}

	private void retrieveAndSetEmoteIcons() {

		// We run the process on a separate thread since it can
		// take a while.
		Runnable iojob = () -> {
			HashMap<String, String> emotes = null;
			try {
				emotes = apiHandler.getAllChatEmotes();
			} catch (IOException e) {
				logMessage("System",
						"Failed to retrieve emotes > Emotes are disabled.");
				return;
			}
			chatController.setChatEmotes(emotes);
			logMessage("System", "Successfully retrieved chat emotes.");
		};

		// we don't want to do this on the edt or the scheduler since its most
		// likely a very long task.
		new Thread(iojob).start();
	}

	@FXML
	public void removeChannel() {
		ObservableList<StreamData> itemChoices = streamSelectionBox.getItems();
		StreamData choice = (StreamData) FXDialogs
				.showRemoveChannelDialog(itemChoices);
		if (choice == null) {
			return;
		}
		settings.removeChannel(choice.getName());
		// remove from gui as well.
		Runnable guijob = () -> {
			itemChoices.remove(choice);
		};
		logMessage("System", "Removed channel > " + choice.getName());
		Runnable iojob = () -> {
			settings.writeSettingsToJsonFile();
		};
		Platform.runLater(guijob);
		scheduler.execute(iojob);

	}

	@FXML
	public void addChannelManual() {
		String channelNames = FXDialogs.showAddChannelDialog();
		if (channelNames == null) {
			return;
		}
		String[] channels = channelNames.split("\\s*,\\s*");
		for (String cName : channels) {
			settings.addChannel(cName);
		}
		Runnable iojob = () -> {
			settings.writeSettingsToJsonFile();
		};
		scheduler.execute(iojob);
		scheduler.execute(apiUpdateWork);
		logMessage("System", "Added manually specified channels.");
	}

	@FXML
	public void addChannelAuto() {

		// We run the process on a separate thread since it can
		// take a while.
		Runnable job = () -> {
			List<String> followedChannels = null;
			try {
				followedChannels = apiHandler.getUserFollowedChannels(settings
						.getUsername());
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (String cName : followedChannels) {
				settings.addChannel(cName);
			}
			settings.writeSettingsToJsonFile();
			logMessage("System",
					"Added channels followed by > " + settings.getUsername());
		};
		scheduler.execute(job);
		scheduler.execute(apiUpdateWork);
	}

	@FXML
	private void aboutDialog() {
		FXDialogs.showAboutDialog();
	}

	@FXML
	private void changeAuth() {
		Pair<String, String> auth = FXDialogs.showAuthDialogPrompt();
		if (auth != null) {
			// disallow partial changes since username+auth are tied together.
			if (auth.getKey() != null && !auth.getKey().trim().isEmpty()
					&& auth.getValue() != null
					&& !auth.getValue().trim().isEmpty()) {

				settings.setUsername(auth.getKey());
				settings.setoAuth(auth.getValue());

				logMessage("System", "Username and OAuth was changed.");
				// We run the rest of the process on a separate thread since it
				// can take a while.
				Runnable iojob = () -> {
					setupIRCConnection();
					settings.writeSettingsToJsonFile();
				};
				scheduler.execute(iojob);
			}
		}

	}

	private void customizeGUIComponents() {

		// event handler that sends a chat message if enter is pressed while
		// using the inputfield.
		chatInput.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				if (keyEvent.getCode() == KeyCode.ENTER) {
					sendMessage();
				}
			}
		});

		// customize streamSelectionBox to correctly show streamData objects.
		streamSelectionBox
				.setCellFactory(new Callback<ListView<StreamData>, ListCell<StreamData>>() {
					@Override
					public ListCell<StreamData> call(ListView<StreamData> param) {
						final ListCell<StreamData> cell = new ListCell<StreamData>() {

							@Override
							public void updateItem(StreamData item,
									boolean empty) {
								super.updateItem(item, empty);
								if (item != null) {
									setText(item.getName());
									Rectangle rect = new Rectangle(14, 14);
									if (item.isOnline()) {
										rect.setFill(Color.CHARTREUSE);
									} else {
										rect.setFill(Color.GAINSBORO);
									}
									setGraphic(rect);
								} else {
									setText(null);
								}
							}
						};
						return cell;
					}
				});

		// Listener that changes the preferred stream quality when the
		// appropriate radiobottom is chosen in the gui.
		qualityToggleGroup.selectedToggleProperty().addListener(
				new ChangeListener<Toggle>() {
					@Override
					public void changed(ObservableValue<? extends Toggle> ov,
							Toggle t, Toggle t1) {
						if (qualityToggleGroup.getSelectedToggle() != null) {
							streamQuality = qualityToggleGroup
									.getSelectedToggle().getUserData()
									.toString();
							logMessage("System",
									"Stream quality priority changed to > "
											+ streamQuality);
						}
					}
				});
	}

}