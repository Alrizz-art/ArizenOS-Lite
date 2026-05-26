package com.arizen.launcher.tools;

/**
 * Base interface for all ArizenOS AI tools
 */
public interface ArizenTool {
    /**
     * Execute the tool with given parameters
     * @param params Tool-specific parameters (parsed from AI response)
     */
    void execute(String params);

    /**
     * Returns a description of this tool for the AI system prompt
     */
    String describe();
}
