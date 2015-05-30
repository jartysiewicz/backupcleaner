package pl.openlines.backupcleaner;

import com.google.common.collect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by yohan on 30.05.15.
 */
public class BackupCleaner {
    private static final float NEED_SPACE_FACTOR = 1.1f;

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
        log.info("finding what to delete from files {}", files);
        ImmutableList.Builder<String> retvalAccumulator = ImmutableList.builder();

        Multimap<LocalDate, FileInfo> sortedByDate = sortByDate(files);
        long needSpaceForNextDay = computeNeedSpace(sortedByDate);
        long missingSpace = needSpaceForNextDay - freeSpace;

        if (missingSpace >0) {
            long stillMissing = acumulateFilesToDelete(retvalAccumulator, sortedByDate, missingSpace);

            if (stillMissing>0) {
                log.error("goal not archived, still missing {}", stillMissing);
            }
        } else {
            log.info("more or equal space ({}) than need ({})", freeSpace, needSpaceForNextDay);
        }

        ImmutableList<String> retval = retvalAccumulator.build();
        log.info("delete proposal: {}", retval);
        return retval;
    }

    private long acumulateFilesToDelete(ImmutableList.Builder<String> retvalBuilder, Multimap<LocalDate, FileInfo> sortedByDate, long missingSpace) {
        long stillMissing = missingSpace;
        log.info("{} bytes missing", missingSpace);

        List<LocalDate> listOfDays = sortedByDate.keySet().stream().sorted().collect(Collectors.toList());

        for (LocalDate day: listOfDays) {
            Collection<FileInfo> filesForDay = sortedByDate.get(day).stream()
                    .sorted((f1, f2) -> Long.compare(f2.getSize(), f1.getSize()))
                    .collect(Collectors.toList());

            log.info("day: {}, files: {}", day, filesForDay);
            for (FileInfo f : filesForDay) {
                retvalBuilder.add(f.getName());
                stillMissing = stillMissing - f.getSize();
                log.info("adding file to delete: {}", f);
                if (stillMissing > 0) {
                    log.info("stillMissing {}", stillMissing);
                } else {
                    log.info("archived goal");
                    break;
                }
            }
            if (stillMissing<=0) {
                break;
            }
        }
        return stillMissing;
    }

    private long computeNeedSpace(Multimap<LocalDate, FileInfo> sortedByDate) {
        long spaceNeed = 0;
        Optional<LocalDate> max = sortedByDate.keys().stream().max(Comparator.naturalOrder());
        if (max.isPresent()) {
            LocalDate lastDay = max.get();
            log.info("Last day {}", lastDay);

            Collection<FileInfo> lastDayFiles = sortedByDate.get(lastDay);
            spaceNeed = (long) (lastDayFiles.stream().mapToLong(f -> f.getSize()).sum() * NEED_SPACE_FACTOR);
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
