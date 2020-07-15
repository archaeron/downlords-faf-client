package com.faforever.client.main.event;

import com.faforever.client.replay.Replay;
import lombok.Getter;
import lombok.Value;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

@Getter
public class LocalReplaysChangedEvent extends ApplicationEvent {

  private final Collection<Replay> newReplays;
  private final Collection<Replay> deletedReplays;
  private final boolean changedInFolder;

  public LocalReplaysChangedEvent(Object source, Collection<Replay> newReplays, Collection<Replay> deletedReplays, boolean changedInFolder) {
    super(source);
    this.newReplays = newReplays;
    this.deletedReplays = deletedReplays;
    this.changedInFolder = changedInFolder;
  }
}

