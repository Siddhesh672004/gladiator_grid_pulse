package com.gridpulse.ai.copilot.tools;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// the tools of one request come from the role in the token: User -> DriverTools, Admin -> AdminTools,
// SuperAdmin -> AdminTools + SuperAdminTools. a model never sees a tool its user may not use
@Component
@RequiredArgsConstructor
public class Toolsets {

    private final ObjectProvider<RoleTools> tools;

    public Object[] forRole(String role) {
        return toolsFor(role).toArray();
    }

    public List<RoleTools> toolsFor(String role) {
        return tools.orderedStream()
                .filter(set -> set.roles().contains(role))
                .toList();
    }
}
