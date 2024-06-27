package com.beverly.hills.money.gang.registry;

import static com.beverly.hills.money.gang.config.ServerConfig.MAX_PLAYERS_PER_GAME;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import io.netty.channel.Channel;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayersRegistry implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(PlayersRegistry.class);

  private final Map<Integer, PlayerStateChannel> players = new ConcurrentHashMap<>();

  public void addPlayer(PlayerState playerState, Channel channel) throws GameLogicError {
    LOG.debug("Add player {}", playerState);
    // not thread-safe
    if (players.size() >= MAX_PLAYERS_PER_GAME) {
      throw new GameLogicError("Can't connect player. Server is full.", GameErrorCode.SERVER_FULL);
    } else if (players.values().stream()
        .anyMatch(playerStateChannel -> playerStateChannel.getPlayerState().getPlayerName()
            .equals(playerState.getPlayerName()))) {
      throw new GameLogicError("Can't connect player. Player name already taken.",
          GameErrorCode.PLAYER_EXISTS);
    }
    // thread-safe
    players.put(playerState.getPlayerId(), PlayerStateChannel.builder()
        .channel(channel).playerState(playerState).build());
  }

  public Optional<PlayerState> getPlayerState(int playerId) {
    return Optional.ofNullable(players.get(playerId))
        .map(playerStateChannel -> playerStateChannel.playerState);
  }

  public Stream<PlayerStateChannel> allPlayers() {
    return players.values().stream();
  }

  public Optional<PlayerStateChannel> findPlayer(int playerId) {
    return Optional.ofNullable(players.get(playerId));
  }

  public int playersOnline() {
    return players.size();
  }

  public Optional<PlayerStateChannel> findPlayer(Channel channel, int playerId) {
    return Optional.ofNullable(players.get(playerId))
        .filter(playerStateChannel -> playerStateChannel.channel == channel);
  }

  public Optional<PlayerState> disconnectPlayer(int playerId) {
    LOG.debug("Disconnect player {}", playerId);
    PlayerStateChannel playerStateChannel = players.remove(playerId);
    if (playerStateChannel != null) {
      playerStateChannel.getChannel().close();
      return Optional.of(playerStateChannel.playerState);
    }
    return Optional.empty();
  }

  public Optional<PlayerStateChannel> removePlayer(int playerId) {
    LOG.debug("Remove player {}", playerId);
    return Optional.ofNullable(players.remove(playerId));
  }

  @Override
  public void close() {
    LOG.info("Close");
    players.values().forEach(playerStateChannel -> playerStateChannel.getChannel().close());
    players.clear();
  }

  @Builder
  @ToString
  public static class PlayerStateChannel {
    // TODO make sure channels are closed properly

    @Getter
    private final Channel channel;
    private int lastPickedSecondaryChannelIdx;
    private final List<Channel> secondaryChannels = new ArrayList<>();

    @Getter
    private final PlayerState playerState;

    public void addSecondaryChannel(Channel channel) {
      secondaryChannels.add(channel);
    }

    public Channel getNextSecondaryChannel() {
      if (secondaryChannels.isEmpty()) {
        // if we have no secondary channel, then we use the main one
        return channel;
      }
      lastPickedSecondaryChannelIdx++;
      return secondaryChannels.get(lastPickedSecondaryChannelIdx % secondaryChannels.size());
    }
  }
}