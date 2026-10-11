package fc.sim;

import java.util.HashMap;
import java.util.Map;

public class Workbench {

    public String tubeInGripper = null;

    public String[] redTubeRack = new String[] {
        "26EU-123H0001",
        "26EU-123H0002",
        "26EU-123H0003",
        null,
        null
    };

    public String[] blueTubeRack = new String[] {
        null,
        null,
        null,
        null,
        null
    };

    public Map<String, String[]> tubeRackMap = new HashMap<>();

    {
        tubeRackMap.put("red", redTubeRack);
        tubeRackMap.put("blue", blueTubeRack);
    }

}
