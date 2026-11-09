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
    private StepStatus stepStatus;

    public ReadBarcodeRunner(RobotRunner robotRunner) {
        this.robotRunner = robotRunner;
        this.stepStatus = robotRunner.stepStatus;
    }

    @Override
    public void run() {

        LOGGER.info("ReadBarCode started");

        try { Thread.sleep(robotRunner.COMMAND_LATENCY); } catch (InterruptedException e) { LOGGER.info("ReadBarCode cancelled"); return; }

        synchronized(robotRunner) {

            if(robotRunner.stepStatus != stepStatus) {
                return;
            }

            if(SimApplication.workbench.tubeInGripper == null) {

                LOGGER.info("Gripper not occupied");
                stepStatus.state = "failed";
                stepStatus.endedAt = new Date();
                stepStatus.error = new StepStatus.StepError();
                stepStatus.error.code = "GRIPPER_EMPTY";
                stepStatus.error.message = "gripper not occupied";

            }
            else {

                LOGGER.info("ReadBarCode read tube: " + SimApplication.workbench.tubeInGripper);
                stepStatus.state = "completed";
                stepStatus.endedAt = new Date();
                stepStatus.observations = new HashMap<>();
                stepStatus.observations.put("barcode", SimApplication.workbench.tubeInGripper);
                stepStatus.detail = "Read barcode " + SimApplication.workbench.tubeInGripper + ".";

            }

            robotRunner.clearStep();

        }

    }

}
