package guiModule;

import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.util.Pair;

public class FXDialogs {

	public static Pair<String, String> showAuthDialogPrompt() {
		// Create the custom login dialog.
		Dialog<Pair<String, String>> dialog = new Dialog<>();
		dialog.setTitle("Twitch Authentication");
		dialog.setHeaderText("Login as:");

		// Set the button types.
		ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes()
				.addAll(loginButtonType, ButtonType.CANCEL);

		// Create the username and password labels and fields.
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		TextField username = new TextField();
		username.setPromptText("Twitch Username");
		PasswordField password = new PasswordField();
		password.setPromptText("oAuth - http://twitchapps.com/tmi/");

		grid.add(new Label("Username:"), 0, 0);
		grid.add(username, 1, 0);
		grid.add(new Label("oAuth:"), 0, 1);
		grid.add(password, 1, 1);
		Label warnlbl = new Label(
				"oAuth is NOT your Twitch.tv account password!\nIt is generated at"
						+ " http://twitchapps.com/tmi/ and is required for chat.");
		warnlbl.setUnderline(true);
		grid.add(warnlbl, 0, 2, 2, 1);

		// Enable/Disable login button depending on whether a username was
		// entered.
		Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
		loginButton.setDisable(true);

		// Do some validation
		ChangeListener<String> cl = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> arg0,
					String arg1, String arg2) {
				loginButton.setDisable(username.getText().trim().isEmpty()
						|| password.getText().trim().isEmpty());
			}
		};

		username.textProperty().addListener(cl);
		password.textProperty().addListener(cl);

		dialog.getDialogPane().setContent(grid);

		// Request focus on the username field by default.
		Platform.runLater(() -> username.requestFocus());

		// Convert the result to a username-password-pair when the login button
		// is clicked.
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == loginButtonType) {
				return new Pair<>(username.getText(), password.getText());
			}
			return null;
		});

		Optional<Pair<String, String>> result = dialog.showAndWait();

		if (result.isPresent()) {
			return result.get();
		}
		return null;
	}

	public static void showAboutDialog() {

		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About TSwitch(TwitchSwitch)");
		alert.setHeaderText("TSwitch - BETA 2");
		alert.setContentText("TSwitch is a Livestreamer"
				+ " and Twitch.tv frontend client. It was developed by Emanuel Y. Lindgren"
				+ " (twitchswitch.contact@gmail.com)");

		alert.showAndWait();
	}

	public static Object showRemoveChannelDialog(ObservableList<?> choices) {
		ChoiceDialog<?> dialog = new ChoiceDialog<>(null, choices);
		dialog.setTitle("Remove a Channel");
		dialog.setHeaderText("Select a channel to remove.");

		Optional<?> result = dialog.showAndWait();

		if (result.isPresent()) {
			return result.get();
		}
		return null;

	}

	public static String showAddChannelDialog() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Add a Channel");
		dialog.setHeaderText("Specify the channels to add.");
		// since this dialog puts the input and and text content on the same
		// line and doesn't care about newline chars we have to "inject" a new
		// content node that does.
		Node oldC = dialog.getDialogPane().getContent();
		Text infoText = new Text("Multiple channels may be added as\n"
				+ "a comma seperated list.\n"
				+ "(Ex. channel1,channel2,channel3..)");
		FlowPane newC = new FlowPane();

		newC.getChildren().addAll(infoText, oldC);
		dialog.getDialogPane().setContent(newC);

		Optional<String> result = dialog.showAndWait();

		if (result.isPresent()) {
			return result.get();
		}
		return null;

	}

	public static boolean showConfirmHyperlinkDialog(String url) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Confirm hyperlink");
		alert.setHeaderText("Do you wish to open this Hyperlink?");
		alert.setContentText(url);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			return true;
		}
		return false;
	}
}
