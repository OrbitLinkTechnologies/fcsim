package fc.sim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// The ICD v0.3 calls (sections 4 to 6) over HTTP against a running simulator. Every robot step takes 5 s.
class IcdApiTests {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ICD_FIELDS = List.of("step_id", "state", "started_at", "ended_at", "observations", "detail", "error");
    private static final String UTC = "\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\dZ";
    private static final String PICK_UP = "Pick up a tube from the red rack.";
    private static final String SCAN = "Scan the barcode of the tube you are holding.";
    private static final String PLACE = "Place the tube you are holding in an open spot in the blue rack.";

    private static String base;

    static class Reply {
        int code;
        JsonNode body;
    }

    @BeforeAll
    static void startSimulator() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        SimApplication.main(new String[] {"--server.port=" + port});
        base = "http://localhost:" + port;
    }

    static Reply call(String method, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/json")
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        Reply reply = new Reply();
        reply.code = response.statusCode();
        reply.body = response.body().isEmpty() ? null : JSON.readTree(response.body());
        return reply;
    }

    static String newRun() throws Exception {
        String runId = "run-" + UUID.randomUUID();
        Reply reply = call("POST", "/runs", "{\"run_id\": \"" + runId + "\"}");
        assertEquals(201, reply.code);
        assertEquals(runId, reply.body.get("run_id").asText());
        assertEquals("running", reply.body.get("state").asText());
        return runId;
    }

    static Reply sendStep(String runId, String stepId, String instruction) throws Exception {
        return call("POST", "/runs/" + runId + "/steps", "{\"step_id\": \"" + stepId + "\", \"instruction\": \"" + instruction + "\"}");
    }

    static JsonNode waitForState(String runId, String stepId, String... states) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        while (true) {
            Reply reply = call("GET", "/runs/" + runId + "/steps/" + stepId, null);
            assertEquals(200, reply.code);
            if (List.of(states).contains(reply.body.get("state").asText())) {
                return reply.body;
            }
            assertTrue(System.currentTimeMillis() < end, stepId + " never reached " + List.of(states) + ": " + reply.body);
            Thread.sleep(250);
        }
    }

    static JsonNode runStep(String runId, String stepId, String instruction) throws Exception {
        assertEquals(202, sendStep(runId, stepId, instruction).code);
        JsonNode status = waitForState(runId, stepId, "completed", "failed", "cancelled");
        assertIcdShape(status);
        return status;
    }

    static void assertIcdShape(JsonNode status) {
        List<String> fields = new ArrayList<>();
        for (Iterator<String> names = status.fieldNames(); names.hasNext(); ) {
            fields.add(names.next());
        }
        assertEquals(ICD_FIELDS, fields);
        for (String time : List.of("started_at", "ended_at")) {
            assertTrue(status.get(time).isNull() || status.get(time).asText().matches(UTC), time + " is " + status.get(time));
        }
        assertEquals(status.get("state").asText().equals("failed"), !status.get("error").isNull());
    }

    @Test
    void healthSaysUpAndSimulator() throws Exception {
        Reply reply = call("GET", "/health", null);
        assertEquals(200, reply.code);
        assertEquals("up", reply.body.get("status").asText());
        assertTrue(reply.body.get("simulator").asBoolean());
    }

    @Test
    void aTubeMovesFromTheRedRackToTheBlueRack() throws Exception {
        String runId = newRun();

        JsonNode pickUp = runStep(runId, "S-1", PICK_UP);
        assertEquals("completed", pickUp.get("state").asText());
        assertEquals("red rack", pickUp.at("/observations/picked_from/labware").asText());
        assertEquals("A1", pickUp.at("/observations/picked_from/position").asText());
        assertFalse(pickUp.get("detail").isNull());

        JsonNode scan = runStep(runId, "S-2", SCAN);
        assertEquals("completed", scan.get("state").asText());
        assertEquals("26EU-123H0001", scan.at("/observations/barcode").asText());

        JsonNode place = runStep(runId, "S-3", PLACE);
        assertEquals("completed", place.get("state").asText());
        assertEquals("blue rack", place.at("/observations/placed_at/labware").asText());
        assertEquals("A1", place.at("/observations/placed_at/position").asText());
    }

    @Test
    void aPendingStepHasNoTimesOrObservationsYet() throws Exception {
        String runId = newRun();
        Reply reply = sendStep(runId, "S-1", PICK_UP);
        assertEquals(202, reply.code);
        assertIcdShape(reply.body);
        assertTrue(List.of("pending", "running").contains(reply.body.get("state").asText()));
        assertTrue(reply.body.get("ended_at").isNull());
        assertEquals(0, reply.body.get("observations").size());
        waitForState(runId, "S-1", "completed");
    }

    @Test
    void startingARunResetsTheWorkbench() throws Exception {
        String first = newRun();
        assertEquals("A1", runStep(first, "S-1", PICK_UP).at("/observations/picked_from/position").asText());
        String second = newRun();
        assertEquals("A1", runStep(second, "S-1", PICK_UP).at("/observations/picked_from/position").asText());
        assertEquals(200, call("POST", "/runs", "{\"run_id\": \"" + second + "\"}").code);
    }

    @Test
    void theSameStepIdIsNeverRunTwice() throws Exception {
        String runId = newRun();
        assertEquals(202, sendStep(runId, "S-1", PICK_UP).code);
        assertEquals(200, sendStep(runId, "S-1", PICK_UP).code);
        assertEquals(409, sendStep(runId, "S-1", SCAN).code);
        waitForState(runId, "S-1", "completed");
        Reply again = sendStep(runId, "S-1", PICK_UP);
        assertEquals(200, again.code);
        assertEquals("A1", again.body.at("/observations/picked_from/position").asText());
    }

    @Test
    void oneStepAtATime() throws Exception {
        String runId = newRun();
        assertEquals(202, sendStep(runId, "S-1", PICK_UP).code);
        Reply second = sendStep(runId, "S-2", SCAN);
        assertEquals(409, second.code);
        assertTrue(second.body.get("error").asText().contains("S-1"));
        assertEquals(409, call("POST", "/runs", "{\"run_id\": \"run-" + UUID.randomUUID() + "\"}").code);
        waitForState(runId, "S-1", "completed");
    }

    @Test
    void failedStepsCarryAnIcdError() throws Exception {
        String runId = newRun();

        JsonNode unknown = runStep(runId, "S-1", "Pick up the pipette.");
        assertEquals("failed", unknown.get("state").asText());
        assertEquals("UNKNOWN_INSTRUCTION", unknown.at("/error/code").asText());
        assertTrue(unknown.at("/error/arm_error_code").isNull());
        assertFalse(unknown.at("/error/message").asText().isEmpty());

        JsonNode emptyGripper = runStep(runId, "S-2", SCAN);
        assertEquals("GRIPPER_EMPTY", emptyGripper.at("/error/code").asText());

        JsonNode noSuchRack = runStep(runId, "S-3", "Pick up a tube from the green rack.");
        assertEquals("UNKNOWN_LABWARE", noSuchRack.at("/error/code").asText());
    }

    @Test
    void cancelStopsTheRunningStepAndEndsTheRun() throws Exception {
        String runId = newRun();
        assertEquals(202, sendStep(runId, "S-1", PICK_UP).code);
        waitForState(runId, "S-1", "running");
        Reply cancel = call("POST", "/runs/" + runId + "/cancel", null);
        assertEquals(202, cancel.code);
        assertEquals("cancelled", cancel.body.get("state").asText());
        JsonNode status = waitForState(runId, "S-1", "cancelled", "completed", "failed");
        assertEquals("cancelled", status.get("state").asText());
        assertIcdShape(status);
        assertTrue(status.get("ended_at").asText().matches(UTC));
        assertEquals(202, call("POST", "/runs/" + runId + "/cancel", null).code);
        assertEquals(409, sendStep(runId, "S-2", SCAN).code);

        String next = newRun();
        assertEquals("A1", runStep(next, "S-1", PICK_UP).at("/observations/picked_from/position").asText());
    }

    @Test
    void unknownRunsAndStepsAre404() throws Exception {
        assertEquals(404, sendStep("no-such-run", "S-1", PICK_UP).code);
        assertEquals(404, call("GET", "/runs/no-such-run/steps/S-1", null).code);
        assertEquals(404, call("POST", "/runs/no-such-run/cancel", null).code);
        String runId = newRun();
        assertEquals(404, call("GET", "/runs/" + runId + "/steps/no-such-step", null).code);
    }

    @Test
    void malformedRequestsAre400() throws Exception {
        assertEquals(400, call("POST", "/runs", "{not json").code);
        assertEquals(400, call("POST", "/runs", "{}").code);
        String runId = newRun();
        assertEquals(400, call("POST", "/runs/" + runId + "/steps", "{\"step_id\": \"S-1\"}").code);
    }

}
