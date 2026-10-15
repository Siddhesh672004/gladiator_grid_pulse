package com.gridpulse.ai.tools;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

// short json for the model: only the fields a tool names. "session.trackName" reads a nested field and keeps it
// as "trackName"; "session.status:sessionStatus" renames it. a missing or empty field is left out
final class Data {

    private Data() {
    }

    static ObjectNode pick(JsonNode node, String... paths) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        Arrays.stream(paths).forEach(path -> {
            String[] parts = path.split(":");
            JsonNode value = node.at("/" + parts[0].replace('.', '/'));
            String name = parts.length > 1 ? parts[1] : parts[0].substring(parts[0].lastIndexOf('.') + 1);
            if (!value.isMissingNode() && !value.isNull()) {
                out.set(name, value);
            }
        });
        return out;
    }

    static ArrayNode pickAll(JsonNode array, String... paths) {
        ArrayNode out = JsonNodeFactory.instance.arrayNode();
        StreamSupport.stream(array.spliterator(), false)
                .map(item -> pick(item, paths))
                .forEach(out::add);
        return out;
    }

    // a page of the admin lists: its rows + how many there are in all
    static ObjectNode page(JsonNode page, String... paths) {
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.set("rows", pickAll(page.path("content"), paths));
        out.set("totalRows", page.path("totalElements"));
        out.set("page", page.path("page"));
        return out;
    }

    static String text(JsonNode node, String path) {
        return node.at("/" + path.replace('.', '/')).asText("");
    }

    // 650.0 -> "Rs.650"
    static String rupees(double amount) {
        return "Rs." + new DecimalFormat("0.##").format(amount);
    }

    // "Buddh Circuit 2026-10-20 10:00-11:00" from a session json
    static String sessionText(JsonNode session) {
        return text(session, "trackName") + " " + text(session, "sessionDate") + " " + hhmm(text(session, "startTime"))
                + "-" + hhmm(text(session, "endTime"));
    }

    // "10:00:00" -> "10:00"
    static String hhmm(String time) {
        return time.length() >= 5 ? time.substring(0, 5) : time;
    }
}
