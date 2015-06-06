package pl.openlines.backupcleaner
import com.google.common.collect.ImmutableList

import java.time.Instant
import java.time.format.DateTimeFormatter

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

    void testFindWhatToDelete_oneFileMoreSpaceThanNeed() {
        // given
        long size = 10L;
        long space = 2*size + 1;

        String timestamp = "2007-12-03T10:15:30.00Z"

        Collection<BackupCleaner.FileInfo> fileInfos = [new BackupCleaner.FileInfo("test", size, Instant.parse(timestamp))] as Set

        // when
        def toDelete = new BackupCleaner().findWhatToDelete(fileInfos, space)

        // then
        assert toDelete.size()==0
    }

    void testFindWhatToDelete_oneFileZeroSpace() {
        // given
        long size = 10L;
        long space = 0;

        String timestamp = "2007-12-03T10:15:30.00Z"

        Collection<BackupCleaner.FileInfo> fileInfos = [new BackupCleaner.FileInfo("test", size, Instant.parse(timestamp))] as Set

        // when
        def toDelete = new BackupCleaner().findWhatToDelete(fileInfos, space)

        // then
        assert toDelete.size()==1
    }

    void testFindWhatToDelete_takeBigFilesFirst() {
        // given
        long size = 10L;
        long space = 0;

        String timestamp1 = "2007-12-03T10:15:30.00Z"
        String timestamp2 = "2007-12-03T20:15:30.00Z"
        String timestamp3 = "2007-12-04T20:15:30.00Z"

        BackupCleaner.FileInfo file1 = new BackupCleaner.FileInfo("test1", size, Instant.parse(timestamp1));
        BackupCleaner.FileInfo file2 = new BackupCleaner.FileInfo("test2", 2 * size, Instant.parse(timestamp2));
        BackupCleaner.FileInfo file3 = new BackupCleaner.FileInfo("test3", size, Instant.parse(timestamp3));

        Collection<BackupCleaner.FileInfo> fileInfos = [file1, file2, file3] as Set

        // when
        def toDelete = new BackupCleaner().findWhatToDelete(fileInfos, space)

        // then
        assert toDelete.size()==1
        assert toDelete[0] == "test2"
    }

    void testFindWhatToDelete_realData() {
        // given
        File testDataFile = new File("src/test/resources/realdata.txt");
        Collection<BackupCleaner.FileInfo> fileInfos = testDataFile.collect { l ->
            def parts = l.split(" +");
            Long size = Long.parseLong(parts[4])
            String tz = parts[7][0..2] + ":" + parts[7][3..4]
            Instant date = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(parts[5]+"T"+parts[6]+tz))

            new BackupCleaner.FileInfo(parts[8], size, date);
        } as Set
        long space = 65 * 1024 * 1024;

        // when
        def toDelete = new BackupCleaner().findWhatToDelete(fileInfos, space)

        // then
        assert toDelete.size()==4
        assert toDelete == ["dms_iq.15_05_30.db.gz", "test", "dms_iq.15_05_30.tar.gz", "dms_iq.15_05_31.db.gz"]
    }
}
