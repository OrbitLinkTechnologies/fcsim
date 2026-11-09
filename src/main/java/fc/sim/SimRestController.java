package fc.sim;

import fc.sim.data.Health;
import fc.sim.data.Step;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

// The robot API of ICD v0.3, section 4.
@RestController
class SimRestController {

    @RequestMapping(path = "/health", method = RequestMethod.GET)
    public Health health(){
        return new Health();
    }

    @RequestMapping(path = "/readyForCommand", method = RequestMethod.GET)
    public Boolean ready(){
        return SimApplication.robotRunner.isReady();
    }

    @RequestMapping(path = "/runs", method = RequestMethod.POST)
    public ResponseEntity<Object> startRun(@RequestBody Map<String, String> body) {
        String runId = body.get("run_id");
        if(runId == null || runId.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "send {\"run_id\": ...}");
        }
        try {
            boolean started = SimApplication.robotRunner.startRun(runId);
            return ResponseEntity.status(started ? HttpStatus.CREATED : HttpStatus.OK).body(run(runId, SimApplication.robotRunner.runStateMap.get(runId)));
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @RequestMapping(path = "/runs/{runId}/steps", method = RequestMethod.POST)
    public ResponseEntity<Object> runStep(@PathVariable String runId, @RequestBody Step step) {
        if(step.stepId == null || step.stepId.isBlank() || step.instruction == null || step.instruction.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "send {\"step_id\": ..., \"instruction\": ...}");
        }
        try {
            boolean accepted = SimApplication.robotRunner.setStep(runId, step);
            return ResponseEntity.status(accepted ? HttpStatus.ACCEPTED : HttpStatus.OK).body(SimApplication.robotRunner.getStepStatus(runId, step.stepId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @RequestMapping(path = "/runs/{runId}/steps/{stepId}", method = RequestMethod.GET)
    public ResponseEntity<Object> statusStep(@PathVariable String runId, @PathVariable String stepId) {
        try {
            return ResponseEntity.ok(SimApplication.robotRunner.getStepStatus(runId, stepId));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @RequestMapping(path = "/runs/{runId}/cancel", method = RequestMethod.POST)
    public ResponseEntity<Object> cancelRun(@PathVariable String runId) {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(run(runId, SimApplication.robotRunner.cancelRun(runId)));
        } catch (NoSuchElementException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private Map<String, String> run(String runId, String state) {
        Map<String, String> run = new LinkedHashMap<>();
        run.put("run_id", runId);
        run.put("state", state);
        return run;
    }

    private ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

}
