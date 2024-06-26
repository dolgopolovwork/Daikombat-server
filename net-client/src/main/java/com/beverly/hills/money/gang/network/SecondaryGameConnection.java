package com.beverly.hills.money.gang.network;

import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.proto.MergeConnectionCommand;
import java.io.IOException;

public class SecondaryGameConnection extends AbstractGameConnection {

  protected SecondaryGameConnection(GameServerCreds gameServerCreds) throws IOException {
    super(gameServerCreds);
  }

  public void write(MergeConnectionCommand mergeConnectionCommand) {
    writeLocal(mergeConnectionCommand);
  }

}
