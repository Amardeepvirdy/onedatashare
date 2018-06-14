package stork.util;

import java.io.IOException;
import java.util.logging.*;
import static java.util.logging.Level.*;
import java.util.logging.SimpleFormatter;
import java.util.logging.FileHandler;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// A slightly more convenient logging utility.

public abstract class Log {
  private final static Logger log = Logger.getAnonymousLogger();
  public static boolean just_log_to_stdout_who_cares = false;
  // Convenience logging methods that take variadic arguments. The last
  // object can optionally be a throwable to print a stack trace.
  public static void log(Level l, Object... o){
      //Writes log input to file in project path
      LocalDateTime date = LocalDateTime.now();
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      FileHandler fh = null;
      log.setLevel(Level.ALL);
      File f = new File("../onedatashare/stork/core/server/LogFiles/" +  dtf.format(date) + " log.log");
      try {
          fh = new FileHandler("../onedatashare/stork/core/server/LogFiles/" +  dtf.format(date) + " log.log",true);
          if (!f.exists()) {
              f.createNewFile();
          }

      }catch (IOException e) {
          System.out.println("Error reading/writing to log file.  Wrong path?");
      }
      log.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);
      if (just_log_to_stdout_who_cares) {
      System.out.println(StorkUtil.joinWith("", o));
    } else if (log.isLoggable(l)) {
      // Get the caller.
      int i;
      StackTraceElement[] st = Thread.currentThread().getStackTrace();
      for (i = 1; i < st.length; i++) {
        if (!st[i].getClassName().equals(Log.class.getName())) {
            break;
        }
      }

      // Check for a throwable.
      Throwable t = null;
      if (o.length > 0 && o[o.length-1] instanceof Throwable) {
        t = (Throwable) o[o.length-1];
        o[o.length-1] = null;
      }

      LogRecord lr = new LogRecord(l, StorkUtil.joinWith("", o));
      lr.setThrown(t);

      if (i < st.length) {
        lr.setSourceClassName(st[i].getClassName());
        lr.setSourceMethodName(st[i].getMethodName());
      }

      log.log(lr);

    }
  } public static void finest(Object... o) {
    log(FINEST, o);
  } public static void finer(Object... o) {
    log(FINER, o);
  } public static void fine(Object... o) {
    log(FINE, o);
  } public static void config(Object... o) {
    log(CONFIG, o);
  } public static void info(Object... o) {
    log(INFO, o);
  } public static void warning(Object... o) {
    log(WARNING, o);
  } public static void severe(Object... o) {
    log(SEVERE, o);
  }

}
