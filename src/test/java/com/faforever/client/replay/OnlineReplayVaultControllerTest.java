package com.faforever.client.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.query.LogicalNodeController;
import com.faforever.client.query.SpecificationController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OnlineReplayVaultControllerTest extends AbstractPlainJavaFxTest {
  private static final int MAX_RESULTS = 100;

  private OnlineReplayVaultController instance;

  @Mock
  private ReplayService replayService;
  @Mock
  private UiService uiService;
  @Mock
  private ReplayDetailController replayDetailController;
  @Mock
  private SpecificationController specificationController;
  @Mock
  private LogicalNodeController logicalNodeController;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private SearchController searchController;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ReportingService reportingService;

  @Captor
  private ArgumentCaptor<Consumer<SearchConfig>> searchListenerCaptor;
  private SortConfig sortOrder;
  private SearchConfig standardSearchConfig;
  private Replay testReplay = new Replay();

  @Before
  public void setUp() throws Exception {
    when(replayDetailController.getRoot()).thenReturn(new Pane());

    when(uiService.loadFxml("theme/vault/replay/replay_card.fxml")).thenAnswer(invocation -> {
      ReplayCardController controller = mock(ReplayCardController.class);
      when(controller.getRoot()).thenReturn(new Pane());
      return controller;
    });
    when(uiService.loadFxml("theme/vault/replay/replay_detail.fxml")).thenAnswer(invocation -> replayDetailController);

    when(replayService.getNewestReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), Collections.emptyMap())));
    when(replayService.getHighestRatedReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(Collections.emptyList(), Collections.emptyMap())));
    when(replayService.findById(anyInt())).thenReturn(CompletableFuture.completedFuture(Optional.of(testReplay)));
    when(preferencesService.getPreferences()).thenReturn(new Preferences());
    sortOrder = preferencesService.getPreferences().getVaultPrefs().getOnlineReplaySortConfig();
    standardSearchConfig = new SearchConfig(sortOrder, "query");

    instance = new OnlineReplayVaultController(replayService, uiService, notificationService, i18n, preferencesService, reportingService);

    loadFxml("theme/vault/replay/online_replays.fxml", clazz -> {
      if (SearchController.class.isAssignableFrom(clazz)) {
        return searchController;
      }
      if (SpecificationController.class.isAssignableFrom(clazz)) {
        return specificationController;
      }
      if (LogicalNodeController.class.isAssignableFrom(clazz)) {
        return logicalNodeController;
      }
      return instance;
    });

    verify(searchController).setSearchListener(searchListenerCaptor.capture());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayVaultRoot));
    assertThat(instance.getRoot().getParent(), is(instanceOf(BorderPane.class)));
    assertThat(instance.getRoot().getParent().getParent(), is(nullValue()));
  }

  @Test
  public void testOnDisplayPopulatesReplays() {
    List<Replay> replays = Arrays.asList(new Replay(), new Replay());
    HashMap<String, Integer> innerHashMap = new HashMap<>();
    innerHashMap.put("totalPages", 1);
    HashMap<String, HashMap<String, Integer>> hashMap = new HashMap<>();
    hashMap.put("page", innerHashMap);

    when(replayService.getNewestReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(replays, hashMap)));
    when(replayService.getHighestRatedReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(replays, hashMap)));
    when(replayService.getOwnReplays(anyInt(), anyInt())).thenReturn(CompletableFuture.completedFuture(new Tuple<>(replays, hashMap)));

    instance.display(new OpenOnlineReplayVaultEvent());

    verify(replayService).getNewestReplays(anyInt(), eq(1));
    verify(replayService).getHighestRatedReplays(anyInt(), eq(1));
    assertThat(instance.pagination.isVisible(), is(false));
  }

  @Test
  public void testOnSearchButtonClicked() {
    Consumer<SearchConfig> searchListener = searchListenerCaptor.getValue();
    HashMap<String, Integer> lowerHashMap = new HashMap<>();
    lowerHashMap.put("totalPages", 1);
    HashMap<String, HashMap<String, Integer>> upperHashMap = new HashMap<>();
    upperHashMap.put("page", lowerHashMap);

    when(replayService.findByQuery("query", MAX_RESULTS, 1, sortOrder))
        .thenReturn(CompletableFuture.completedFuture(new Tuple<>(Arrays.asList(new Replay(), new Replay()), upperHashMap)));

    searchListener.accept(standardSearchConfig);

    assertThat(instance.showroomGroup.isVisible(), is(false));
    assertThat(instance.searchResultGroup.isVisible(), is(true));
    verify(replayService).findByQuery("query", MAX_RESULTS, 1, sortOrder);
  }

  @Test
  public void testShowReplayEventWhenUninitialized() {
    instance.display(new ShowReplayEvent(123));
    WaitForAsyncUtils.waitForFxEvents();
    verify(replayDetailController).setReplay(testReplay);
  }

  @Test
  public void testShowReplayEventWhenInitialized() {
    instance.display(new OpenOnlineReplayVaultEvent());
    instance.display(new ShowReplayEvent(123));
    WaitForAsyncUtils.waitForFxEvents();
    verify(replayDetailController).setReplay(testReplay);
  }

  @Test
  public void testOnSearchButtonClickedHandlesException() {
    Consumer<SearchConfig> searchListener = searchListenerCaptor.getValue();
    CompletableFuture<Tuple<List<Replay>, Map<String, ?>>> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new RuntimeException("JUnit test exception"));
    when(replayService.findByQuery("query", MAX_RESULTS, 1, sortOrder)).thenReturn(completableFuture);

    searchListener.accept(standardSearchConfig);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testPagination() {
    Consumer<SearchConfig> searchListener = searchListenerCaptor.getValue();
    List<Replay> list = new ArrayList<>();
    for (int i = 0; i != 100; i++) {
      list.add(new Replay());
    }
    CompletableFuture<List<Replay>> completableFuture = new CompletableFuture<>();
    completableFuture.complete(list);
    //when(replayService.findByQuery(eq("query"), eq(MAX_RESULTS), anyInt(), eq(sortOrder))).thenReturn(completableFuture);

    searchListener.accept(standardSearchConfig);

    // instance.onLoadMoreButtonClicked(new ActionEvent());

    WaitForAsyncUtils.waitForFxEvents();
    verify(replayService).findByQuery("query", MAX_RESULTS, 1, sortOrder);
    verify(replayService).findByQuery("query", MAX_RESULTS, 2, sortOrder);
    // assertThat(instance.moreButton.isVisible(), is(true));
  }

  @Test
  public void testFirstPageButton() {
    /* load replays
    navigate to page != 1
     simulate first page button press
    * check if on first page

    * */
    Consumer<SearchConfig> searchListener = searchListenerCaptor.getValue();
    List<Replay> list = new ArrayList<>();
    for (int i = 0; i != 100; i++) {
      list.add(new Replay());
    }
    CompletableFuture<List<Replay>> completableFuture = new CompletableFuture<>();
    completableFuture.complete(list);
    //when(replayService.findByQuery(eq("query"), eq(MAX_RESULTS), anyInt(), eq(sortOrder))).thenReturn(completableFuture);

    searchListener.accept(standardSearchConfig);

    //instance.onLoadMoreButtonClicked(new ActionEvent());

    WaitForAsyncUtils.waitForFxEvents();
    verify(replayService).findByQuery("query", MAX_RESULTS, 1, sortOrder);
    verify(replayService).findByQuery("query", MAX_RESULTS, 2, sortOrder);
    // assertThat(instance.moreButton.isVisible(), is(true));
  }

  @Test
  public void testLastPageButton() {
    /* load replays
     simulate last page button press
    * check if on last  page
    * */

    HashMap<String, Integer> innerHashMap = new HashMap<>();
    innerHashMap.put("totalPages", 4);
    HashMap<String, HashMap<String, Integer>> hashMap = new HashMap<>();
    hashMap.put("page", innerHashMap);

    Consumer<SearchConfig> searchListener = searchListenerCaptor.getValue();
    List<Replay> list = new ArrayList<>();
    for (int i = 0; i != 400; i++) {
      list.add(new Replay());
    }

    CompletableFuture<Tuple<List<Replay>, Map<String, ?>>> completableFuture = new CompletableFuture<>();
    completableFuture.complete(new Tuple<>(list, hashMap));
    when(replayService.findByQuery(eq("query"), eq(MAX_RESULTS), anyInt(), eq(sortOrder))).thenReturn(completableFuture);

    searchListener.accept(standardSearchConfig);

    instance.lastPageButton.fire();

    WaitForAsyncUtils.waitForFxEvents();
    verify(replayService).findByQuery("query", MAX_RESULTS, 1, sortOrder);
    verify(replayService).findByQuery("query", MAX_RESULTS, 4, sortOrder);
    assertThat(instance.pagination.getCurrentPageIndex(), is(3));
  }

  @Test
  public void showReplayButReplayNotPresent() {
    when(replayService.findById(anyInt())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    instance.display(new ShowReplayEvent(123));
    WaitForAsyncUtils.waitForFxEvents();
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
