package fc.sim.data;

import java.util.Date;
import java.util.Map;

public class StepStatus {

    public String runId;
    public String stepId;
    public String instruction;
    public String state;
    public Date startedAt;
    public Date endedAt;
    public Map<String, Observation> observations;
    public String error;

    public static class Observation { }

    public static class RackObservation extends Observation {
        public String labware;
        public String position;
    }

    public static class BarCodeObservation extends Observation {
        public String barcode;
    }

}
