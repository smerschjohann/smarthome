package org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.quartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import org.joda.time.base.AbstractInstant;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzScheduler {
    static Logger logger = LoggerFactory.getLogger(QuartzScheduler.class);

    /**
     * Schedules a block of code for later execution.
     *
     * @param instant the point in time when the code should be executed
     * @param closure the code block to execute
     *
     * @return a handle to the created timer, so that it can be canceled or rescheduled
     * @throws ScriptExecutionException if an error occurs during the execution
     */
    public static Timer createTimer(AbstractInstant instant, Runnable closure) {
        JobKey jobKey = new JobKey(instant.toString() + ": " + closure.toString());
        Trigger trigger = newTrigger().startAt(instant.toDate()).build();
        Timer timer = new Timer(jobKey, trigger.getKey(), instant);
        try {
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("procedure", closure);
            dataMap.put("timer", timer);
            JobDetail job = newJob(TimerExecutionJob.class).withIdentity(jobKey).usingJobData(dataMap).build();
            if (Timer.scheduler.checkExists(job.getKey())) {
                Timer.scheduler.deleteJob(job.getKey());
                logger.debug("Deleted existing Job {}", job.getKey().toString());
            }
            Timer.scheduler.scheduleJob(job, trigger);
            logger.debug("Scheduled code for execution at {}", instant.toString());
            return timer;
        } catch (SchedulerException e) {
            logger.error("Failed to schedule code for execution.", e);
            return null;
        }
    }
}
