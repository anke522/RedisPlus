package com.maxbill.core.desktop;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DesktopApp extends Application {

    private final Integer prefWidth = 1300;
    private final Integer prefHeight = 800;
    private final Integer titlePrefWidth = 30;
    private final Integer titlePrefHeight = 30;
    private final String appName = "RedisStudio";
    private final String appShow = "/static/styles/winui.css";
    private final String appPath = "http://127.0.0.1:9999/root";
    private final String appIcon = "/static/image/task-logo.png";


    @Override
    public void start(Stage stage) {
        //this.useMyselfStage(stage);
        this.useDefaultStage(stage);
    }

    /**
     * 默认标题栏实现
     */
    public void useDefaultStage(Stage stage) {
        stage.setTitle(appName);
        stage.setScene(new Scene(getWebView()));
        stage.getIcons().add(new Image(appIcon));
        stage.setResizable(false);
        stage.show();
    }


    /**
     * 自定义标题栏实现
     */
    public void useMyselfStage(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(new Scene(getWinRoot(stage)));
        stage.getIcons().add(new Image(appIcon));
        stage.setResizable(false);
        stage.show();
    }

    /**
     * 自定义主窗体
     */
    private VBox getWinRoot(Stage stage) {
        //主题窗口
        VBox winRoot = new VBox();
        winRoot.setId("winRoot");
        String style = this.getClass().getResource(appShow).toString();
        winRoot.getStylesheets().add(style);
        //内容窗口
        VBox content = new VBox();
        content.getChildren().addAll(getWebView());
        winRoot.getChildren().addAll(getTopView(stage), content);
        return winRoot;
    }

    /**
     * 页面视图
     */
    private WebView getWebView() {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webView.setPrefSize(prefWidth, prefHeight);
        webEngine.load(appPath);
        return webView;
    }

    /**
     * 自定顶部窗体
     */
    private VBox getTopView(Stage stage) {
        // 顶部视图
        VBox topView = new VBox();
        topView.setId("topView");
        topView.setPrefSize(prefWidth, titlePrefHeight);
        // 标题栏视图
        AnchorPane titleView = new AnchorPane();
        // 标题图标
        Label winImage = new Label();
        winImage.setPrefSize(titlePrefWidth, titlePrefHeight);
        winImage.setId("winImage");
        titleView.getChildren().add(winImage);
        // 标题文字
        Label winTitle = new Label();
        winTitle.setPrefSize(100, titlePrefHeight);
        winTitle.setId("winTitle");
        winTitle.setText(appName);
        titleView.getChildren().add(winTitle);
        // 最小化按钮
        Label winAbate = new Label();
        winAbate.setPrefSize(titlePrefWidth, titlePrefHeight);
        winAbate.setId("winAbate");
        winAbate.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    stage.close();
                }
            }
        });
        titleView.getChildren().add(winAbate);
        // 关闭按钮
        Label winClose = new Label();
        winClose.setPrefSize(titlePrefWidth, titlePrefHeight);
        winClose.setId("winClose");
        winClose.setOnMouseClicked(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    stage.setIconified(true);
                }
            }
        });
        titleView.getChildren().add(winClose);
        // 顶部布局
        AnchorPane.setLeftAnchor(winImage, 5.0);
        AnchorPane.setLeftAnchor(winTitle, 40.0);
        AnchorPane.setRightAnchor(winAbate, 40.0);
        AnchorPane.setRightAnchor(winClose, 5.0);
        topView.getChildren().add(titleView);
        return topView;
    }


}
