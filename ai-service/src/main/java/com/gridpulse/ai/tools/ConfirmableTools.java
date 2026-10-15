package com.gridpulse.ai.tools;

import java.util.Map;
import java.util.function.Function;

import com.gridpulse.ai.dto.response.ActionResult;

// a tool class that prepares changes also runs them after Confirm: tool name -> what to do with its saved arguments
public interface ConfirmableTools {

    Map<String, Function<String, ActionResult>> handlers();
}
