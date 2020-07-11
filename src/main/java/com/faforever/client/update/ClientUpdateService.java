package com.faforever.client.update;


import java.util.concurrent.CompletableFuture;

public interface ClientUpdateService {

  /**
   * Returns information about an available newest update. Future contains {@code null} if no update is available.
   */
  CompletableFuture<UpdateInfo> checkForUpdateInBackground();

  String getCurrentVersion();

  ClientUpdateTask updateInBackground(UpdateInfo updateInfo);
}
