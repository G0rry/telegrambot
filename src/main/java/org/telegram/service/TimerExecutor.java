package org.telegram.service;

import org.telegram.telegrambots.logging.BotLogger;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by pgorun on 27.02.2017.
 */
public class TimerExecutor {
    private static final String LOGTAG = "TIMEREXECUTOR";
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public static class TimerExecutorHolder {
        public static final TimerExecutor HOLDER_INSTANCE = new TimerExecutor();
    }

    public static TimerExecutor getInstance() {
        return TimerExecutorHolder.HOLDER_INSTANCE;
    }

    public void startExecutionEveryHour(CustomTimerTask task){
        BotLogger.warn(LOGTAG, "Run task" + task.getTaskName());
        executorService.scheduleAtFixedRate(() ->{
            task.execute();
        },0,5, TimeUnit.MINUTES);
    }


    public void startExecutionForTime(CustomTimerTask task, int targetHour, int targetMin, int targetSec){
        BotLogger.warn(LOGTAG, "New task" + task.getTaskName());
        final Runnable taskWrapper = () -> {
          try{
              task.execute();
              task.reduceTimes();
              startExecutionForTime(task,targetHour,targetMin,targetSec);
          } catch (Exception e){
              BotLogger.severe(LOGTAG, "Exception at timer executor", e);
          }
        };

        if (task.getTimes() != 0) {
            final long delay = computNextDilay(targetHour, targetMin, targetSec);
            executorService.schedule(taskWrapper, delay, TimeUnit.SECONDS);
        }
    }

    private long computNextDilay(int targetHour, int targetMin, int targetSec) {
        final LocalDateTime localNow = LocalDateTime.now(Clock.system(ZoneId.of("Europe/Moscow")));
        LocalDateTime localNextTarget = localNow.withHour(targetHour).withMinute(targetMin).withSecond(targetSec);
        while (localNow.compareTo(localNextTarget)>0){
            localNextTarget = localNextTarget.plusDays(1);
        }
        localNextTarget = localNow.plusHours(targetHour);

        final Duration duration = Duration.between(localNow,localNextTarget);
        BotLogger.info(LOGTAG, String.valueOf(duration.getSeconds()));
        return duration.getSeconds();
    }

    @Override
    protected void finalize() throws Throwable {
        this.stop();
    }

    private void stop() {
        executorService.shutdown();
        try{
            executorService.awaitTermination(1,TimeUnit.DAYS);
        } catch (InterruptedException e){
            BotLogger.severe(LOGTAG, e);
        } catch (Exception e){
            BotLogger.severe(LOGTAG, e);
        }
    }
}
