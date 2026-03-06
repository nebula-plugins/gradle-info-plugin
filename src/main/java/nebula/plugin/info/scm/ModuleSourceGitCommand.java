package nebula.plugin.info.scm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ModuleSourceGitCommand extends GitReadCommand {
   private static final Logger log = LoggerFactory.getLogger(ModuleSourceGitCommand.class);

    @Override
    public String obtain() {
        try {
            return executeGitCommand("rev-parse", "--show-toplevel")
                    .replaceAll("\n", "").trim();
        } catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
    }
}
