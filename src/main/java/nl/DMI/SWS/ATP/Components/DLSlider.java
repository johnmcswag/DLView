package nl.DMI.SWS.ATP.Components;

import javafx.application.Platform;
import nl.DMI.SWS.ATP.Models.N3300AModule;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Optional;

import static nl.DMI.SWS.ATP.Util.Math.toFixed;

public class DLSlider {

    private double sliderValue = 0.0;
    private final double steps = 0.1;
    private VBox container;
    private N3300AModule load;
    private boolean isEnabled = false;
    public DLSlider(N3300AModule load) {
        this.load = load;
        load.setSlider(this);

        container = new VBox(8);
        container.setPrefWidth(120);
        container.setPadding(new Insets(8));
        container.setAlignment(Pos.TOP_CENTER);

        Label loadLabel = createLabel();
        Label voltageLabel = createLabel();
        Label currentLabel = createLabel();

        loadLabel.getStyleClass().add("info");
        voltageLabel.getStyleClass().add("info");
        currentLabel.getStyleClass().add("info");

        loadLabel.setText("DISABLED");

        container.getChildren().addAll(loadLabel, voltageLabel, currentLabel);

        VBox sliderControlContainer = new VBox(8);
        Label maxCurrentLabel = createLabel();
        Label minCurrentLabel = createLabel();
        maxCurrentLabel.getStyleClass().add("slider");
        minCurrentLabel.getStyleClass().add("slider");
        maxCurrentLabel.setText("0.1");
        minCurrentLabel.setText("0");

        Slider slider = createSlider();
        slider.setDisable(true);
        sliderControlContainer.getChildren().addAll(maxCurrentLabel, slider, minCurrentLabel);
        sliderControlContainer.setAlignment(Pos.TOP_CENTER);
        sliderControlContainer.getStyleClass().add("controlContainer");
        VBox.setVgrow(sliderControlContainer, Priority.ALWAYS);


        Button setButton = new Button("SET");
        setButton.setDisable(true);
        setButton.setMaxWidth(Double.MAX_VALUE);
        setButton.setAlignment(Pos.CENTER);
        setButton.setOnAction((value) -> {

            Dialog<Double> dialog = new Dialog<>();
            dialog.setTitle(load.toString());
            ButtonType apply = new ButtonType("Apply", ButtonData.APPLY);
            ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(apply, cancel);
            final Button okButton = (Button) dialog.getDialogPane().lookupButton(apply);
            okButton.setDisable(true);

            HBox content = new HBox(16);
            Text text = new Text("Input new max value");
            TextField input = new TextField();
            input.setOnAction((event) -> okButton.fire());
            input.textProperty().addListener((observable, oldValue, newValue) -> {
                okButton.setDisable(!newValue.matches("^[0-9]+(\\.[0-9])?$"));
            });

            content.getChildren().addAll(text, input);
            dialog.getDialogPane().setContent(content);
            dialog.setResultConverter(button -> {
                if(button == apply) {
                    return Double.parseDouble(input.getText());
                }
                return null;
            });

            Platform.runLater(input::requestFocus);

            Optional<Double> dialogValue = dialog.showAndWait();
            if(dialogValue.isPresent()) {
                double newValue = dialogValue.get();
                maxCurrentLabel.setText(newValue + "");
                slider.setMax(newValue);
            }
        });

        ToggleSwitch toggle = new ToggleSwitch();
        toggle.switchOnProperty().addListener(((observable, oldValue, newValue) -> toggleDLSlider(newValue)));

        VBox controlContainer = new VBox(8);
        controlContainer.getChildren().addAll(sliderControlContainer, setButton, toggle);
        controlContainer.setFillWidth(true);

        HBox controlWrapper = new HBox();
        controlWrapper.setAlignment(Pos.CENTER);
        controlWrapper.getChildren().add(controlContainer);
        VBox.setVgrow(controlWrapper, Priority.ALWAYS);
        container.getChildren().add(controlWrapper);
    }

    private void toggleDLSlider(boolean state) {
        System.out.println("toggleDLSlider");
        System.out.println("State: " + state);
        this.isEnabled = state;
    }

    private Label createLabel() {
        Label label = new Label("INIT");
        label.setFont(Font.font(18));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        label.getStyleClass().add("sliderLabel");
        return label;
    }

    private Slider createSlider() {
        Slider slider = new Slider(0, 0.1, 0) {
            Text text;

            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                if (text == null) {
                    text = new Text(getValue() + "");
                    valueProperty().addListener((obs, old, val) -> text.setText(toFixed((Double) val, 1) + ""));
                    StackPane thumb = (StackPane) lookup(".thumb");
                    thumb.setPadding(new Insets(10));
                    thumb.getChildren().add(text);
                }
            }
        };
        slider.setOrientation(Orientation.VERTICAL);
        slider.setBlockIncrement(steps);

        slider.setMajorTickUnit(steps);
        slider.setMinorTickCount(0);
        slider.setSnapToTicks(true);
        VBox.setVgrow(slider, Priority.ALWAYS);
        return slider;
    }

    public void unload() {}

    public boolean isEnabled() {
        return isEnabled;
    }
    public VBox getContainer() {
        return container;
    }

    public N3300AModule getLoad() {
        return load;
    }

    public double getSliderValue() {
        return sliderValue;
    }

    public void setSliderValue(double sliderValue) {
        this.sliderValue = sliderValue;
    }
}
