package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import io.netty.channel.Channel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.beverly.hills.money.gang.exception.GameErrorCode.CAN_NOT_SHOOT_YOURSELF;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameTest {

    private final Random random = new Random();

    private Game game;

    @BeforeEach
    public void setUp() {
        game = new Game(random.nextInt());
    }

    @AfterEach
    public void tearDown() {
        if (game != null) {
            game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
                assertTrue(playerStateChannel.getPlayerState().getHealth() >= 0, "Health can't be negative");
                assertTrue(playerStateChannel.getPlayerState().getKills() >= 0, "Kill count can't be negative");
            });
            assertTrue(game.playersOnline() >= 0, "Player count can't be negative");
            game.close();
        }
    }


    /**
     * @given a game with no players
     * @when a new player comes in to connect to the game
     * @then the player is connected to the game
     **/
    @Test
    public void testConnectPlayerOnce() throws Throwable {
        assertEquals(0, game.getPlayersRegistry().playersOnline(),
                "No online players as nobody connected yet");
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
        assertEquals(0, game.getBufferedMoves().count(), "Nobody moved");
        assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertFalse(playerState.hasMoved(), "Nobody moved");
        assertEquals(playerName, playerState.getPlayerName());
        assertEquals(0, playerState.getKills(), "Nobody got killed yet");
        assertEquals(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerState.getPlayerId());
        assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
    }

    /**
     * @given a connected player
     * @when the player tries to connect the second time
     * @then game fails to connect because the player has already connected
     */
    @Test
    public void testConnectPlayerTwice() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        // connect the same twice
        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.connectPlayer(playerName, channel),
                "Second try should fail because it's the same player");
        assertEquals(GameErrorCode.PLAYER_EXISTS, gameLogicError.getErrorCode());

        assertEquals(1, game.getPlayersRegistry().playersOnline(), "We connected 1 player only");
        assertEquals(0, game.getBufferedMoves().count(), "Nobody moved");
        assertEquals(1, game.getPlayersRegistry().allPlayers().count(), "We connected 1 player only");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertFalse(playerState.hasMoved(), "Nobody moved");
        assertEquals(playerName, playerState.getPlayerName());
        assertEquals(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerState.getPlayerId());
        assertEquals(100, playerState.getHealth(), "Full 100% HP must be set by default");
    }

    /**
     * @given a game with no players
     * @when when max number of players per game come to connect
     * @then game successfully connects everybody
     */
    @Test
    public void testConnectPlayerMax() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
    }

    /**
     * @given a game with max players per game connected
     * @when one more player comes to connect
     * @then the player is rejected as the game is full
     */
    @Test
    public void testConnectPlayerToMany() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        // connect MAX_PLAYERS_PER_GAME+1 player
        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.connectPlayer(
                        "over the top", channel),
                "We can't connect so many players");
        assertEquals(GameErrorCode.SERVER_FULL, gameLogicError.getErrorCode());

        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
    }

    /**
     * @given a game with no players
     * @when max players per game come to connect concurrently
     * @then the game connects everybody successfully
     */
    @RepeatedTest(32)
    public void testConnectPlayerConcurrency() {
        String playerName = "some player";
        AtomicInteger failures = new AtomicInteger();
        Channel channel = mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    latch.await();
                    game.connectPlayer(playerName + " " + finalI, channel);
                } catch (Exception e) {
                    failures.incrementAndGet();
                    throw new RuntimeException(e);
                }
            }));
        }
        threads.forEach(Thread::start);
        latch.countDown();
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(0, failures.get());
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getPlayersRegistry().playersOnline());
    }

    /**
     * @given a player
     * @when the player shoots and misses
     * @then nobody gets shot
     */
    @Test
    public void testShootMiss() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        PlayerShootingGameState playerShootingGameState = game.shoot(
                playerConnectedGameState.getPlayerStateReader().getCoordinates(),
                playerConnectedGameState.getPlayerStateReader().getPlayerId(), null);
        assertNull(playerShootingGameState.getPlayerShot(), "Nobody is shot");
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody was killed");
        assertEquals(1, game.playersOnline());
    }

    /**
     * @given 2 players
     * @when one player shoots the other
     * @then the shot player gets hit health reduced
     */
    @Test
    public void testShootHit() throws Throwable {
        String shooterPlayerName = "shooter player";
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        assertNotNull(playerShootingGameState.getPlayerShot());

        assertFalse(playerShootingGameState.getPlayerShot().isDead(), "Just one shot. Nobody is dead yet");
        assertEquals(100 - ServerConfig.DEFAULT_DAMAGE, playerShootingGameState.getPlayerShot().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody was killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100 - ServerConfig.DEFAULT_DAMAGE, shotState.getHealth());
        assertFalse(shotState.isDead());
    }

    /**
     * @given 2 players
     * @when one player kills the other
     * @then the shot player dies
     */
    @Test
    public void testShootDead() throws Throwable {
        String shooterPlayerName = "shooter player";
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_DAMAGE);

        // after this loop, one player is almost dead
        for (int i = 0; i < shotsToKill - 1; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        assertNotNull(playerShootingGameState.getPlayerShot());

        assertTrue(playerShootingGameState.getPlayerShot().isDead());
        assertEquals(0, playerShootingGameState.getPlayerShot().getHealth());
        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player was killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());
    }


    /**
     * @given a player
     * @when the player shoots itself
     * @then game rejects the action as you can't shoot yourself
     */
    @Test
    public void testShootYourself() throws Throwable {
        String shooterPlayerName = "shooter player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);

        GameLogicError gameLogicError = assertThrows(GameLogicError.class, () -> game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId()), "You can't shoot yourself");
        assertEquals(gameLogicError.getErrorCode(), CAN_NOT_SHOOT_YOURSELF);

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(
                        shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, game.playersOnline());
        assertEquals(0, shooterState.getKills(), "You can't kill yourself");
    }


    /**
     * @given 2 players, one dead
     * @when the alive player shoots the dead one
     * @then nothing happens
     */
    @Test
    public void testShootHitAlreadyDeadPlayer() throws Throwable {
        String shooterPlayerName = "shooter player";
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        assertNull(playerShootingGameState, "You can't shoot a dead player");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player got killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());
    }

    /**
     * @given a player
     * @when the player shoots a not existing player
     * @then nothing happens
     */
    @Test
    public void testShootHitNotExistingPlayer() throws Throwable {
        String shooterPlayerName = "shooter player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);

        PlayerShootingGameState playerShootingGameState = game.shoot(
                shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                123);
        assertNull(playerShootingGameState, "You can't shoot a non-existing player");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(0, shooterState.getKills(), "Nobody got killed");
        assertEquals(1, game.playersOnline());
    }

    /**
     * @given 2 players, one dead
     * @when the dead player shoots the alive one
     * @then nothing happens. dead players don't shoot
     */
    @Test
    public void testShootShooterIsDead() throws Throwable {
        String shooterPlayerName = "shooter player";
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerShootingGameState playerShootingGameState = game.shoot(
                shotPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId());

        assertNull(playerShootingGameState, "A dead player can't shoot anybody");

        PlayerState shooterState = game.getPlayersRegistry().getPlayerState(shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, shooterState.getHealth(), "Shooter hasn't been hit");
        assertEquals(1, shooterState.getKills(), "One player got killed");
        assertEquals(2, game.playersOnline());
        PlayerState shotState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(0, shotState.getHealth());
        assertTrue(shotState.isDead());
    }

    /**
     * @given many players in the game
     * @when all of them shoot each other once concurrently
     * @then nobody gets killed, everybody's health is reduced
     */
    @RepeatedTest(32)
    public void testShootConcurrency() throws Throwable {

        CountDownLatch latch = new CountDownLatch(1);
        List<PlayerConnectedGameState> connectedPlayers = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            String shotPlayerName = "player " + i;
            Channel channel = mock(Channel.class);
            PlayerConnectedGameState connectedPlayer = game.connectPlayer(shotPlayerName, channel);
            connectedPlayers.add(connectedPlayer);
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    latch.await();
                    PlayerConnectedGameState myTarget = connectedPlayers.get((finalI + 1) % connectedPlayers.size());
                    PlayerConnectedGameState me = connectedPlayers.get(finalI);
                    game.shoot(
                            me.getPlayerStateReader().getCoordinates(),
                            me.getPlayerStateReader().getPlayerId(),
                            myTarget.getPlayerStateReader().getPlayerId());
                } catch (Exception e) {
                    failures.incrementAndGet();
                    throw new RuntimeException(e);
                }
            }));
        }
        threads.forEach(Thread::start);
        latch.countDown();
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(0, failures.get());
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.playersOnline());
        game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
            assertFalse(playerStateChannel.getPlayerState().isDead(), "Nobody is dead");
            assertEquals(0, playerStateChannel.getPlayerState().getKills(), "Nobody got killed");
            assertEquals(100 - ServerConfig.DEFAULT_DAMAGE, playerStateChannel.getPlayerState().getHealth(), "Everybody got hit once");
        });
    }

    /**
     * @given a player
     * @when the player moves
     * @then the game changes player's coordinates and buffers them
     */
    @Test
    public void testMove() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(0, game.getBufferedMoves().count(), "No moves buffered before you actually move");
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerCoordinates);
        assertEquals(1, game.getBufferedMoves().count(), "One move should be buffered");
        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, playerState.getHealth());
        assertEquals(0, playerState.getKills(), "Nobody got killed");
        assertEquals(1, game.playersOnline());
        assertEquals(Vector.builder().x(1f).y(0).build(), playerState.getCoordinates().getDirection());
        assertEquals(Vector.builder().x(0f).y(1).build(), playerState.getCoordinates().getPosition());
    }

    /**
     * @given a player
     * @when the player moves twice
     * @then the game changes player's coordinates to the latest and buffers them
     */
    @Test
    public void testMoveTwice() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState playerConnectedGameState = game.connectPlayer(playerName, channel);
        assertEquals(0, game.getBufferedMoves().count(), "No moves buffered before you actually move");
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerCoordinates);
        PlayerState.PlayerCoordinates playerNewCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(2f).y(1).build())
                .position(Vector.builder().x(1f).y(2).build()).build();
        game.bufferMove(playerConnectedGameState.getPlayerStateReader().getPlayerId(), playerNewCoordinates);
        assertEquals(1, game.getBufferedMoves().count(), "One move should be buffered");

        PlayerState playerState = game.getPlayersRegistry().getPlayerState(playerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));
        assertEquals(100, playerState.getHealth());
        assertEquals(0, playerState.getKills(), "Nobody got killed");
        assertEquals(1, game.playersOnline());
        assertEquals(Vector.builder().x(2f).y(1).build(), playerState.getCoordinates().getDirection());
        assertEquals(Vector.builder().x(1f).y(2).build(), playerState.getCoordinates().getPosition());
    }

    /**
     * @given a game with no players
     * @when a non-existing player moves
     * @then nothing happens
     */
    @Test
    public void testMoveNotExistingPlayer() throws GameLogicError {

        assertEquals(0, game.getBufferedMoves().count(), "No moves buffered before you actually move");
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(123, playerCoordinates);
        assertEquals(0, game.getBufferedMoves().count(),
                "No moves buffered because only existing players can move");

    }

    /**
     * @given a dead player
     * @when the dead player moves
     * @then nothing happens
     */
    @Test
    public void testMoveDead() throws Throwable {
        String shooterPlayerName = "shooter player";
        String shotPlayerName = "shot player";
        Channel channel = mock(Channel.class);
        PlayerConnectedGameState shooterPlayerConnectedGameState = game.connectPlayer(shooterPlayerName, channel);
        PlayerConnectedGameState shotPlayerConnectedGameState = game.connectPlayer(shotPlayerName, channel);

        int shotsToKill = (int) Math.ceil(100d / ServerConfig.DEFAULT_DAMAGE);

        // after this loop, one player is  dead
        for (int i = 0; i < shotsToKill; i++) {
            game.shoot(
                    shooterPlayerConnectedGameState.getPlayerStateReader().getCoordinates(),
                    shooterPlayerConnectedGameState.getPlayerStateReader().getPlayerId(),
                    shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId());
        }
        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                .builder()
                .direction(Vector.builder().x(1f).y(0).build())
                .position(Vector.builder().x(0f).y(1).build()).build();
        game.bufferMove(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId(), playerCoordinates);

        PlayerState deadPlayerState = game.getPlayersRegistry().getPlayerState(shotPlayerConnectedGameState.getPlayerStateReader().getPlayerId())
                .orElseThrow((Supplier<Throwable>) () -> new IllegalStateException("A connected player must have a state!"));

        assertEquals(shotPlayerConnectedGameState.getPlayerStateReader().getCoordinates().getDirection(),
                deadPlayerState.getCoordinates().getDirection(),
                "Direction should be the same as the player has moved only after getting killed");
        assertEquals(shotPlayerConnectedGameState.getPlayerStateReader().getCoordinates().getPosition(),
                deadPlayerState.getCoordinates().getPosition(),
                "Position should be the same as the player has moved only after getting killed");
    }

    /**
     * @given many players connected to the same game
     * @when players move concurrently
     * @then players' coordinates are set to the latest and all moves are buffered
     */
    @Test
    public void testMoveConcurrency() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        List<PlayerConnectedGameState> connectedPlayers = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            String shotPlayerName = "player " + i;
            Channel channel = mock(Channel.class);
            PlayerConnectedGameState connectedPlayer = game.connectPlayer(shotPlayerName, channel);
            connectedPlayers.add(connectedPlayer);
        }

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            int finalI = i;
            threads.add(new Thread(() -> {
                try {
                    latch.await();
                    PlayerConnectedGameState me = connectedPlayers.get(finalI);
                    for (int j = 0; j < 10; j++) {
                        PlayerState.PlayerCoordinates playerCoordinates = PlayerState.PlayerCoordinates
                                .builder()
                                .direction(Vector.builder().x(1f + j).y(0).build())
                                .position(Vector.builder().x(0f).y(1 + j).build()).build();
                        game.bufferMove(me.getPlayerStateReader().getPlayerId(), playerCoordinates);
                    }

                } catch (Exception e) {
                    failures.incrementAndGet();
                    throw new RuntimeException(e);
                }
            }));
        }
        threads.forEach(Thread::start);
        latch.countDown();
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(0, failures.get());
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.playersOnline());

        game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
            assertFalse(playerStateChannel.getPlayerState().isDead(), "Nobody is dead");
            assertEquals(0, playerStateChannel.getPlayerState().getKills(), "Nobody got killed");
            assertEquals(100, playerStateChannel.getPlayerState().getHealth(), "Nobody got shot");
            PlayerState.PlayerCoordinates finalCoordinates = PlayerState.PlayerCoordinates
                    .builder()
                    .direction(Vector.builder().x(10f).y(0).build())
                    .position(Vector.builder().x(0f).y(10f).build()).build();
            assertEquals(finalCoordinates.getPosition(),
                    playerStateChannel.getPlayerState().getCoordinates().getPosition());
            assertEquals(finalCoordinates.getDirection(),
                    playerStateChannel.getPlayerState().getCoordinates().getDirection());
        });
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, game.getBufferedMoves().count(), "All players moved");
    }

    /**
     * @given a game with no players
     * @when the game gets closed
     * @then nothing happens
     */
    @Test
    public void testCloseNobodyConnected() {
        game.close();
    }


    /**
     * @given a game with many players
     * @when the game gets closed
     * @then all players' channels get closed and no player is connected anymore
     */
    @Test
    public void testCloseSomebodyConnected() throws Throwable {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        game.close();
        // all channels should be closed
        verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
        assertEquals(0, game.playersOnline(), "No players online when game is closed");
        assertEquals(0, game.getPlayersRegistry().allPlayers().count(), "No players in the registry when game is closed");
    }

    /**
     * @given a closed game with many players
     * @when the game gets closed again
     * @then nothing happens. the game is still closed.
     */
    @Test
    public void testCloseTwice() throws GameLogicError {
        String playerName = "some player";
        Channel channel = mock(Channel.class);
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            game.connectPlayer(playerName + " " + i, channel);
        }
        game.close(); // close once
        game.close(); // close second time
        // all channels should be closed
        verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
        assertEquals(0, game.playersOnline(), "No players online when game is closed");
        assertEquals(0, game.getPlayersRegistry().allPlayers().count(), "No players in the registry when game is closed");
    }

}
