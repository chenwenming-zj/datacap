package io.edurt.datacap.service.initializer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import io.edurt.datacap.captcha.entity.ResultEntity;
import io.edurt.datacap.service.entity.PipelineEntity;
import io.edurt.datacap.service.loader.CaptchaCacheLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class InitializerConfigure
{
    @Getter
    @Value(value = "${datacap.registration.enable}")
    private Boolean registrationEnable;

    @Getter
    @Value(value = "${datacap.captcha.enable}")
    private Boolean captchaEnable;

    @Getter
    @Value(value = "${datacap.cache.maximum}")
    private Long cacheMaximum;

    @Getter
    @Value(value = "${datacap.cache.expiration}")
    private Long cacheExpiration;

    @Value(value = "${datacap.pipeline.maxRunning}")
    private Integer maxRunning;

    @Value(value = "${datacap.pipeline.maxQueue}")
    private Integer maxQueue;

    @Getter
    private LoadingCache<Long, ResultEntity> cache;

    @Getter
    private BlockingQueue<PipelineEntity> taskQueue;

    @Getter
    private Map<String, ExecutorService> taskExecutors;

    /**
     * Initializes the function.
     */
    @PostConstruct
    public void init()
    {
        if (ObjectUtils.isEmpty(registrationEnable)) {
            this.registrationEnable = Boolean.TRUE;
        }
        log.info("Datacap registration enable: {}", this.registrationEnable);
        if (ObjectUtils.isEmpty(captchaEnable)) {
            captchaEnable = Boolean.TRUE;
        }
        log.info("Datacap captcha enable: {}", this.captchaEnable);

        if (ObjectUtils.isEmpty(cacheMaximum)) {
            cacheMaximum = 10000L;
        }
        log.info("Datacap cache maximum: {}", this.cacheMaximum);
        if (ObjectUtils.isEmpty(cacheExpiration)) {
            cacheExpiration = 2L;
        }
        log.info("Datacap cache expiration: {}", this.cacheExpiration);
        if (ObjectUtils.isEmpty(cache) && captchaEnable) {
            cache = CacheBuilder.newBuilder()
                    .expireAfterWrite(cacheExpiration, TimeUnit.MINUTES)
                    .maximumSize(cacheMaximum)
                    .build(new CaptchaCacheLoader());
        }

        if (ObjectUtils.isEmpty(maxRunning)) {
            maxRunning = 100;
        }
        log.info("Datacap pipeline max running: {}", this.maxRunning);

        if (ObjectUtils.isEmpty(maxQueue)) {
            maxQueue = 200;
        }
        log.info("Datacap pipeline max queue: {}", this.maxQueue);

        this.taskQueue = new LinkedBlockingQueue<>(this.maxQueue);
        this.taskExecutors = Maps.newConcurrentMap();
    }

    /**
     * Check if the task queue is full.
     *
     * @return true if the task queue is full, false otherwise
     */
    public boolean isQueueFull()
    {
        if (this.taskQueue.size() >= this.maxQueue) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the task is ready for submission.
     *
     * @return true if the number of task executors is equal to or greater than the maximum allowed running tasks, false otherwise
     */
    public boolean isSubmit()
    {
        if (this.taskExecutors.size() >= this.maxRunning) {
            return true;
        }
        return false;
    }
}
