// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RemoteWebDriverUnitTest {

  private static final String ELEMENT_KEY = "element-6066-11e4-a52e-4f735466cecf";

  @Test
  public void constructorShouldThrowIfExecutorIsNull() {
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> new RemoteWebDriver((CommandExecutor) null, new ImmutableCapabilities()))
      .withMessage("RemoteWebDriver cannot work without a command executor");
  }

  @Test
  public void constructorShouldThrowIfExecutorThrowsOnAnAttemptToStartASession() {
    CommandExecutor executor = prepareExecutorMock(exceptionResponder);
    assertThatExceptionOfType(SessionNotCreatedException.class)
      .isThrownBy(() -> new RemoteWebDriver(executor, new ImmutableCapabilities()));

    verifyCommands(executor, null); // no commands
  }

  @Test
  public void constructorShouldThrowIfExecutorReturnsNullOnAnAttemptToStartASession() {
    CommandExecutor executor = prepareExecutorMock(nullResponder);
    assertThatExceptionOfType(SessionNotCreatedException.class)
      .isThrownBy(() -> new RemoteWebDriver(executor, new ImmutableCapabilities()));

    verifyCommands(executor, null); // no commands
  }

  @Test
  public void constructorShouldThrowIfExecutorReturnsAResponseWithNullValueOnAnAttemptToStartASession() {
    CommandExecutor executor = prepareExecutorMock(nullValueResponder);
    assertThatExceptionOfType(SessionNotCreatedException.class)
      .isThrownBy(() -> new RemoteWebDriver(executor, new ImmutableCapabilities()));

    verifyCommands(executor, null); // no commands
  }

  @Test
  public void constructorShouldThrowIfExecutorReturnsSomethingButNotCapabilitiesOnAnAttemptToStartASession() {
    CommandExecutor executor = prepareExecutorMock(valueResponder("OK"));
    assertThatExceptionOfType(SessionNotCreatedException.class)
      .isThrownBy(() -> new RemoteWebDriver(executor, new ImmutableCapabilities()));

    verifyCommands(executor, null); // no commands
  }

  @Test
  public void constructorStartsSessionAndPassesCapabilities() throws IOException {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);
    ImmutableCapabilities capabilities = new ImmutableCapabilities("browserName", "cheese browser");

    RemoteWebDriver driver = new RemoteWebDriver(executor, capabilities);

    verify(executor).execute(argThat(
      command -> command.getName().equals(DriverCommand.NEW_SESSION)
                 && command.getSessionId() == null
                 && command.getParameters().get("desiredCapabilities") == capabilities
    ));
    verifyNoMoreInteractions(executor);
    assertThat(driver.getSessionId()).isNotNull();
  }

  @Test
  public void canHandlePlatformNameCapability() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);
    ImmutableCapabilities capabilities = new ImmutableCapabilities(
      "browserName", "cheese browser", "platformName", Platform.MOJAVE);

    RemoteWebDriver driver = new RemoteWebDriver(executor, capabilities);

    assertThat(driver.getCapabilities().getPlatformName())
      .satisfies(p -> p.is(Platform.MOJAVE));
  }

  @Test
  public void canHandlePlatformOSSCapability() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);
    ImmutableCapabilities capabilities = new ImmutableCapabilities(
      "browserName", "cheese browser", "platform", Platform.MOJAVE);

    RemoteWebDriver driver = new RemoteWebDriver(executor, capabilities);

    assertThat(driver.getCapabilities().getPlatformName())
      .satisfies(p -> p.is(Platform.MOJAVE));
  }

  @Test
  public void canHandleUnknownPlatformNameAndFallsBackToUnix() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);
    ImmutableCapabilities capabilities = new ImmutableCapabilities(
      "browserName", "cheese browser", "platformName", "cheese platform");

    RemoteWebDriver driver = new RemoteWebDriver(executor, capabilities);

    assertThat(driver.getCapabilities().getPlatformName())
      .satisfies(p -> p.is(Platform.UNIX)); // fallback
  }

  @Test
  public void canHandleGetCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.get("http://some.host.com");

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(
        DriverCommand.GET, ImmutableMap.of("url", "http://some.host.com")));
  }

  @Test
  public void canHandleGetCurrentUrlCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("http://some.host.com"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    String url = driver.getCurrentUrl();

    assertThat(url).isEqualTo("http://some.host.com");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_CURRENT_URL, emptyMap()));
  }

  @Test
  public void canHandleGetTitleCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Hello, world!"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    String title = driver.getTitle();

    assertThat(title).isEqualTo("Hello, world!");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_TITLE, emptyMap()));
  }

  @Test
  public void canHandleGetPageSourceCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Hello, world!"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    String html = driver.getPageSource();

    assertThat(html).isEqualTo("Hello, world!");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_PAGE_SOURCE, emptyMap()));
  }

  @Test
  public void canHandleExecuteScriptCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Hello, world!"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.setLogLevel(Level.WARNING);
    Object result = driver.executeScript("return 1", 1, "2");

    assertThat(result).isEqualTo("Hello, world!");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.EXECUTE_SCRIPT, ImmutableMap.of(
        "script", "return 1", "args", Arrays.asList(1, "2"))));
  }

  @Test
  public void canHandleExecuteAsyncScriptCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Hello, world!"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Object result = driver.executeAsyncScript("return 1", 1, "2");

    assertThat(result).isEqualTo("Hello, world!");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.EXECUTE_ASYNC_SCRIPT, ImmutableMap.of(
        "script", "return 1", "args", Arrays.asList(1, "2"))));
  }

  @Test
  public void canHandleFindElementOSSCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(ImmutableMap.of("ELEMENT", UUID.randomUUID().toString())));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebElement found = driver.findElement(By.id("cheese"));

    assertThat(found).isNotNull();
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.FIND_ELEMENT, ImmutableMap.of(
        "using", "id", "value", "cheese")));
  }

  @Test
  public void canHandleFindElementW3CCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(ImmutableMap.of(ELEMENT_KEY, UUID.randomUUID().toString())));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebElement found = driver.findElement(By.id("cheese"));

    assertThat(found).isNotNull();
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.FIND_ELEMENT, ImmutableMap.of(
        "using", "id", "value", "cheese")));
  }

  @Test
  public void canHandleFindElementCommandWithNonStandardLocator() {
    WebElement element1 = mock(WebElement.class);
    WebElement element2 = mock(WebElement.class);
    By locator = new By() {
      @Override
      public List<WebElement> findElements(SearchContext context) {
        return Arrays.asList(element1, element2);
      }
    };
    CommandExecutor executor = prepareExecutorMock(echoCapabilities);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebElement found = driver.findElement(locator);

    assertThat(found).isSameAs(element1);
    verifyCommands(executor, driver.getSessionId() /* no commands */);
  }

  @Test
  public void canHandleFindElementsOSSCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(Arrays.asList(
        ImmutableMap.of("ELEMENT", UUID.randomUUID().toString()),
        ImmutableMap.of("ELEMENT", UUID.randomUUID().toString()))));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    List<WebElement> found = driver.findElements(By.id("cheese"));

    assertThat(found).hasSize(2);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.FIND_ELEMENTS, ImmutableMap.of(
        "using", "id", "value", "cheese")));
  }

  @Test
  public void canHandleFindElementsW3CCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(Arrays.asList(
        ImmutableMap.of(ELEMENT_KEY, UUID.randomUUID().toString()),
        ImmutableMap.of(ELEMENT_KEY, UUID.randomUUID().toString()))));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    List<WebElement> found = driver.findElements(By.id("cheese"));

    assertThat(found).hasSize(2);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.FIND_ELEMENTS, ImmutableMap.of(
        "using", "id", "value", "cheese")));
  }

  @Test
  public void canHandleFindElementsCommandWithNonStandardLocator() {
    WebElement element1 = mock(WebElement.class);
    WebElement element2 = mock(WebElement.class);
    By locator = new By() {
      @Override
      public List<WebElement> findElements(SearchContext context) {
        return Arrays.asList(element1, element2);
      }
    };
    CommandExecutor executor = prepareExecutorMock(echoCapabilities);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    List<WebElement> found = driver.findElements(locator);

    assertThat(found).containsExactly(element1, element2);
    verifyCommands(executor, driver.getSessionId());
  }

  @Test
  public void returnsEmptyListIfRemoteEndReturnsNullFromFindElements() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    List<WebElement> result = driver.findElements(By.id("id"));
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void returnsEmptyListIfRemoteEndReturnsNullFromFindChildren() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId("unique");

    List<WebElement> result = element.findElements(By.id("id"));
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  public void throwsIfRemoteEndReturnsNullFromFindElement() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    assertThatExceptionOfType(NoSuchElementException.class)
      .isThrownBy(() -> driver.findElement(By.id("id")));
  }

  @Test
  public void throwIfRemoteEndReturnsNullFromFindChild() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId("unique");

    assertThatExceptionOfType(NoSuchElementException.class)
      .isThrownBy(() -> element.findElement(By.id("id")));
  }

  @Test
  public void canHandleGetWindowHandleCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Hello, world!"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    String handle = driver.getWindowHandle();

    assertThat(handle).isEqualTo("Hello, world!");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_CURRENT_WINDOW_HANDLE, emptyMap()));
  }

  @Test
  public void canHandleGetWindowHandlesCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder(Arrays.asList("window 1", "window 2")));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Set<String> handles = driver.getWindowHandles();

    assertThat(handles).hasSize(2).contains("window 1", "window 2");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_WINDOW_HANDLES, emptyMap()));
  }

  @Test
  public void canHandleCloseCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.close();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.CLOSE, emptyMap()));
  }

  @Test
  public void canHandleQuitCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    SessionId sid = driver.getSessionId();
    driver.quit();

    assertThat(driver.getSessionId()).isNull();
    verifyCommands(
      executor, sid,
      new CommandPayload(DriverCommand.QUIT, emptyMap()));
  }

  @Test
  public void canHandleQuitCommandAfterQuit() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    SessionId sid = driver.getSessionId();
    driver.quit();

    assertThat(driver.getSessionId()).isNull();
    verifyCommands(
      executor, sid,
      new CommandPayload(DriverCommand.QUIT, emptyMap()));

    driver.quit();
    verifyNoMoreInteractions(executor);
  }

  @Test
  public void canHandleSwitchToWindowCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebDriver driver2 = driver.switchTo().window("window1");

    assertThat(driver2).isSameAs(driver);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SWITCH_TO_WINDOW, ImmutableMap.of("handle", "window1")));
  }

  @Test
  public void canHandleSwitchToNewWindowCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder(ImmutableMap.of("handle", "new window")));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebDriver driver2 = driver.switchTo().newWindow(WindowType.TAB);

    assertThat(driver2).isSameAs(driver);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_CURRENT_WINDOW_HANDLE, emptyMap()),
      new CommandPayload(DriverCommand.SWITCH_TO_NEW_WINDOW, ImmutableMap.of("type", "tab")),
      new CommandPayload(DriverCommand.SWITCH_TO_WINDOW, ImmutableMap.of("handle", "new window")));
  }

  @Test
  public void canHandleSwitchToFrameByIndexCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebDriver driver2 = driver.switchTo().frame(1);

    assertThat(driver2).isSameAs(driver);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SWITCH_TO_FRAME, ImmutableMap.of("id", 1)));
  }

  @Test
  public void canHandleSwitchToFrameByNameCommand() throws IOException {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(Arrays.asList(
        ImmutableMap.of(ELEMENT_KEY, UUID.randomUUID().toString()),
        ImmutableMap.of(ELEMENT_KEY, UUID.randomUUID().toString()))));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebDriver driver2 = driver.switchTo().frame("frameName");

    assertThat(driver2).isSameAs(driver);
    verify(executor).execute(argThat(
      command -> command.getName().equals(DriverCommand.NEW_SESSION)));
    verify(executor).execute(argThat(
      command -> command.getName().equals(DriverCommand.FIND_ELEMENTS)));
    verify(executor).execute(argThat(
      command -> command.getName().equals(DriverCommand.SWITCH_TO_FRAME)
                 && command.getParameters().size() == 1
                 && isWebElement(command.getParameters().get("id"))));
    verifyNoMoreInteractions(executor);
  }

  @Test
  public void canHandleSwitchToNonExistingFrameCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder(EMPTY_LIST));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    assertThatExceptionOfType(NoSuchFrameException.class)
      .isThrownBy(() -> driver.switchTo().frame("frameName"));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.FIND_ELEMENTS, ImmutableMap.of(
        "using", "css selector", "value", "frame[name='frameName'],iframe[name='frameName']")),
      new CommandPayload(DriverCommand.FIND_ELEMENTS, ImmutableMap.of(
        "using", "css selector", "value", "frame#frameName,iframe#frameName")));
  }

  @Test
  public void canHandleSwitchToParentFrameCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebDriver driver2 = driver.switchTo().parentFrame();

    assertThat(driver2).isSameAs(driver);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SWITCH_TO_PARENT_FRAME, emptyMap()));
  }

  @Test
  public void canHandleSwitchToTopCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    WebDriver driver2 = driver.switchTo().defaultContent();

    assertThat(driver2).isSameAs(driver);
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SWITCH_TO_FRAME, Collections.singletonMap("id", null)));
  }

  @Test
  public void canHandleSwitchToAlertCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Alarm!"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Alert alert = driver.switchTo().alert();

    assertThat(alert.getText()).isEqualTo("Alarm!");
    verifyCommands(
      executor, driver.getSessionId(),
      new MultiCommandPayload(2, DriverCommand.GET_ALERT_TEXT, emptyMap()));
  }

  @Test
  public void canHandleAlertAcceptCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Alarm!"), nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.switchTo().alert().accept();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ALERT_TEXT, emptyMap()),
      new CommandPayload(DriverCommand.ACCEPT_ALERT, emptyMap()));
  }

  @Test
  public void canHandleAlertDismissCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Alarm!"), nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.switchTo().alert().dismiss();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ALERT_TEXT, emptyMap()),
      new CommandPayload(DriverCommand.DISMISS_ALERT, emptyMap()));
  }

  @Test
  public void canHandleAlertSendKeysCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder("Are you sure?"), nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.switchTo().alert().sendKeys("no");

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ALERT_TEXT, emptyMap()),
      new CommandPayload(DriverCommand.SET_ALERT_VALUE, ImmutableMap.of("text", "no")));
  }

  @Test
  public void canHandleRefreshCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.navigate().refresh();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.REFRESH, emptyMap()));
  }

  @Test
  public void canHandleBackCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.navigate().back();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GO_BACK, emptyMap()));
  }

  @Test
  public void canHandleForwardCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.navigate().forward();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GO_FORWARD, emptyMap()));
  }

  @Test
  public void canHandleNavigateToCommand() throws IOException {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.navigate().to(new URL("http://www.test.com/"));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET, ImmutableMap.of("url", "http://www.test.com/")));
  }

  @Test
  public void canHandleGetCookiesCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(Arrays.asList(
        ImmutableMap.of("name", "cookie1", "value", "value1", "sameSite", "Lax"),
        ImmutableMap.of("name", "cookie2", "value", "value2"))));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Set<Cookie> cookies = driver.manage().getCookies();

    assertThat(cookies)
      .hasSize(2)
      .contains(
        new Cookie.Builder("cookie1", "value1").sameSite("Lax").build(),
        new Cookie("cookie2", "value2"));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ALL_COOKIES, ImmutableMap.of()));
  }

  @Test
  public void canHandleGetCookieNamedCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(Arrays.asList(
        ImmutableMap.of("name", "cookie1", "value", "value1"),
        ImmutableMap.of("name", "cookie2", "value", "value2"))));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Cookie found = driver.manage().getCookieNamed("cookie2");

    assertThat(found).isEqualTo(new Cookie("cookie2", "value2"));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ALL_COOKIES, emptyMap()));
  }

  @Test
  public void canHandleAddCookieCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    Cookie cookie = new Cookie("x", "y");
    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().addCookie(cookie);

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.ADD_COOKIE, ImmutableMap.of("cookie", cookie)));
  }

  @Test
  public void canHandleDeleteCookieCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    Cookie cookie = new Cookie("x", "y");
    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().deleteCookie(cookie);

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.DELETE_COOKIE, ImmutableMap.of("name", "x")));
  }

  @Test
  public void canHandleDeleteAllCookiesCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().deleteAllCookies();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.DELETE_ALL_COOKIES, emptyMap()));
  }

  @Test
  public void canHandleGetWindowSizeCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(ImmutableMap.of("width", 400, "height", 600)));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Dimension size = driver.manage().window().getSize();

    assertThat(size).isEqualTo(new Dimension(400, 600));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_CURRENT_WINDOW_SIZE, emptyMap()));
  }

  @Test
  public void canHandleSetWindowSizeCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().window().setSize(new Dimension(400, 600));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SET_CURRENT_WINDOW_SIZE,
                         ImmutableMap.of("width", 400, "height", 600)));
  }

  @Test
  public void canHandleGetWindowPositionCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(ImmutableMap.of("x", 100, "y", 200)));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    Point position = driver.manage().window().getPosition();

    assertThat(position).isEqualTo(new Point(100, 200));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_CURRENT_WINDOW_POSITION,
                         ImmutableMap.of("windowHandle", "current")));
  }

  @Test
  public void canHandleSetWindowPositionCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().window().setPosition(new Point(100, 200));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SET_CURRENT_WINDOW_POSITION,
                         ImmutableMap.of("x", 100, "y", 200)));
  }

  @Test
  public void canHandleMaximizeCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().window().maximize();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.MAXIMIZE_CURRENT_WINDOW, emptyMap()));
  }

  @Test
  public void canHandleFullscreenCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().window().fullscreen();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.FULLSCREEN_CURRENT_WINDOW, emptyMap()));
  }

  @Test
  public void canHandleSetImplicitWaitCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SET_TIMEOUT, ImmutableMap.of("implicit", 10000L)));
  }

  @Test
  public void canHandleSetScriptTimeoutCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().timeouts().setScriptTimeout(Duration.ofSeconds(10));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SET_TIMEOUT, ImmutableMap.of("script", 10000L)));
  }

  @Test
  public void canHandleSetPageLoadTimeoutCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SET_TIMEOUT, ImmutableMap.of("pageLoad", 10000L)));
  }

  @Test
  public void canHandleIME() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder(singletonList("cheese")));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    List<String> engines = driver.manage().ime().getAvailableEngines();

    assertThat(engines).hasSize(1).contains("cheese");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.IME_GET_AVAILABLE_ENGINES, emptyMap()));
  }

  @Test
  public void canHandleElementClickCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    element.click();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.CLICK_ELEMENT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleWebDriverExceptionThrownByCommandExecutor() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, webDriverExceptionResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities(
      "browserName", "cheese", "platformName", "WINDOWS"));
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    String elementId = UUID.randomUUID().toString();
    element.setId(elementId);
    element.setFoundBy(driver, "id", "test");

    assertThatExceptionOfType(WebDriverException.class)
      .isThrownBy(element::click)
      .withMessageStartingWith("BOOM!!!")
      .withMessageContaining("Build info: ")
      .withMessageContaining(
        "Driver info: org.openqa.selenium.remote.RemoteWebDriver")
      .withMessageContaining(String.format(
        "Session ID: %s", driver.getSessionId()))
      .withMessageContaining(String.format(
        "%s", driver.getCapabilities()))
      .withMessageContaining(String.format(
        "Command: [%s, clickElement {id=%s}]", driver.getSessionId(), elementId))
      .withMessageContaining(String.format(
        "Element: [[RemoteWebDriver: cheese on WINDOWS (%s)] -> id: test]", driver.getSessionId()));

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.CLICK_ELEMENT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleGeneralExceptionThrownByCommandExecutor() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, exceptionResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities(
      "browserName", "cheese", "platformName", "WINDOWS"));
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    String elementId = UUID.randomUUID().toString();
    element.setId(elementId);
    element.setFoundBy(driver, "id", "test");

    assertThatExceptionOfType(WebDriverException.class)
      .isThrownBy(element::click)
      .withMessageStartingWith("Error communicating with the remote browser. It may have died.")
      .withMessageContaining("Build info: ")
      .withMessageContaining(
        "Driver info: org.openqa.selenium.remote.RemoteWebDriver")
      .withMessageContaining(String.format(
        "Session ID: %s", driver.getSessionId()))
      .withMessageContaining(String.format(
        "%s", driver.getCapabilities()))
      .withMessageContaining(String.format(
        "Command: [%s, clickElement {id=%s}]", driver.getSessionId(), elementId))
      .withMessageContaining(String.format(
        "Element: [[RemoteWebDriver: cheese on WINDOWS (%s)] -> id: test]", driver.getSessionId()))
      .havingCause()
      .withMessage("BOOM!!!");

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.CLICK_ELEMENT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementClearCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    element.clear();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.CLEAR_ELEMENT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementSubmitCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    element.submit();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SUBMIT_ELEMENT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementSendKeysCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, nullValueResponder);

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());
    element.setFileDetector(mock(FileDetector.class));

    element.sendKeys("test");

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.SEND_KEYS_TO_ELEMENT, ImmutableMap.of(
        "id", element.getId(), "value", new CharSequence[]{"test"})));
  }

  @Test
  public void canHandleElementGetAttributeCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder("test"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    String attr = element.getAttribute("id");

    assertThat(attr).isEqualTo("test");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_ATTRIBUTE, ImmutableMap.of(
        "id", element.getId(), "name", "id")));
  }

  @Test
  public void canHandleElementIsSelectedCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder(true));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    assertThat(element.isSelected()).isTrue();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.IS_ELEMENT_SELECTED, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementIsEnabledCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder(true));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    assertThat(element.isEnabled()).isTrue();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.IS_ELEMENT_ENABLED, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementIsDisplayedCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder(true));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    assertThat(element.isDisplayed()).isTrue();

    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.IS_ELEMENT_DISPLAYED, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementGeTextCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder("test"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    String text = element.getText();

    assertThat(text).isEqualTo("test");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_TEXT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementGetTagNameCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder("div"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    String tag = element.getTagName();

    assertThat(tag).isEqualTo("div");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_TAG_NAME,
                         ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementGetLocationCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder(ImmutableMap.of("x", 10, "y", 20)));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    Point location = element.getLocation();

    assertThat(location).isEqualTo(new Point(10, 20));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_LOCATION,
                         ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementGetSizeCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities, valueResponder(ImmutableMap.of("width", 100, "height", 200)));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    Dimension size = element.getSize();

    assertThat(size).isEqualTo(new Dimension(100, 200));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_SIZE, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementGetRectCommand() {
    CommandExecutor executor = prepareExecutorMock(
      echoCapabilities,
      valueResponder(ImmutableMap.of("x", 10, "y", 20, "width", 100, "height", 200)));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    Rectangle rect = element.getRect();

    assertThat(rect).isEqualTo(new Rectangle(new Point(10, 20), new Dimension(100, 200)));
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_RECT, ImmutableMap.of("id", element.getId())));
  }

  @Test
  public void canHandleElementCssPropertyCommand() {
    CommandExecutor executor = prepareExecutorMock(echoCapabilities, valueResponder("red"));

    RemoteWebDriver driver = new RemoteWebDriver(executor, new ImmutableCapabilities());
    RemoteWebElement element = new RemoteWebElement();
    element.setParent(driver);
    element.setId(UUID.randomUUID().toString());

    String color = element.getCssValue("color");

    assertThat(color).isEqualTo("red");
    verifyCommands(
      executor, driver.getSessionId(),
      new CommandPayload(DriverCommand.GET_ELEMENT_VALUE_OF_CSS_PROPERTY,
                         ImmutableMap.of("id", element.getId(), "propertyName", "color")));
  }

  private static class MultiCommandPayload extends CommandPayload {
    private final int times;

    MultiCommandPayload(int times, String name, Map<String, ?> parameters) {
      super(name, parameters);
      this.times = times;
    }

    public int getTimes() {
      return times;
    }
  }

  private void verifyCommands(CommandExecutor executor, SessionId sid, CommandPayload... commands) {
    InOrder inOrder = Mockito.inOrder(executor);
    try {
      inOrder.verify(executor).execute(argThat(
        command -> command.getName().equals(DriverCommand.NEW_SESSION)));
      for (CommandPayload target : commands) {
        int
          x =
          target instanceof MultiCommandPayload ? ((MultiCommandPayload) target).getTimes() : 1;
        inOrder.verify(executor, times(x)).execute(argThat(
          cmd -> cmd.getSessionId().equals(sid)
                 && cmd.getName().equals(target.getName())
                 && areEqual(cmd.getParameters(), target.getParameters())));
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    verifyNoMoreInteractions(executor);
  }

  private boolean areEqual(Map<String, ?> left, Map<String, ?> right) {
    if (left.size() != right.size()) {
      return false;
    }
    if (! left.keySet().equals(right.keySet())) {
      return false;
    }
    for (String key : left.keySet()) {
      if (! areEqual(left.get(key), right.get(key))) {
        return false;
      }
    }
    return true;
  }

  private boolean areEqual(Object left, Object right) {
    if (left == null) {
      return right == null;
    }
    if (! left.getClass().isArray()) {
      return left.equals(right);
    }
    if (! right.getClass().isArray()) {
      return false;
    }
    for(int i = 0; i < Array.getLength(left); i++) {
      if (! Array.get(left, i).equals(Array.get(right, i))) {
        return false;
      }
    }
    return true;
  }

  private boolean isWebElement(Object value) {
    return Optional.of(value)
      .filter(v -> v instanceof Map)
      .map(v -> (Map<String, Object>) v)
      .filter(m -> m.size() == 2)
      .filter(m -> m.containsKey("ELEMENT") && m.containsKey(ELEMENT_KEY))
      .filter(m -> m.get("ELEMENT").equals(m.get(ELEMENT_KEY)))
      .isPresent();
  }

  private final Function<Command, Response> nullResponder = cmd -> null;

  private final Function<Command, Response> exceptionResponder = cmd -> {
    throw new InternalError("BOOM!!!");
  };

  private final Function<Command, Response> webDriverExceptionResponder = cmd -> {
    throw new WebDriverException("BOOM!!!");
  };

  private final Function<Command, Response> nullValueResponder = valueResponder(null);

  private Function<Command, Response> valueResponder(Object value) {
    return cmd -> {
      Response response = new Response();
      response.setValue(value);
      response.setSessionId(cmd.getSessionId() != null ? cmd.getSessionId().toString() : null);
      return response;
    };
  }

  private final Function<Command, Response> echoCapabilities = cmd -> {
    Response response = new Response();
    response.setValue(
      ((Capabilities) cmd.getParameters().get("desiredCapabilities")).asMap()
        .entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString())));
    response.setSessionId(UUID.randomUUID().toString());
    return response;
  };

  @SafeVarargs
  private final CommandExecutor prepareExecutorMock(Function<Command, Response>... handlers)
  {
    CommandExecutor executor = mock(CommandExecutor.class);
    try {
      OngoingStubbing<Response> callChain = when(executor.execute(any()));
      for (Function<Command, Response> handler : handlers) {
        callChain = callChain.thenAnswer(invocation -> handler.apply(invocation.getArgument(0)));
      }
      return executor;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
