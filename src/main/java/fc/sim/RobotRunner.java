package fc.sim;

import fc.sim.command.PickUpRunner;
import fc.sim.command.PutDownRunner;
import fc.sim.command.ReadBarcodeRunner;
import fc.sim.data.Step;
import fc.sim.data.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RobotRunner implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(RobotRunner.class);

    public Boolean ready = true;
    public String runId = null;
    public Step step = null;
    public Map<String, StepStatus> stepStatusMap = new HashMap<>();

    private Pattern pickUp = Pattern.compile("Pick up a tube from the (.*) rack.");
    private Pattern putDown = Pattern.compile("Place the tube you are holding in an open spot in the (.*) rack.");
    private Pattern scan = Pattern.compile("Scan the barcode of the tube you are holding.");

    public final int COMMAND_LATENCY = 5000;

    @Override
    public void run() {

        LOGGER.info("RobotRunner thread started");

        while(true) {

            try {

                if(ready && (step != null)) {

                    LOGGER.info("Dispatching instruction: " + step.instruction);

                    stepStatusMap.remove(runId + "." + step.stepId);

                    Matcher matcherPickup = pickUp.matcher(step.instruction);
                    Matcher matcherPutDown = putDown.matcher(step.instruction);
                    Matcher matcherScan = scan.matcher(step.instruction);

                    if(matcherPickup.matches()) {
                        PickUpRunner pickupRunner = new PickUpRunner(this, matcherPickup.group(1));
                        Thread pickUpRunnerThread = new Thread(pickupRunner);
                        pickUpRunnerThread.start();
                    }

                    else if(matcherPutDown.matches()) {
                        PutDownRunner putDownRunner = new PutDownRunner(this, matcherPutDown.group(1));
                        Thread putDownRunnerThread = new Thread(putDownRunner);
                        putDownRunnerThread.start();
                    }

                    else if(matcherScan.matches()) {
                        ReadBarcodeRunner readBarcodeRunner = new ReadBarcodeRunner(this);
                        Thread readBarCodeRunnerThread = new Thread(readBarcodeRunner);
                        readBarCodeRunnerThread.start();
                    }

                    else {
                        LOGGER.info("I don't know how to parse instruction: " + step.instruction);
                        StepStatus stepStatus = new StepStatus();
                        stepStatusMap.put(runId + "." + step.stepId, stepStatus);
                        stepStatus.runId = runId;
                        stepStatus.stepId = step.stepId;
                        stepStatus.instruction = step.instruction;
                        stepStatus.state = "failed";
                        stepStatus.endedAt = new Date();
                        stepStatus.error = "unparseable instruction";
                        step = null;
                        ready = true;
                    }

                }

                Thread.sleep(1000);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

    }

    public boolean isReady() {
        return ready;
    }

    public void setStep(String runId, Step step) {
        if(ready) {
            this.runId = runId;
            this.step = step;
        }
        else {
            throw new  RuntimeException("Robot is not ready to accept a command");
        }
    }

    StepStatus getStepStatus(String runId, String stepId) {
        return stepStatusMap.get(runId + "." + stepId);
    }

}