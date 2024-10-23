package org.example;

import com.renomad.minum.state.Constants;
import com.renomad.minum.state.Context;
import com.renomad.minum.utils.FileUtils;
import com.renomad.minum.utils.MyThread;
import com.renomad.minum.utils.ThrowingRunnable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.Callable;

import static com.renomad.minum.testing.TestFramework.*;


public class ScheduleTest
{

    private Context context;

    @Before
    public void init() {
        context = buildTestingContext("SchedulerTests");
    }

    @After
    public void cleanup() {
        // delay a sec so our system has time to finish before we start deleting files
        MyThread.sleep(500);
        context.getLogger().stop();
        context.getExecutorService().shutdownNow();
        Constants constants = context.getConstants();
        FileUtils fileUtils = new FileUtils(context.getLogger(), constants);
        fileUtils.deleteDirectoryRecursivelyIfExists(Path.of(constants.dbDirectory).resolve("schedule"));
    }

    @Test
    public void testThatActionShouldNotRunIfBeforeTime() {
        Callable<LocalTime> currentTime = () -> LocalTime.of(12, 44);
        LocalTime timeToRun = LocalTime.of(12, 45);
        ThrowingRunnable action = () -> System.out.println("hello world");
        Scheduler scheduler = new Scheduler(context, action, timeToRun, currentTime);
        // wait two seconds to give time for the system to decide not to take action
        MyThread.sleep(2000);
        assertFalse(scheduler.hasRun());
    }

    @Test
    public void testThatActionShouldRunIfAfterTime() {
        Callable<LocalTime> currentTime = () -> LocalTime.of(12, 46);
        LocalTime timeToRun = LocalTime.of(12, 45);
        ThrowingRunnable action = () -> System.out.println("hello world");
        Scheduler scheduler = new Scheduler(context, action, timeToRun, currentTime);
        // wait two seconds for the action to take place.
        MyThread.sleep(2000);
        assertTrue(scheduler.hasRun());
    }


}
