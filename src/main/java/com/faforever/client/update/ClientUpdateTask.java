package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.io.FileUtils;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.updater.Updater;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.update4j.service.UpdateHandler;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ClientUpdateTask extends CompletableTask<Void> {

  private final I18n i18n;
  private final PreferencesService preferencesService;

  @Setter
  private UpdateInfo updateInfo;

  @Inject
  public ClientUpdateTask(I18n i18n, PreferencesService preferencesService) {
    super(Priority.MEDIUM);

    this.i18n = i18n;
    this.preferencesService = preferencesService;
  }

  @Override
  protected Void call() throws Exception {
    updateTitle(i18n.get("clientUpdateDownloadTask.title"));

    // update4j will check for an .update subdirectory
    Path updateDirectory = preferencesService.getCacheDirectory().resolve("update");

    updateInfo.getConfiguration().updateTemp(updateDirectory, updateHandler());

    startUpdateFinalizer(updateDirectory);

    return null;
  }

  @SneakyThrows
  private void startUpdateFinalizer(Path updateDirectory) {
    Path jreDir = copyJre(updateDirectory);
    Path updaterJar = copyUpdaterJar();

    String command = String.format("%s -jar %s", jreDir.resolve("bin/javaw").toAbsolutePath(), updaterJar.toAbsolutePath());
    log.info("Starting updater using command: {}", command);
    Runtime.getRuntime().exec(command);
  }

  private Path copyJre(Path updateDirectory) {
    Path jreSourceDir = Paths.get(System.getProperty("java.home"));
    Path jreTargetDir = updateDirectory.resolve("jre");

    log.debug("Copying JRE from {} to {}", jreSourceDir, jreTargetDir);

    FileUtils.copyContentRecursively(jreSourceDir, jreTargetDir);
    return jreTargetDir;
  }

  private Path copyUpdaterJar() throws IOException, URISyntaxException {
    URL updaterJar = Updater.class.getProtectionDomain().getCodeSource().getLocation();
    Path tmpJar = Files.createTempFile("updater", ".jar");
    // Since the updater will delete the updater JAR, it must not be run from it.
    Path targetJar = Paths.get(updaterJar.toURI());

    log.debug("Copying updater JAR from {} to {}", tmpJar, targetJar);
    Files.copy(targetJar, tmpJar);
    return targetJar;
  }

  private UpdateHandler updateHandler() {
    return new UpdateHandler() {
      @Override
      public void updateDownloadProgress(float frac) {
        updateProgress(frac, 1);
      }
    };
  }
}
