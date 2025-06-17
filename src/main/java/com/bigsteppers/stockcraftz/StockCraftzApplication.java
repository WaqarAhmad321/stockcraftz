package com.bigsteppers.stockcraftz;

import com.bigsteppers.stockcraftz.interfaces.LoadablePage;
import com.bigsteppers.stockcraftz.model.SessionManager;
import com.bigsteppers.stockcraftz.utils.FXUtils;
import com.dbfx.database.DBConnectionPool;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StockCraftzApplication extends Application {

    public static final String RESOURCES_PATH = "/com/bigsteppers/stockcraftz/images";
    private final Map<String, Object> controllers = new HashMap<>();
    private Stage primaryStage;
    private Map<String, Scene> scenes = new HashMap<>();

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        DBConnectionPool.initializeDB("jdbc:postgresql://ep-withered-voice-a5udta9w-pooler.us-east-2.aws.neon.tech/stockcraftzdb?user=stockcraftzdb_owner&password=npg_lau6ZiYgo7Gh&sslmode=require", "stockcraftzdb", "npg_lau6ZiYgo7Gh");
        stage.setTitle("StockCraftz");
//        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/")));
        Font.loadFont(getClass().getResource("/com/bigsteppers/stockcraftz/fonts/VT323-Regular.ttf").toExternalForm(), 10);

        // Load all scenes
        String[] pages = {"login", "register", "dashboard", "menu", "raw_materials", "crafted_items", "crafting", "leaderboard", "marketplace"};
        for (String page : pages) {
            loadScene(page, "/com/bigsteppers/stockcraftz/fxml/" + page + ".fxml");
        }

        // Show initial scene
        showPage("raw_materials");
        stage.show();
    }

    private void loadScene(String name, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        var root = loader.load();

        ScrollPane scrollPane = new ScrollPane((Node) root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane, 900, 768);
        scene.getStylesheets().add(getClass().getResource("/com/bigsteppers/stockcraftz/css/styles.css").toExternalForm());

        scenes.put(name, scene);
        controllers.put(name, loader.getController());

        scene.addPreLayoutPulseListener(() -> {
            if (!SessionManager.isLoggedIn() && !name.equals("register") && !name.equals("login")) {
                FXUtils.navigateTo("login", scene.getRoot());
            }
        });

        scene.getRoot().setUserData(this);
    }

    public void showPage(String pageName) {
        Scene scene = scenes.get(pageName);
        if (scene != null) {
            primaryStage.setScene(scene);

            Object controller = controllers.get(pageName);
            if (controller instanceof LoadablePage loadable) {
                loadable.onLoad(); // run every time scene is shown
            }
        }
    }
}