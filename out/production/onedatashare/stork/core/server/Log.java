package stork.core.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {
    Logger logger;
    FileHandler fh;

    public Log(String filename) throws IOException {
        File f = new File(filename);
        if (!f.exists()) {
            f.createNewFile();
        }

        this.fh = new FileHandler(filename);
        this.logger = Logger.getLogger(Log.class.getName());
        this.logger.addHandler(this.fh);
        SimpleFormatter formatter = new SimpleFormatter();
        this.fh.setFormatter(formatter);
    }
}
