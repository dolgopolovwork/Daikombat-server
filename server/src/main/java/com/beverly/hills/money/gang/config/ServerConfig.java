package com.beverly.hills.money.gang.config;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;
import java.util.Properties;

public interface ServerConfig {

    int PORT = NumberUtils.toInt(System.getenv("GAME_SERVER_PORT"), 7777);
    int GAMES_TO_CREATE = NumberUtils.toInt(System.getenv("GAME_SERVER_GAMES_TO_CREATE"), 10);
    int MAX_PLAYERS_PER_GAME = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_PLAYERS_PER_GAME"), 25);
    int MOVES_UPDATE_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS"), 100);
    int PING_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_PING_FREQUENCY_MLS"), 2_500);
    int IDLE_PLAYERS_KILLER_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS"), 10_000);
    int MAX_IDLE_TIME_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_IDLE_TIME_MLS"), 10_000);

    int DEFAULT_SHOTGUN_DAMAGE = NumberUtils.toInt(System.getenv("GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE"), 20);

    int DEFAULT_PUNCH_DAMAGE = NumberUtils.toInt(System.getenv("GAME_SERVER_DEFAULT_PUNCH_DAMAGE"), 50);

    String PIN_CODE = Optional.of(StringUtils.defaultIfBlank(System.getenv("GAME_SERVER_PIN_CODE"), "5555"))
            .filter(pin -> StringUtils.isNotBlank(pin) && StringUtils.length(pin) >= 4 && pin.matches("\\d+"))
            .orElseThrow(() -> new IllegalArgumentException(
                    "Pin code should: 1) be not empty 2) be at least 4 symbols 3) consist of digits only"));

    String VERSION = Optional.ofNullable(
            ServerConfig.class.getClassLoader().getResourceAsStream("server-version.properties")).map(
            inputStream -> {
                try (inputStream) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    return properties.getProperty("server.version");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).orElseThrow(
            () -> new IllegalStateException("Can't get version"));

}
