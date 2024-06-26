package nl.DMI.SWS.ATP.Service;

import nl.DMI.SWS.ATP.Components.DLSlider;
import nl.DMI.SWS.ATP.DTO.InstrumentInfoDTO;
import nl.DMI.SWS.ATP.Enum.ToastType;
import nl.DMI.SWS.ATP.Exception.InstrumentException;
import nl.DMI.SWS.ATP.Models.N3300A;
import nl.DMI.SWS.ATP.Models.N3300AModule;
import nl.DMI.SWS.ATP.Singleton.ResourceManager;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import nl.DMI.SWS.ATP.Util.ToastManager;
import xyz.froud.jvisa.JVisaResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static nl.DMI.SWS.ATP.Util.Math.toFixed;

public class N3300AService extends ThreadService {
    private N3300A DLLoad1;
    private N3300A DLLoad2;
    private List<N3300AModule> loads = new ArrayList<>();
    private JVisaResourceManager rm;

    public N3300AService() {
        taskPeriod_ms = 100;
        rm = ResourceManager.getResourceManager();
        startExecutor();
    }

    public List<N3300A> discoverLoads() {
        List<InstrumentInfoDTO> instruments = InstrumentService.discover();
        List<InstrumentInfoDTO> DLResources = instruments.stream().filter(i -> i.getIDN().contains("N3300A")).collect(Collectors.toList());
        List<N3300A> DLLoads = new ArrayList<>();
        try {
            if(DLResources.size() != 2) throw new InstrumentException("Dynamic load missing. Are both turned on?");
            DLLoad1 = new N3300A(rm, DLResources.get(0).getResourceName());
            DLLoad2 = new N3300A(rm, DLResources.get(1).getResourceName());
            DLLoads.add(DLLoad1);
            DLLoads.add(DLLoad2);

            loads.addAll(DLLoad1.LOADS.values());
            loads.addAll(DLLoad2.LOADS.values());
        } catch (InstrumentException e) {
            ToastManager.showToast(e.getMessage(), ToastType.ERROR);
            System.out.println(e.getMessage());
            System.out.println(e);
        }

        return DLLoads;
    }

    public N3300A getDLLoad1() {
        return DLLoad1;
    }

    public N3300A getDLLoad2() {
        return DLLoad2;
    }

    public List<N3300AModule> getLoads() {
        return loads;
    }

    @Override
    public void close() {
        super.close();
        try {
            if(DLLoad1 != null) DLLoad1.close();
            if(DLLoad2 != null) DLLoad2.close();
        } catch (Exception e) {
            System.out.println("Error closing Dynamic loads.");
            System.out.println(e);
        }
    }

    @Override
    protected void updateTask() {
        try {
            for (N3300AModule load : loads) {
                if (task.isCancelled() || task.isCancelled()) return;
                DLSlider slider = load.getSlider();
                if (slider.isEnabled() != load.isEnabled()) toggleDLSlider(slider);
                if (!slider.isEnabled()) continue;
                measureVoltTask(slider);
                measureCurrTask(slider);
                setCurrTask(slider);
            }
        } catch (Exception e) {
            System.out.println("Error in updateTask");
            System.out.println(e);
        }
    }

    private void measureVoltTask(DLSlider slider) {
        if(task.isCancelled() || task.isCancelled()) return;
        VBox sliderContainer = slider.getContainer();
        N3300AModule load = slider.getLoad();
        try {
            double voltage = load.getVoltage();
            Platform.runLater(() -> {
                Label voltageLabel = (Label) sliderContainer.getChildren().get(1);
                voltageLabel.setText(voltage + "");
            });
        } catch (InstrumentException e) {
            System.out.println("Error measuring voltage. " + load);
            System.out.println(e.getMessage());
        }
    }

    private void measureCurrTask(DLSlider slider) {
        if(task.isCancelled() || task.isCancelled()) return;
        VBox sliderContainer = slider.getContainer();
        N3300AModule load = slider.getLoad();
        try {
            double current = load.getCurrent();
            Platform.runLater(() -> {

                Label currentLabel = (Label) sliderContainer.getChildren().get(2);
                currentLabel.setText(current + "");
            });
        } catch (InstrumentException e) {
            System.out.println("Error measuring current. " + load);
            System.out.println(e.getMessage());
        }

    }

    private void setCurrTask(DLSlider slider) {
        if(task.isCancelled() || task.isCancelled()) return;
        VBox sliderContainer = slider.getContainer();
        N3300AModule load = slider.getLoad();
        try {
            HBox controlWrapper = (HBox) sliderContainer.getChildren().get(3);
            VBox controlContainer = (VBox) controlWrapper.getChildren().get(0);
            Slider sliderControl = (Slider) ((VBox) controlContainer.getChildren().get(0)).getChildren().get(1);

            double sliderControlValue = toFixed(sliderControl.getValue(), 1);
            double sliderValue = slider.getSliderValue();
            double nearestTick = Math.round(sliderControlValue / 0.1) * 0.1;
            if (Math.abs(nearestTick - sliderValue) >= 0.1 - 0.01) {
                slider.setSliderValue(sliderControlValue);
                try {
                    load.setCurrent(sliderControlValue);
                } catch (InstrumentException e) {
                    Platform.runLater(() -> {
                        System.out.println("Error setting current to level: " + sliderControlValue);
                        System.out.println(e.getMessage());
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("Error in setCurrTask");
            System.out.println(e);
        }
    }

    private void toggleDLSlider(DLSlider slider) {
        if(task.isCancelled() || task.isCancelled()) return;
        VBox sliderContainer = slider.getContainer();
        N3300AModule load = slider.getLoad();

        HBox controlWrapper = (HBox) sliderContainer.getChildren().get(3);
        VBox controlContainer = (VBox) controlWrapper.getChildren().get(0);

        Slider sliderControl = (Slider) ((VBox) controlContainer.getChildren().get(0)).getChildren().get(1);
        Button set = (Button) controlContainer.getChildren().get(1);
        Label loadLabel = (Label) sliderContainer.getChildren().get(0);
        boolean state = slider.isEnabled();

        try {
            if(state) {
                load.enable();
            } else {
                load.disable();
            }
        } catch (InstrumentException e) {
            System.out.println("Issue toggling " + load);
            System.out.println(e.getMessage());
        }

        Platform.runLater(() -> {
            set.setDisable(!state);
            sliderControl.setDisable(!state);

            if(state) {
                loadLabel.setText(load.toString());
            } else {
                loadLabel.setText("DISABLED");
                sliderControl.setValue(0.0);
            }
        });

    }
}
