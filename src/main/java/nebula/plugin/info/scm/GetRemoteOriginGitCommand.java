package nebula.plugin.info.scm;

/**
 * Returns remote origin url
 * ex. git config --get remote.origin.url
 */
public abstract class GetRemoteOriginGitCommand extends GitReadCommand {
    @Override
    public String obtain() {
        try {
            return executeGitCommand("config", "--get", "remote.origin.url")
                    .replaceAll("\n", "").trim();
        } catch (Exception e) {
            return null;
        }
    }
}
