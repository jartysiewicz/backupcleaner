package pl.openlines.backupcleaner;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.stream.Collectors;

public class BackupCleanerApp {
    private Logger log = LoggerFactory.getLogger(BackupCleanerApp.class);

    private BackupCleanerApp() {
    }

    public void run(Path dir) throws IOException {
        log.info("dir: {}", dir);

        ImmutableSet.Builder<BackupCleaner.FileInfo> fileInfosBuilder = walkFiles(dir);

        long freeSpace = dir.toFile().getFreeSpace();
        log.info("assuming free space on fs: {}", BackupCleaner.humanReadableByteCount(freeSpace));

        Collection<String> whatToDelete = new BackupCleaner().findWhatToDelete(fileInfosBuilder.build(), freeSpace);
        String message = produceMessage(whatToDelete);

        log.info(message);
    }

    private String produceMessage(Collection<String> whatToDelete) {
        String message = "\n\nNothing to delete";
        if (whatToDelete.size()>0) {
            message = whatToDelete.stream().collect(Collectors.joining("\\\n\n", "rm -rf ", "\n\n"));
            log.info(message);
        }
        return message;
    }

    private ImmutableSet.Builder<BackupCleaner.FileInfo> walkFiles(Path dir) throws IOException {
        ImmutableSet.Builder<BackupCleaner.FileInfo> fileInfosBuilder = ImmutableSet.builder();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                BackupCleaner.FileInfo fileInfo = new BackupCleaner.FileInfo(file.getFileName().toString(),
                        attrs.size(), attrs.creationTime().toInstant());
                log.debug("found file: {}", fileInfo);
                fileInfosBuilder.add(fileInfo);
                return FileVisitResult.CONTINUE;
            }
        });
        return fileInfosBuilder;
    }

    public static void main(String[] args) throws IOException {
        String dir = System.getProperty("user.dir");
        if (args.length>0) {
            dir = args[0];
        }
        Path p = Paths.get(dir);
        new BackupCleanerApp().run(p.toAbsolutePath().normalize());
    }
}