package com.beverly.hills.money.gang.scheduler;

import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.config.ServerConfig.*;
import static com.beverly.hills.money.gang.factory.ServerResponseFactory.*;

@Component
@RequiredArgsConstructor
public class GameScheduler implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GameScheduler.class);

    private final GameRoomRegistry gameRoomRegistry;

    private final ScheduledExecutorService bufferedMovesExecutor = Executors.newScheduledThreadPool(1,
            new BasicThreadFactory.Builder().namingPattern("moves-buffer-%d").build());
    private final ScheduledExecutorService idlePlayersKillerExecutor = Executors.newScheduledThreadPool(1,
            new BasicThreadFactory.Builder().namingPattern("idle-players-killer-%d").build());
    private final ScheduledExecutorService pingExecutor = Executors.newScheduledThreadPool(1,
            new BasicThreadFactory.Builder().namingPattern("ping-%d").build());

    public void init() {
        LOG.info("Init scheduler");
        scheduleSendBufferedMoves();
        scheduleIdlePlayerKiller();
        schedulePing();
    }

    private void scheduleSendBufferedMoves() {
        bufferedMovesExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            try {
                if (game.getPlayersRegistry().playersOnline() == 0) {
                    return;
                }
                var bufferedMoves = game.getBufferedMoves();
                if (bufferedMoves.isEmpty()) {
                    return;
                }
                game.getPlayersRegistry().allPlayers().forEach(playerStateChannel -> {
                    // don't send me MY own moves
                    Optional.of(getAllBufferedPlayerMovesExceptMine(
                                    bufferedMoves, playerStateChannel.getPlayerState().getPlayerId()))
                            .filter(playerSpecificBufferedMoves -> !playerSpecificBufferedMoves.isEmpty())
                            .ifPresent(playerSpecificBufferedMoves ->
                                    playerStateChannel.getChannel()
                                            .writeAndFlush(createMovesEventAllPlayers
                                                    (game.getPlayersRegistry().playersOnline(), playerSpecificBufferedMoves)));
                });


            } finally {
                game.flushBufferedMoves();
            }
        }), MOVES_UPDATE_FREQUENCY_MLS, MOVES_UPDATE_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

    private List<PlayerStateReader> getAllBufferedPlayerMovesExceptMine(List<PlayerStateReader> bufferedPlayerMoves, int myPlayerId) {
        return bufferedPlayerMoves.stream()
                .filter(bufferedPlayerMove -> bufferedPlayerMove.getPlayerId() != myPlayerId)
                .collect(Collectors.toList());
    }


    private void schedulePing() {
        pingExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            if (game.getPlayersRegistry().playersOnline() == 0) {
                return;
            }
            LOG.info("Ping");
            ServerResponse ping = createPing(game.playersOnline());
            game.getPlayersRegistry().allPlayers()
                    .filter(playerStateChannel -> playerStateChannel.getPlayerState().isFullyConnected())
                    .map(PlayersRegistry.PlayerStateChannel::getChannel)
                    .forEach(channel -> channel.writeAndFlush(ping));
        }), 0, PING_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

    private void scheduleIdlePlayerKiller() {
        idlePlayersKillerExecutor.scheduleAtFixedRate(() -> gameRoomRegistry.getGames().forEach(game -> {
            if (game.getPlayersRegistry().playersOnline() == 0) {
                return;
            }
            var idlePlayers = game.getPlayersRegistry().allPlayers()
                    .filter(playerStateChannel -> playerStateChannel.getPlayerState().isIdleForTooLong())
                    .collect(Collectors.toList());
            if (idlePlayers.isEmpty()) {
                return;
            }
            LOG.info("Players to disconnect {}", idlePlayers);
            ServerResponse disconnectedEvents = createExitEvent(
                    game.playersOnline(),
                    idlePlayers.stream()
                            .map(PlayersRegistry.PlayerStateChannel::getPlayerState));

            idlePlayers.forEach(playerStateChannel
                    -> game.getPlayersRegistry()
                    .removePlayer(playerStateChannel.getPlayerState().getPlayerId()));
            game.getPlayersRegistry().allPlayers()
                    .forEach(playerStateChannel -> playerStateChannel.getChannel().writeAndFlush(disconnectedEvents));

        }), IDLE_PLAYERS_KILLER_FREQUENCY_MLS, IDLE_PLAYERS_KILLER_FREQUENCY_MLS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        LOG.info("Close");
        try {
            bufferedMovesExecutor.shutdownNow();
        } catch (Exception e) {
            LOG.error("Can't shutdown buffered moves executor", e);
        }
        try {
            idlePlayersKillerExecutor.shutdownNow();
        } catch (Exception e) {
            LOG.error("Can't shutdown player killer executor", e);
        }

        try {
            pingExecutor.shutdownNow();
        } catch (Exception e) {
            LOG.error("Can't shutdown ping executor", e);
        }
    }

}
