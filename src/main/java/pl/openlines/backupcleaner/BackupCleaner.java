package pl.openlines.backupcleaner;

import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by yohan on 30.05.15.
 */
public class BackupCleaner {
    private static final float NEED_SPACE_FACTOR = 2.0f;

    private final Logger log = LoggerFactory.getLogger(BackupCleaner.class);

    public static class FileInfo {
        private String name;
        private long size;
        private Instant createTimestamp;

        public FileInfo(String name, long size, Instant createTimestmp) {
            this.name = name;
            this.size = size;
            this.createTimestamp = createTimestmp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileInfo fileInfo = (FileInfo) o;

            return name.equals(fileInfo.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        public Instant getCreateTimestamp() {
            return createTimestamp;
        }

        @Override
        public String toString() {
            return name + " " + size + " (" + createTimestamp + ")";
        }
    }
    public Collection<String> findWhatToDelete(Set<FileInfo> files, long freeSpace) {
        ImmutableList.Builder<String> retvalBuilder = ImmutableList.builder();

        Multimap<LocalDate, FileInfo> sortedByDate = sortByDate(files);
        long needSpace = computeNeedSpace(sortedByDate);
        if (needSpace>0) {
            sortedByDate.keySet().stream().sorted().forEach(d-> {
                Collection<FileInfo> filesForDay = sortedByDate.get(d);

                log.info("day: {}, files: {}", d, filesForDay);

            });
        }

        return retvalBuilder.build();
    }

    private long computeNeedSpace(Multimap<LocalDate, FileInfo> sortedByDate) {
        long spaceNeed = 0;
        Optional<LocalDate> max = sortedByDate.keys().stream().max(Comparator.naturalOrder());
        if (max.isPresent()) {
            LocalDate lastDay = max.get();
            log.info("Last day {}", lastDay);

            Collection<FileInfo> lastDayFiles = sortedByDate.get(lastDay);
            spaceNeed = (long) (lastDayFiles.stream().mapToLong(f -> f.getSize()).sum() * NEED_SPACE_FACTOR);
            log.info("Need space {}", spaceNeed);
        }
        return spaceNeed;
    }

    private Multimap<LocalDate, FileInfo> sortByDate(Set<FileInfo> files) {
        Multimap<LocalDate, FileInfo> sortedByDate = ArrayListMultimap.create();
        files.stream().forEach(f-> {
            LocalDate fileDate = f.getCreateTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
            sortedByDate.put(fileDate, f);
        });
        return sortedByDate;
    }

}
