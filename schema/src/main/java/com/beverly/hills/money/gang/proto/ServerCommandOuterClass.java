// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: schema/src/main/resources/server-command.proto

// Protobuf Java Version: 3.25.0
package com.beverly.hills.money.gang.proto;

public final class ServerCommandOuterClass {
  private ServerCommandOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_ServerCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_ServerCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_GetServerInfoCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_GetServerInfoCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_PingCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_PingCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_PushChatEventCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_PushChatEventCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_JoinGameCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_JoinGameCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_RespawnCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_RespawnCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_PushGameEventCommand_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_PushGameEventCommand_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_daikombat_dto_PushGameEventCommand_Vector_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_daikombat_dto_PushGameEventCommand_Vector_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n.schema/src/main/resources/server-comma" +
      "nd.proto\022\rdaikombat.dto\"\232\003\n\rServerComman" +
      "d\022\021\n\004hmac\030\002 \001(\014H\001\210\001\001\022:\n\013chatCommand\030\003 \001(" +
      "\0132#.daikombat.dto.PushChatEventCommandH\000" +
      "\022:\n\013gameCommand\030\004 \001(\0132#.daikombat.dto.Pu" +
      "shGameEventCommandH\000\0229\n\017joinGameCommand\030" +
      "\005 \001(\0132\036.daikombat.dto.JoinGameCommandH\000\022" +
      "C\n\024getServerInfoCommand\030\006 \001(\0132#.daikomba" +
      "t.dto.GetServerInfoCommandH\000\0221\n\013pingComm" +
      "and\030\007 \001(\0132\032.daikombat.dto.PingCommandH\000\022" +
      "7\n\016respawnCommand\030\010 \001(\0132\035.daikombat.dto." +
      "RespawnCommandH\000B\t\n\007commandB\007\n\005_hmac\"\026\n\024" +
      "GetServerInfoCommand\"\r\n\013PingCommand\"|\n\024P" +
      "ushChatEventCommand\022\023\n\006gameId\030\001 \001(\005H\000\210\001\001" +
      "\022\024\n\007message\030\002 \001(\tH\001\210\001\001\022\025\n\010playerId\030\003 \001(\005" +
      "H\002\210\001\001B\t\n\007_gameIdB\n\n\010_messageB\013\n\t_playerI" +
      "d\"\272\001\n\017JoinGameCommand\022\023\n\006gameId\030\001 \001(\005H\000\210" +
      "\001\001\022\024\n\007version\030\002 \001(\tH\001\210\001\001\022\027\n\nplayerName\030\003" +
      " \001(\tH\002\210\001\001\0224\n\004skin\030\004 \001(\0162!.daikombat.dto." +
      "SkinColorSelectionH\003\210\001\001B\t\n\007_gameIdB\n\n\010_v" +
      "ersionB\r\n\013_playerNameB\007\n\005_skin\"T\n\016Respaw" +
      "nCommand\022\023\n\006gameId\030\001 \001(\005H\000\210\001\001\022\025\n\010playerI" +
      "d\030\002 \001(\005H\001\210\001\001B\t\n\007_gameIdB\013\n\t_playerId\"\212\004\n" +
      "\024PushGameEventCommand\022\023\n\006gameId\030\001 \001(\005H\000\210" +
      "\001\001\022I\n\teventType\030\002 \001(\01621.daikombat.dto.Pu" +
      "shGameEventCommand.GameEventTypeH\001\210\001\001\022A\n" +
      "\010position\030\003 \001(\0132*.daikombat.dto.PushGame" +
      "EventCommand.VectorH\002\210\001\001\022B\n\tdirection\030\004 " +
      "\001(\0132*.daikombat.dto.PushGameEventCommand" +
      ".VectorH\003\210\001\001\022\025\n\010playerId\030\005 \001(\005H\004\210\001\001\022\035\n\020a" +
      "ffectedPlayerId\030\006 \001(\005H\005\210\001\001\0324\n\006Vector\022\016\n\001" +
      "x\030\001 \001(\002H\000\210\001\001\022\016\n\001y\030\002 \001(\002H\001\210\001\001B\004\n\002_xB\004\n\002_y" +
      "\"I\n\rGameEventType\022\010\n\004MOVE\020\000\022\t\n\005SHOOT\020\001\022\t" +
      "\n\005PUNCH\020\002\022\030\n\024QUAD_DAMAGE_POWER_UP\020\003B\t\n\007_" +
      "gameIdB\014\n\n_eventTypeB\013\n\t_positionB\014\n\n_di" +
      "rectionB\013\n\t_playerIdB\023\n\021_affectedPlayerI" +
      "d*W\n\022SkinColorSelection\022\t\n\005GREEN\020\000\022\010\n\004PI" +
      "NK\020\001\022\n\n\006PURPLE\020\002\022\010\n\004BLUE\020\003\022\n\n\006YELLOW\020\004\022\n" +
      "\n\006ORANGE\020\005B&\n\"com.beverly.hills.money.ga" +
      "ng.protoP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_daikombat_dto_ServerCommand_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_daikombat_dto_ServerCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_ServerCommand_descriptor,
        new java.lang.String[] { "Hmac", "ChatCommand", "GameCommand", "JoinGameCommand", "GetServerInfoCommand", "PingCommand", "RespawnCommand", "Command", });
    internal_static_daikombat_dto_GetServerInfoCommand_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_daikombat_dto_GetServerInfoCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_GetServerInfoCommand_descriptor,
        new java.lang.String[] { });
    internal_static_daikombat_dto_PingCommand_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_daikombat_dto_PingCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_PingCommand_descriptor,
        new java.lang.String[] { });
    internal_static_daikombat_dto_PushChatEventCommand_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_daikombat_dto_PushChatEventCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_PushChatEventCommand_descriptor,
        new java.lang.String[] { "GameId", "Message", "PlayerId", });
    internal_static_daikombat_dto_JoinGameCommand_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_daikombat_dto_JoinGameCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_JoinGameCommand_descriptor,
        new java.lang.String[] { "GameId", "Version", "PlayerName", "Skin", });
    internal_static_daikombat_dto_RespawnCommand_descriptor =
      getDescriptor().getMessageTypes().get(5);
    internal_static_daikombat_dto_RespawnCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_RespawnCommand_descriptor,
        new java.lang.String[] { "GameId", "PlayerId", });
    internal_static_daikombat_dto_PushGameEventCommand_descriptor =
      getDescriptor().getMessageTypes().get(6);
    internal_static_daikombat_dto_PushGameEventCommand_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_PushGameEventCommand_descriptor,
        new java.lang.String[] { "GameId", "EventType", "Position", "Direction", "PlayerId", "AffectedPlayerId", });
    internal_static_daikombat_dto_PushGameEventCommand_Vector_descriptor =
      internal_static_daikombat_dto_PushGameEventCommand_descriptor.getNestedTypes().get(0);
    internal_static_daikombat_dto_PushGameEventCommand_Vector_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_daikombat_dto_PushGameEventCommand_Vector_descriptor,
        new java.lang.String[] { "X", "Y", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
