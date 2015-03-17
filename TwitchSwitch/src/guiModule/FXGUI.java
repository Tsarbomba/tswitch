package guiModule;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FXGUI extends Application {
	public static Application instance;

	@Override
	public void start(Stage primaryStage) {
		try {

			Parent root = FXMLLoader.load(getClass().getResource(
					"/resources/GuiLayout.fxml"));
			instance = this;
			Scene scene = new Scene(root);
			scene.getStylesheets().add(
					getClass().getResource("/resources/chatStyle.css")
							.toString());
			primaryStage.setTitle("TSwitch GUI");
			primaryStage.setResizable(false);
			primaryStage.setScene(scene);
			primaryStage.show();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void stop() {
		System.exit(0);
	}
}
