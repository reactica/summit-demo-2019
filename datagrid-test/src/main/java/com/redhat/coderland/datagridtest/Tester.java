package com.redhat.coderland.datagridtest;


import com.redhat.coderland.reactica.model.User;
import io.quarkus.scheduler.Scheduled;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Tester {
    Logger LOGGER = LoggerFactory.getLogger("DG TESTER");

    AtomicInteger rollerCoasterSize = new AtomicInteger(0);
    AtomicInteger screamerSize = new AtomicInteger(0);


    @Inject
    RemoteCache<String, User> cache;

    @Scheduled(every = "5s",delay = 2, delayUnit = TimeUnit.SECONDS)
    public void retriveAndCount() {
        setRollerCoasterSize(0);
        setScreamerSize(0);

        cache.entrySet().forEach(e -> {
            User user = e.getValue();
            if("roller-coaster".equals(user.getRideId()))
                incrementRollerCoaster();
            else
                incrementScreamer();

        });
        LOGGER.info("Cache status: Total Size {}, Roller Coaster {}, Screamer {}",getRollerCoasterSize()+getScreamerSize(),getRollerCoasterSize(),getScreamerSize());
    }

    public int getRollerCoasterSize() {
        return rollerCoasterSize.get();
    }

    public void setRollerCoasterSize(int rollerCoasterSize) {
        this.rollerCoasterSize.set(rollerCoasterSize);
    }

    public int getScreamerSize() {
        return screamerSize.get();
    }

    public void setScreamerSize(int screamerSize) {
        this.screamerSize.set(screamerSize);
    }

    public void incrementRollerCoaster() {
        rollerCoasterSize.incrementAndGet();
    }

    public void incrementScreamer() {
        screamerSize.incrementAndGet();
    }
}
