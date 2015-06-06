package pl.openlines.backupcleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupCleanerApp {
    private Logger log = LoggerFactory.getLogger(BackupCleanerApp.class);

    private BackupCleanerApp() {
    }

    public void run() {
        log.info("hello world");
    }

    public static void main(String[] args) {
        new BackupCleanerApp().run();
    }
}
