package fc.sim.command;

import fc.sim.RobotRunner;
import fc.sim.SimApplication;
import fc.sim.data.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class ReadBarcodeRunner implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReadBarcodeRunner.class);

    private RobotRunner robotRunner;

    public ReadBarcodeRunner(RobotRunner robotRunner) {
        this.robotRunner = robotRunner;
    }

    @Override
    public void run() {

        robotRunner.ready = false;

        LOGGER.info("ReadBarCode started");

        StepStatus stepStatus = new StepStatus();
        robotRunner.stepStatusMap.put(robotRunner.runId + "." + robotRunner.step.stepId, stepStatus);
        robotRunner.stepStatusList.add(stepStatus);
        stepStatus.runId = robotRunner.runId;
        stepStatus.stepId = robotRunner.step.stepId;
        stepStatus.instruction = robotRunner.step.instruction;

        stepStatus.state = "running";
        stepStatus.startedAt = new Date();

        try { Thread.sleep(robotRunner.COMMAND_LATENCY); } catch (InterruptedException e) { throw new RuntimeException(e); }

        if(SimApplication.workbench.tubeInGripper == null) {

            LOGGER.info("Gripper not occupied");
            stepStatus.state = "failed";
            stepStatus.endedAt = new Date();
            stepStatus.error = "gripper not occupied";

        }
        else {

            LOGGER.info("ReadBarCode read tube: " + SimApplication.workbench.tubeInGripper);
            stepStatus.state = "complete";
            stepStatus.endedAt = new Date();
            StepStatus.BarCodeObservation observation = new StepStatus.BarCodeObservation();
            stepStatus.observations = new HashMap<>();
            stepStatus.observations.put("readBarcode", observation);
            observation.barcode = SimApplication.workbench.tubeInGripper;

        }

        robotRunner.step = null;
        robotRunner.ready = true;

    }

}
