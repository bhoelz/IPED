package iped.engine.lucene;

import iped.engine.CmdLineArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexDeletionPolicy;

import java.io.IOException;
import java.util.List;

@Slf4j
public class CustomIndexDeletionPolicy extends IndexDeletionPolicy {

    private CmdLineArgs cmdArgs;

    public CustomIndexDeletionPolicy(CmdLineArgs args) {
        this.cmdArgs = args;
    }

    @Override
    public void onInit(List<? extends IndexCommit> commits) throws IOException {

        // removes oldest commit
        if (!cmdArgs.isContinue() && !cmdArgs.isRestart() && commits.size() > 1) {
            commits.get(0).delete();
            log.info("Deleting oldest commit");
        }

        // removes last commit
        if (cmdArgs.isRestart() && commits.size() > 1) {
            commits.get(commits.size() - 1).delete();
            log.info("Deleting last commit");
        }

        // removes intermediary commits
        if (commits.size() > 2) {
            for (int i = 1; i < commits.size() - 1; i++) {
                commits.get(i).delete();
            }
        }
    }

    @Override
    public void onCommit(List<? extends IndexCommit> commits) throws IOException {
        // removes intermediary commits
        if (commits.size() > 2) {
            for (int i = 1; i < commits.size() - 1; i++) {
                commits.get(i).delete();
            }
        }
    }

}
