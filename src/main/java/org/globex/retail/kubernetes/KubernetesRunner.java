package org.globex.retail.kubernetes;

import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class KubernetesRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesRunner.class);

    @Inject
    OpenShiftClient client;

    public int run() {

        String host = System.getenv("HOST");
        if (host == null || host.isBlank()) {
            LOGGER.error("Environment variable 'HOST' for route host not set. Exiting...");
            return -1;
        }

        String namespace = System.getenv("NAMESPACE");
        if (namespace == null || namespace.isBlank()) {
            LOGGER.error("Environment variable 'NAMESPACE' for namespace not set. Exiting...");
            return -1;
        }

        String maxTimeToWaitStr = System.getenv().getOrDefault("MAX_TIME_TO_WAIT_MS", "600000");
        String intervalStr = System.getenv().getOrDefault("INTERVAL", "10000");

        long maxTimeToWait = Long.parseLong(maxTimeToWaitStr);
        long interval = Long.parseLong(intervalStr);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Instant timeLimit = Instant.now().plus( Duration.ofMillis(maxTimeToWait));
        Duration untilNextRun = Duration.ofMillis(interval);
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        CheckForRouteTask task = new CheckForRouteTask(host, namespace, timeLimit, untilNextRun, client, ses, countDownLatch );
        ses.schedule(task,0, TimeUnit.SECONDS );

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {}

        if (Instant.now().isAfter(timeLimit)) {
            return -1;
        } else {
            return 0;
        }
    }

}
