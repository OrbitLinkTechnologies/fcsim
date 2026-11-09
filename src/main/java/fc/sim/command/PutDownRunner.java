package fc.sim.command;

import fc.sim.RobotRunner;
import fc.sim.SimApplication;
import fc.sim.data.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

public class PutDownRunner implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(PutDownRunner.class);

    private RobotRunner robotRunner;
    private String rackColor;
    private StepStatus stepStatus;

    public PutDownRunner(RobotRunner robotRunner, String rackColor) {
        this.robotRunner = robotRunner;
        this.rackColor = rackColor;
        this.stepStatus = robotRunner.stepStatus;
    }

    @Override
    public void run() {

        LOGGER.info("PutDown started: " + rackColor);

        try { Thread.sleep(robotRunner.COMMAND_LATENCY); } catch (InterruptedException e) { LOGGER.info("PutDown cancelled"); return; }

        synchronized(robotRunner) {

            if(robotRunner.stepStatus != stepStatus) {
                return;
            }

            String[] tubeRack = SimApplication.workbench.tubeRackMap.get(rackColor);

            if(SimApplication.workbench.tubeInGripper == null) {

                LOGGER.info("Gripper not occupied");
                stepStatus.state = "failed";
                stepStatus.endedAt = new Date();
                stepStatus.error = new StepStatus.StepError();
                stepStatus.error.code = "GRIPPER_EMPTY";
                stepStatus.error.message = "gripper not occupied";

            }
            else if(tubeRack == null) {

                LOGGER.info("PutDown did not find a " + rackColor + " rack");
                stepStatus.state = "failed";
                stepStatus.endedAt = new Date();
                stepStatus.error = new StepStatus.StepError();
                stepStatus.error.code = "UNKNOWN_LABWARE";
                stepStatus.error.message = "no " + rackColor + " rack";

            }
            else {

                int index = -1;
                for (int i = 0; i < tubeRack.length; i++) {
                    if (tubeRack[i] == null) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    LOGGER.info("PutDown found empty position for: " + SimApplication.workbench.tubeInGripper + " at position " + index);
                    tubeRack[index] = SimApplication.workbench.tubeInGripper;
                    SimApplication.workbench.tubeInGripper = null;
                    stepStatus.state = "completed";
                    stepStatus.endedAt = new Date();
                    StepStatus.RackObservation observation = new StepStatus.RackObservation();
                    stepStatus.observations = new HashMap<>();
                    stepStatus.observations.put("placed_at", observation);
                    observation.labware = rackColor + " rack";
                    observation.position = "A" + (index + 1);
                    stepStatus.detail = "Placed " + tubeRack[index] + " in " + observation.labware + " " + observation.position + ".";
                } else {
                    LOGGER.info("PickUp did not find an empty position");
                    stepStatus.state = "failed";
                    stepStatus.endedAt = new Date();
                    stepStatus.error = new StepStatus.StepError();
                    stepStatus.error.code = "RACK_FULL";
                    stepStatus.error.message = "full rack";
                }

            }

            robotRunner.clearStep();

        }

    }

}
