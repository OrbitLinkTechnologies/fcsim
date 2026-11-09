package fc.sim;

import fc.sim.command.PickUpRunner;
import fc.sim.command.PutDownRunner;
import fc.sim.command.ReadBarcodeRunner;
import fc.sim.data.Step;
import fc.sim.data.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RobotRunner implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(RobotRunner.class);

    public Boolean ready = true;
    public String runId = null;
    public Step step = null;
    public StepStatus stepStatus = null;
    public Thread commandThread = null;
    public Map<String, StepStatus> stepStatusMap = new HashMap<>();
    public List<StepStatus> stepStatusList = new ArrayList<>();
    public Map<String, String> runStateMap = new HashMap<>();

    private Pattern pickUp = Pattern.compile("Pick up a tube from the (.*) rack.");
    private Pattern putDown = Pattern.compile("Place the tube you are holding in an open spot in the (.*) rack.");
    private Pattern scan = Pattern.compile("Scan the barcode of the tube you are holding.");

    public final int COMMAND_LATENCY = 5000;

    @Override
    public void run() {

        LOGGER.info("RobotRunner thread started");

        while(true) {

            try {

                synchronized(this) {

                    if(ready && (step != null)) {

                        LOGGER.info("Dispatching instruction: " + step.instruction);

                        Matcher matcherPickup = pickUp.matcher(step.instruction);
                        Matcher matcherPutDown = putDown.matcher(step.instruction);
                        Matcher matcherScan = scan.matcher(step.instruction);

                        if(matcherPickup.matches()) {
                            commandThread = new Thread(new PickUpRunner(this, matcherPickup.group(1)));
                        }

                        else if(matcherPutDown.matches()) {
                            commandThread = new Thread(new PutDownRunner(this, matcherPutDown.group(1)));
                        }

                        else if(matcherScan.matches()) {
                            commandThread = new Thread(new ReadBarcodeRunner(this));
                        }

                        else {
                            LOGGER.info("I don't know how to parse instruction: " + step.instruction);
                            stepStatus.state = "failed";
                            stepStatus.endedAt = new Date();
                            stepStatus.error = new StepStatus.StepError();
                            stepStatus.error.code = "UNKNOWN_INSTRUCTION";
                            stepStatus.error.message = "unparseable instruction";
                            clearStep();
                        }

                        if(commandThread != null) {
                            ready = false;
                            stepStatus.state = "running";
                            stepStatus.startedAt = new Date();
                            commandThread.start();
                        }

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

    public synchronized boolean startRun(String runId) {
        if(runStateMap.containsKey(runId)) {
            return false;
        }
        if(step != null) {
            throw new IllegalStateException("step " + step.stepId + " of run " + this.runId + " is still " + stepStatus.state + "; wait for it or cancel the run");
        }
        runStateMap.replaceAll((id, state) -> state.equals("running") ? "completed" : state);
        runStateMap.put(runId, "running");
        SimApplication.workbench = new Workbench();
        return true;
    }

    public synchronized boolean setStep(String runId, Step step) {
        if(!runStateMap.containsKey(runId)) {
            throw new NoSuchElementException("unknown run " + runId);
        }
        StepStatus sent = stepStatusMap.get(runId + "." + step.stepId);
        if(sent != null) {
            if(!sent.instruction.equals(step.instruction)) {
                throw new IllegalStateException("step " + step.stepId + " was already sent with another instruction");
            }
            return false;
        }
        if(!runStateMap.get(runId).equals("running")) {
            throw new IllegalStateException("run " + runId + " is " + runStateMap.get(runId) + "; start a new run");
        }
        if(this.step != null) {
            throw new IllegalStateException("step " + this.step.stepId + " is still " + stepStatus.state + "; send the next step after it ends");
        }
        StepStatus stepStatus = new StepStatus();
        stepStatusMap.put(runId + "." + step.stepId, stepStatus);
        stepStatusList.add(stepStatus);
        stepStatus.runId = runId;
        stepStatus.stepId = step.stepId;
        stepStatus.instruction = step.instruction;
        stepStatus.state = "pending";
        this.runId = runId;
        this.step = step;
        this.stepStatus = stepStatus;
        return true;
    }

    public synchronized StepStatus getStepStatus(String runId, String stepId) {
        if(!runStateMap.containsKey(runId)) {
            throw new NoSuchElementException("unknown run " + runId);
        }
        StepStatus stepStatus = stepStatusMap.get(runId + "." + stepId);
        if(stepStatus == null) {
            throw new NoSuchElementException("unknown step " + stepId + " in run " + runId);
        }
        return stepStatus;
    }

    public synchronized String cancelRun(String runId) {
        if(!runStateMap.containsKey(runId)) {
            throw new NoSuchElementException("unknown run " + runId);
        }
        if(runStateMap.get(runId).equals("running")) {
            runStateMap.put(runId, "cancelled");
            if(step != null) {
                LOGGER.info("Cancelling instruction: " + step.instruction);
                stepStatus.state = "cancelled";
                stepStatus.endedAt = new Date();
                stepStatus.detail = "Cancelled. The robot stopped where it was.";
                if(commandThread != null) {
                    commandThread.interrupt();
                }
                clearStep();
            }
        }
        return runStateMap.get(runId);
    }

    // The robot is free for the next step.
    public void clearStep() {
        step = null;
        stepStatus = null;
        commandThread = null;
        ready = true;
    }

}
