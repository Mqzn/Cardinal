package eg.mqzen.cardinal.api.punishments.templates;

public record TemplateAction(String command, boolean runByConsole) {
    public TemplateAction(String command) {
        this(command, command.startsWith("console:"));
    }
    
    public String getProcessedCommand() {
        return runByConsole ? command.substring("console:".length()) : command;
    }
}