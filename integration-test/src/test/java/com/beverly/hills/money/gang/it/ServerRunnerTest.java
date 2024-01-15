package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.runner.ServerRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ServerRunnerTest {

    private final List<ServerRunner> runners = new ArrayList<>();

    private ServerRunner createRunner(int port) {
        var runner = new ServerRunner(port);
        runners.add(runner);
        return runner;
    }

    @AfterEach
    public void tearDown() {
        runners.forEach(ServerRunner::stop);
        runners.clear();
    }


    @Test
    public void testRun() throws InterruptedException {
        int port = AbstractGameServerTest.createRandomPort();
        var runner = createRunner(port);
        AtomicBoolean failed = new AtomicBoolean();
        assertEquals(ServerRunner.State.INIT, runner.getState());
        new Thread(() -> {
            try {
                runner.runServer();
            } catch (Exception e) {
                failed.set(true);
                throw new RuntimeException(e);
            }
        }).start();
        runner.waitFullyRunning();
        assertFalse(AbstractGameServerTest.isPortAvailable(port),
                "Port shouldn't available as game server uses it");
        assertEquals(ServerRunner.State.RUNNING, runner.getState());
        assertFalse(failed.get(), "No failure expected");
    }


    @Test
    public void testRunTwice() throws InterruptedException {
        int port = AbstractGameServerTest.createRandomPort();
        var runner = createRunner(port);
        AtomicBoolean failed = new AtomicBoolean();
        assertEquals(ServerRunner.State.INIT, runner.getState());
        new Thread(() -> {
            try {
                runner.runServer();
            } catch (Exception e) {
                failed.set(true);
                throw new RuntimeException(e);
            }
        }).start();
        runner.waitFullyRunning();
        Exception ex = assertThrows(IllegalStateException.class, runner::runServer,
                "Shouldn't be able to run the same server twice");
        assertEquals("Can't run!", ex.getMessage());
        assertFalse(failed.get(), "No failure expected");
    }

    @Test
    public void testStop() throws InterruptedException {
        int port = AbstractGameServerTest.createRandomPort();
        var runner = createRunner(port);
        CountDownLatch stopLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean();
        assertEquals(ServerRunner.State.INIT, runner.getState());
        new Thread(() -> {
            try {
                runner.runServer();
                stopLatch.countDown();
            } catch (Exception e) {
                failed.set(true);
                throw new RuntimeException(e);
            }
        }).start();
        runner.waitFullyRunning();
        runner.stop();
        assertTrue(stopLatch.await(10, TimeUnit.SECONDS), "Server should stop gracefully");
        assertFalse(failed.get(), "No failure expected");
        assertEquals(ServerRunner.State.STOPPED, runner.getState());
    }

    @Test
    public void testStopTwice() throws InterruptedException {
        int port = AbstractGameServerTest.createRandomPort();
        var runner = createRunner(port);
        CountDownLatch stopLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean();
        assertEquals(ServerRunner.State.INIT, runner.getState());
        new Thread(() -> {
            try {
                runner.runServer();
                stopLatch.countDown();
            } catch (Exception e) {
                failed.set(true);
                throw new RuntimeException(e);
            }
        }).start();
        runner.waitFullyRunning();
        runner.stop();
        runner.stop(); // stop twice
        assertTrue(stopLatch.await(10, TimeUnit.SECONDS), "Server should stop gracefully");
        assertFalse(failed.get(), "No failure expected");
        assertEquals(ServerRunner.State.STOPPED, runner.getState());
    }
}
