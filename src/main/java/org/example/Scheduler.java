package org.example;

import com.renomad.minum.database.Db;
import com.renomad.minum.database.DbData;
import com.renomad.minum.state.Constants;
import com.renomad.minum.state.Context;
import com.renomad.minum.logging.ILogger;
import com.renomad.minum.logging.LoggingLevel;
import com.renomad.minum.utils.MyThread;
import com.renomad.minum.utils.ThrowingRunnable;
import com.renomad.minum.utils.TimeUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * This class starts an infinite loop when the application begins,
 * waiting until a time specified, to carry out a specified action
 */
public class Scheduler {

    private final ExecutorService es;
    private final ILogger logger;
    private final Constants constants;
    private final ThrowingRunnable runnable;
    private final LocalTime time;
    /**
     * This is so we can control the current time, for testing
     */
    private final Callable<LocalTime> getNow;
    private final Db<Schedule> schedule;
    private Thread myThread;

    public Scheduler(Context context, ThrowingRunnable runnable, LocalTime time, Callable<LocalTime> getNow) {
        this.es = context.getExecutorService();
        this.logger = context.getLogger();
        this.constants = context.getConstants();
        schedule = context.getDb("schedule", Schedule.EMPTY);
        this.runnable = runnable;
        this.time = time;
        this.getNow = getNow;
        initialize();
    }

    public Scheduler(Context context, ThrowingRunnable runnable, LocalTime time) {
        this(context, runnable, time, LocalTime::now);
    }

    @SuppressWarnings({"BusyWait"})
    private void initialize() {

        logger.logDebug(() -> "Initializing Scheduler main loop");
        Callable<Object> innerLoopThread = () -> {
            Thread.currentThread().setName("Scheduler");
            myThread = Thread.currentThread();
            while (true) {
                try {
                    actIfTime();
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    if (constants.logLevels.contains(LoggingLevel.DEBUG)) System.out.printf(TimeUtils.getTimestampIsoInstant() + " Scheduler is stopped.%n");
                    Thread.currentThread().interrupt();
                    return null;
                } catch (Exception ex) {
                    System.out.printf(TimeUtils.getTimestampIsoInstant() + " ERROR: Scheduler has stopped unexpectedly. error: %s%n", ex);
                    throw ex;
                }
            }
        };
        es.submit(innerLoopThread);
    }

    /**
     * If the process determines it is time to act, the user-specified command will be run.
     */
    private void actIfTime() throws Exception {
        LocalTime now = getNow.call();

        // delete any future schedules from the database. Nothing should stay in the database
        // during the day until *after* we hit the time.
        schedule.values()
                .stream()
                .filter(x -> x.getTime().isAfter(now))
                .forEach(schedule::delete);


        // if after the time, check if there is an existing schedule for today, meaning it has been done.
        // if not, do it immediately.
        if (now.isAfter(time) && schedule.values().stream().noneMatch(x -> x.getTime().isBefore(now))) {
            try {
                runnable.run();
                schedule.write(new Schedule(0, time));
            } catch (Exception e) {
                logger.logAsyncError(() -> "Error occurred during run of action in scheduler: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Kills the infinite loop running inside this class.
     */
    public void stop() {
        logger.logDebug(() -> "Scheduler has been told to stop");
        for (int i = 0; i < 10; i++) {
            if (myThread != null) {
                logger.logDebug(() -> "Scheduler: Sending interrupt to thread");
                myThread.interrupt();
                return;
            } else {
                MyThread.sleep(20);
            }
        }
        throw new RuntimeException("Scheduler: Leaving without successfully stopping thread");
    }

    public boolean hasRun() {
        // if the schedule database isn't empty, then there was an action taken at some
        // time during the day.
        return !schedule.values().isEmpty();
    }
}
