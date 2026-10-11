package fc.sim;

import fc.sim.data.Health;
import fc.sim.data.Step;
import fc.sim.data.StepStatus;
import org.springframework.web.bind.annotation.*;

@RestController
class SimController {

    @RequestMapping(path = "/health", method = RequestMethod.GET)
    public Health health(){
        return new Health();
    }

    @RequestMapping(path = "/readyForCommand", method = RequestMethod.GET)
    public Boolean ready(){
        return SimApplication.robotRunner.isReady();
    }

    @RequestMapping(path = "/run/{runId}/steps", method = RequestMethod.POST)
    public String runStep(@PathVariable String runId, @RequestBody Step step) {
        SimApplication.robotRunner.setStep(runId, step);
        return "ok";
    }

    @RequestMapping(path = "/run/{runId}/steps/{stepId}", method = RequestMethod.GET)
    public StepStatus statusStep(@PathVariable String runId, @PathVariable String stepId) {
        return SimApplication.robotRunner.getStepStatus(runId, stepId);
    }

}
