package fc.sim.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// A step's status as ICD v0.3 section 5 defines it.
@JsonPropertyOrder({"step_id", "state", "started_at", "ended_at", "observations", "detail", "error"})
public class StepStatus {

    @JsonIgnore
    public String runId;
    @JsonProperty("step_id")
    public String stepId;
    @JsonIgnore
    public String instruction;
    public String state;
    @JsonProperty("started_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    public Date startedAt;
    @JsonProperty("ended_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    public Date endedAt;
    public Map<String, Object> observations = new HashMap<>();
    public String detail;
    public StepError error;

    public static class RackObservation {
        public String labware;
        public String position;
    }

    @JsonPropertyOrder({"code", "arm_error_code", "message"})
    public static class StepError {
        public String code;
        @JsonProperty("arm_error_code")
        public String armErrorCode;
        public String message;
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    @JsonIgnore
    public String getObservationsJson() {
        if(observations == null || observations.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(observations);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
