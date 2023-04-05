package org.globex.retail.kubernetes;

import io.fabric8.openshift.api.model.RouteList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CheckForRouteTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForRouteTask.class);

    private final String host;

    private final String namespace;

    private final Instant timeLimit;

    private final Duration untilNextRun;

    private final OpenShiftClient openShiftClient;

    private final ScheduledExecutorService scheduledExecutorService;

    private final CountDownLatch countDownLatch;

    public CheckForRouteTask(String host, String namespace, Instant timeLimit, Duration untilNextRun, OpenShiftClient openShiftClient,
                             ScheduledExecutorService scheduledExecutorService, CountDownLatch countDownLatch) {
        this.host = host;
        this.namespace = namespace;
        this.timeLimit = timeLimit;
        this.untilNextRun = untilNextRun;
        this.openShiftClient = openShiftClient;
        this.scheduledExecutorService = scheduledExecutorService;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        if (Instant.now().isAfter(timeLimit)) {
            LOGGER.error("Route with host " + host + " not found in namespace " + namespace + " before timeout. Exiting.");
            countDownLatch.countDown();
        }
        RouteList route = openShiftClient.routes().inNamespace(namespace).withField("spec.host", host).list();
        if (route.getItems().isEmpty()) {
            LOGGER.info("Route with host " + host + " not found in namespace " + namespace + ". Rescheduling task.");
            this.scheduledExecutorService.schedule(this, this.untilNextRun.toSeconds(), TimeUnit.SECONDS);
        } else {
            LOGGER.info("Route with host " + host + " found in namespace " + namespace + ".");
            countDownLatch.countDown();
        }
    }
}
