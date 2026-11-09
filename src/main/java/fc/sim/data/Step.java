package fc.sim.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Step {
    @JsonProperty("step_id")
    public String stepId;
    public String instruction;
}
