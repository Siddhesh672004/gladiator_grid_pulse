package com.gridpulse.ai.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.gridpulse.ai.client.CoreClient;
import com.gridpulse.ai.dto.response.ActionResult;
import com.gridpulse.ai.service.PendingActions;
import com.gridpulse.web.security.SuperAdminOnly;

import lombok.RequiredArgsConstructor;

// the super admin's extra tools: the admin accounts. a new admin gets an invite link by email (no password here);
// deleting an admin is left to the Admins page
@Component
@RequiredArgsConstructor
public class SuperAdminTools implements RoleTools, ConfirmableTools {

    private record AdminArgs(Map<String, Object> body) {
    }

    private record SwitchArgs(int userId, boolean on) {
    }

    private final CoreClient core;
    private final PendingActions pendingActions;

    @Override
    public Set<String> roles() {
        return Set.of("SuperAdmin");
    }

    @SuperAdminOnly
    @Tool(description = "All admin accounts with their status.")
    public JsonNode listAdmins() {
        return Data.pickAll(core.admins(), "userId", "username", "email", "mobileNumber", "enabled", "createdAt");
    }

    @SuperAdminOnly
    @Tool(description = "Prepare adding an admin: they get an email with a link to choose their password. Nothing "
            + "changes until the super admin presses Confirm.")
    public String prepareAddAdmin(
            @ToolParam(description = "the admin's name") String username,
            @ToolParam(description = "the admin's email") String email,
            @ToolParam(description = "10 digit mobile number", required = false) String mobileNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("mobileNumber", mobileNumber);
        String summary = "Add " + username + " (" + email + ") as an admin; an invite link to choose a password is "
                + "emailed to them.";
        return pendingActions.prepare("addAdmin", new AdminArgs(body), summary);
    }

    @SuperAdminOnly
    @Tool(description = "Prepare enabling or disabling an admin account. Nothing changes until the super admin "
            + "presses Confirm.")
    public String prepareSetAdminActive(
            @ToolParam(description = "the admin's user id") Integer userId,
            @ToolParam(description = "true = enabled, false = disabled") Boolean enabled) {
        String name = StreamSupport.stream(core.admins().spliterator(), false)
                .filter(admin -> admin.path("userId").asInt() == userId)
                .map(admin -> Data.text(admin, "username") + " (" + Data.text(admin, "email") + ")")
                .findFirst()
                .orElse("#" + userId);
        String summary = (Boolean.TRUE.equals(enabled) ? "Enable" : "Disable (cannot log in)") + " the admin account "
                + name + ".";
        return pendingActions.prepare("setAdminActive", new SwitchArgs(userId, Boolean.TRUE.equals(enabled)), summary);
    }

    @Override
    public Map<String, Function<String, ActionResult>> handlers() {
        return Map.of(
                "addAdmin", json -> {
                    JsonNode admin = core.addAdmin(pendingActions.args(json, AdminArgs.class).body());
                    return ActionResult.done(Data.text(admin, "username") + " is added as an admin; the invite email "
                            + "is on its way.");
                },
                "setAdminActive", json -> {
                    SwitchArgs args = pendingActions.args(json, SwitchArgs.class);
                    core.setAdminStatus(args.userId(), Map.of("enabled", args.on()));
                    return ActionResult.done("The admin account #" + args.userId() + " is now "
                            + (args.on() ? "enabled." : "disabled."));
                });
    }
}
