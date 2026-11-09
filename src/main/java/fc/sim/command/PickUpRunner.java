package fc.sim.command;

import fc.sim.RobotRunner;
import fc.sim.SimApplication;
import fc.sim.data.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class PickUpRunner implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(PickUpRunner.class);

    private RobotRunner robotRunner;
    private String rackColor;
    private StepStatus stepStatus;

    public PickUpRunner(RobotRunner robotRunner, String rackColor) {
        this.robotRunner = robotRunner;
        this.rackColor = rackColor;
        this.stepStatus = robotRunner.stepStatus;
    }

    @Override
    public void run() {

        LOGGER.info("PickUp started: " + rackColor);

        try { Thread.sleep(robotRunner.COMMAND_LATENCY); } catch (InterruptedException e) { LOGGER.info("PickUp cancelled"); return; }

        synchronized(robotRunner) {

            if(robotRunner.stepStatus != stepStatus) {
                return;
            }

            String[] tubeRack = SimApplication.workbench.tubeRackMap.get(rackColor);

            if(SimApplication.workbench.tubeInGripper != null) {

                LOGGER.info("Gripper occupied: " + SimApplication.workbench.tubeInGripper);
                stepStatus.state = "failed";
                stepStatus.endedAt = new Date();
                stepStatus.error = new StepStatus.StepError();
                stepStatus.error.code = "GRIPPER_OCCUPIED";
                stepStatus.error.message = "gripper occupied";

            }
            else if(tubeRack == null) {

                LOGGER.info("PickUp did not find a " + rackColor + " rack");
                stepStatus.state = "failed";
                stepStatus.endedAt = new Date();
                stepStatus.error = new StepStatus.StepError();
                stepStatus.error.code = "UNKNOWN_LABWARE";
                stepStatus.error.message = "no " + rackColor + " rack";

            }
            else {

                int index = -1;
                for (int i = 0; i < tubeRack.length; i++) {
                    if (tubeRack[i] != null) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    LOGGER.info("PickUp found tube: " + tubeRack[index] + " in position " + index);
                    SimApplication.workbench.tubeInGripper = tubeRack[index];
                    tubeRack[index] = null;
                    stepStatus.state = "completed";
                    stepStatus.endedAt = new Date();
                    StepStatus.RackObservation observation = new StepStatus.RackObservation();
                    stepStatus.observations = new HashMap<>();
                    stepStatus.observations.put("picked_from", observation);
                    observation.labware = rackColor + " rack";
                    observation.position = "A" + (index + 1);
                    stepStatus.detail = "Picked up " + SimApplication.workbench.tubeInGripper + " from " + observation.labware + " " + observation.position + ".";
                } else {
                    LOGGER.info("PickUp did not find a tube");
                    stepStatus.state = "failed";
                    stepStatus.endedAt = new Date();
                    stepStatus.error = new StepStatus.StepError();
                    stepStatus.error.code = "NO_TUBE_FOUND";
                    stepStatus.error.message = "empty rack";
                }

            }

            robotRunner.clearStep();

        }

    }

}
