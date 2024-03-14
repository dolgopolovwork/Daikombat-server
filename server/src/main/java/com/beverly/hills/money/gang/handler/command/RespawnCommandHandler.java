package com.beverly.hills.money.gang.handler.command;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerRespawnedGameState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createRespawnEventSinglePlayer;
import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.createSpawnEventSinglePlayerMinimal;

@Component
@RequiredArgsConstructor
public class RespawnCommandHandler extends ServerCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RespawnCommandHandler.class);

    private final GameRoomRegistry gameRoomRegistry;

    @Override
    protected boolean isValidCommand(ServerCommand msg, Channel currentChannel) {
        var respawnCommand = msg.getRespawnCommand();
        return respawnCommand.hasGameId() && respawnCommand.hasPlayerId();
    }

    @Override
    protected void handleInternal(ServerCommand msg, Channel currentChannel) throws GameLogicError {
        var respawnCommand = msg.getRespawnCommand();
        Game game = gameRoomRegistry.getGame(respawnCommand.getGameId());

        PlayerRespawnedGameState playerRespawnedGameState = game.respawnPlayer(respawnCommand.getPlayerId());
        ServerResponse playerSpawnEvent = createRespawnEventSinglePlayer(
                game.playersOnline(), playerRespawnedGameState);
        LOG.info("Send my spawn to myself");

        currentChannel.writeAndFlush(playerSpawnEvent)
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        LOG.error("Failed to respawn player", channelFuture.cause());
                        game.getPlayersRegistry().disconnectPlayer(playerRespawnedGameState.getPlayerState().getPlayerId());
                        return;
                    }
                    var otherPlayers = game.getPlayersRegistry()
                            .allPlayers()
                            .filter(playerStateChannel -> playerStateChannel.getChannel() != currentChannel)
                            .collect(Collectors.toList());
                    if (otherPlayers.isEmpty()) {
                        LOG.info("No other players");
                        return;
                    }
                    LOG.info("Send new player spawn to everyone");
                    ServerResponse playerSpawnEventForOthers = createSpawnEventSinglePlayerMinimal(game.playersOnline(),
                            playerRespawnedGameState.getPlayerState());
                    otherPlayers.forEach(playerStateChannel -> playerStateChannel.getChannel()
                            .writeAndFlush(playerSpawnEventForOthers));
                });

    }
}
