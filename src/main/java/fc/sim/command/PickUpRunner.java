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

    public PickUpRunner(RobotRunner robotRunner, String rackColor) {
        this.robotRunner = robotRunner;
        this.rackColor = rackColor;
    }

    @Override
    public void run() {

        robotRunner.ready = false;

        LOGGER.info("PickUp started: " + rackColor);

        StepStatus stepStatus = new StepStatus();
        robotRunner.stepStatusMap.put(robotRunner.runId + "." + robotRunner.step.stepId, stepStatus);
        robotRunner.stepStatusList.add(stepStatus);
        stepStatus.runId = robotRunner.runId;
        stepStatus.stepId = robotRunner.step.stepId;
        stepStatus.instruction = robotRunner.step.instruction;

        stepStatus.state = "running";
        stepStatus.startedAt = new Date();

        try { Thread.sleep(robotRunner.COMMAND_LATENCY); } catch (InterruptedException e) { throw new RuntimeException(e); }

        if(SimApplication.workbench.tubeInGripper != null) {

            LOGGER.info("Gripper occupied: " + SimApplication.workbench.tubeInGripper);
            stepStatus.state = "failed";
            stepStatus.endedAt = new Date();
            stepStatus.error = "gripper occupied";

        }
        else {

            String[] tubeRack = SimApplication.workbench.tubeRackMap.get(rackColor);
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
                stepStatus.state = "complete";
                stepStatus.endedAt = new Date();
                StepStatus.RackObservation observation = new StepStatus.RackObservation();
                stepStatus.observations = new HashMap<>();
                stepStatus.observations.put("pickup", observation);
                observation.labware = rackColor;
                observation.position = String.valueOf(index);
            } else {
                LOGGER.info("PickUp did not find a tube");
                stepStatus.state = "failed";
                stepStatus.endedAt = new Date();
                stepStatus.error = "empty rack";
            }

        }

        robotRunner.step = null;
        robotRunner.ready = true;

    }

}
