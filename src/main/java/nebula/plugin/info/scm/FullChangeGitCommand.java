package nebula.plugin.info.scm;

public abstract class FullChangeGitCommand extends GitReadCommand {

    @Override
    public String obtain() {
        try {
            return executeGitCommand("rev-parse",  "HEAD")
                    .replaceAll("\n", "").trim();
        } catch (Exception e) {
            return null;
        }
    }
}
