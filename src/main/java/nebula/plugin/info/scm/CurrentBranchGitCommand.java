package nebula.plugin.info.scm;

/**
 * Returns current branch name
 * ex.  git rev-parse --abbrev-ref HEAD  -> configuration-cache-support
 */
public abstract class CurrentBranchGitCommand extends GitReadCommand {

    @Override
    public String obtain() {
        try {
            return executeGitCommand("rev-parse", "--abbrev-ref", "HEAD")
                    .replaceAll("\n", "").trim();
        } catch (Exception e) {
            return null;
        }
    }
}
