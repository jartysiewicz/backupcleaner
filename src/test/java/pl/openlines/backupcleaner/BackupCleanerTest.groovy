package pl.openlines.backupcleaner
import com.google.common.collect.ImmutableList

import java.time.Instant

/**
 * Created by yohan on 30.05.15.
 */
class BackupCleanerTest extends GroovyTestCase {
    void testFindWhatToDelete_empty() {
        // given
        Collection<BackupCleaner.FileInfo> fileInfos = [] as Set

        // when
        def toDelete = new BackupCleaner().findWhatToDelete(fileInfos, 0)

        // then
        assert toDelete.size()==0
    }

    void testFindWhatToDelete_oneFileZeroSpace() {
        // given
        long space = 0;
        String timestamp = "2007-12-03T10:15:30.00Z"

        Collection<BackupCleaner.FileInfo> fileInfos = [new BackupCleaner.FileInfo("test", 10L, Instant.parse(timestamp))] as Set

        // when
        def toDelete = new BackupCleaner().findWhatToDelete(fileInfos, 0)

        // then
        assert toDelete.size()==1
        assert toDelete[1] == "test"
    }
}
