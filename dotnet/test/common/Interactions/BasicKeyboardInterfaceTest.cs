using NUnit.Framework;
using OpenQA.Selenium.Environment;
using System;
using System.Drawing;

namespace OpenQA.Selenium.Interactions
{
    [TestFixture]
    public class BasicKeyboardInterfaceTest : DriverTestFixture
    {
        [SetUp]
        public void Setup()
        {
            new Actions(driver).SendKeys(Keys.Null).Perform();
        }

        [TearDown]
        public void ReleaseModifierKeys()
        {
            new Actions(driver).SendKeys(Keys.Null).Perform();
        }

        [Test]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowBasicKeyboardInput()
        {
            driver.Url = javascriptPage;

            IWebElement keyReporter = driver.FindElement(By.Id("keyReporter"));

            // Scroll the element into view before attempting any actions on it.
            ((IJavaScriptExecutor)driver).ExecuteScript("arguments[0].scrollIntoView();", keyReporter);

            Actions actionProvider = new Actions(driver);
            IAction sendLowercase = actionProvider.SendKeys(keyReporter, "abc def").Build();

            sendLowercase.Perform();

            Assert.AreEqual("abc def", keyReporter.GetAttribute("value"));

        }

        [Test]
        [IgnoreBrowser(Browser.Firefox, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowSendingKeyDownOnly()
        {
            driver.Url = javascriptPage;

            IWebElement keysEventInput = driver.FindElement(By.Id("theworks"));

            // Scroll the element into view before attempting any actions on it.
            ((IJavaScriptExecutor)driver).ExecuteScript("arguments[0].scrollIntoView();", keysEventInput);

            Actions actionProvider = new Actions(driver);

            IAction pressShift = actionProvider.KeyDown(keysEventInput, Keys.Shift).Build();
            pressShift.Perform();

            IWebElement keyLoggingElement = driver.FindElement(By.Id("result"));
            string logText = keyLoggingElement.Text;

            IAction releaseShift = actionProvider.KeyDown(keysEventInput, Keys.Shift).Build();
            releaseShift.Perform();

            Assert.IsTrue(logText.EndsWith("keydown"), "Key down event not isolated. Log text should end with 'keydown', got: " + logText);
        }

        [Test]
        [IgnoreBrowser(Browser.Firefox, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Chrome, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowSendingKeyUp()
        {
            driver.Url = javascriptPage;
            IWebElement keysEventInput = driver.FindElement(By.Id("theworks"));

            // Scroll the element into view before attempting any actions on it.
            ((IJavaScriptExecutor)driver).ExecuteScript("arguments[0].scrollIntoView();", keysEventInput);

            IAction pressShift = new Actions(driver).KeyDown(keysEventInput, Keys.Shift).Build();
            pressShift.Perform();

            IWebElement keyLoggingElement = driver.FindElement(By.Id("result"));

            string eventsText = keyLoggingElement.Text;
            Assert.IsTrue(keyLoggingElement.Text.EndsWith("keydown"), "Key down should be isolated for this test to be meaningful. Event text should end with 'keydown', got events: " + eventsText);

            IAction releaseShift = new Actions(driver).KeyUp(keysEventInput, Keys.Shift).Build();

            releaseShift.Perform();

            eventsText = keyLoggingElement.Text;
            Assert.IsTrue(keyLoggingElement.Text.EndsWith("keyup"), "Key up event not isolated. Event text should end with 'keyup', got: " + eventsText);
        }

        [Test]
        [IgnoreBrowser(Browser.Firefox, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Chrome, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowSendingKeysWithShiftPressed()
        {
            driver.Url = javascriptPage;

            IWebElement keysEventInput = driver.FindElement(By.Id("theworks"));

            keysEventInput.Click();

            IAction pressShift = new Actions(driver).KeyDown(Keys.Shift).Build();
            pressShift.Perform();

            IAction sendLowercase = new Actions(driver).SendKeys("ab").Build();
            sendLowercase.Perform();

            IAction releaseShift = new Actions(driver).KeyUp(Keys.Shift).Build();
            releaseShift.Perform();

            AssertThatFormEventsFiredAreExactly("focus keydown keydown keypress keyup keydown keypress keyup keyup"); 

            Assert.AreEqual("AB", keysEventInput.GetAttribute("value"));
        }

        [Test]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowSendingKeysToActiveElement()
        {
            driver.Url = bodyTypingPage;

            Actions actionProvider = new Actions(driver);
            IAction someKeys = actionProvider.SendKeys("ab").Build();
            someKeys.Perform();

            AssertThatBodyEventsFiredAreExactly("keypress keypress");
            IWebElement formLoggingElement = driver.FindElement(By.Id("result"));
            AssertThatFormEventsFiredAreExactly(string.Empty); 
        }

        [Test]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowBasicKeyboardInputOnActiveElement()
        {
            driver.Url = javascriptPage;

            IWebElement keyReporter = driver.FindElement(By.Id("keyReporter"));

            keyReporter.Click();

            Actions actionProvider = new Actions(driver);
            IAction sendLowercase = actionProvider.SendKeys("abc def").Build();

            sendLowercase.Perform();

            Assert.AreEqual("abc def", keyReporter.GetAttribute("value"));
        }

        [Test]
        public void ThrowsIllegalArgumentExceptionWithNullKeys()
        {
            driver.Url = javascriptPage;
            Assert.That(() => driver.FindElement(By.Id("keyReporter")).SendKeys(null), Throws.InstanceOf<ArgumentNullException>());
        }

        [Test]
        [IgnoreBrowser(Browser.Safari, "Not yet implemented")]
        public void CanGenerateKeyboardShortcuts()
        { 
            driver.Url = EnvironmentManager.Instance.UrlBuilder.WhereIs("keyboard_shortcut.html");

            IWebElement body = driver.FindElement(By.XPath("//body"));
            AssertBackgroundColor(body, Color.White);

            new Actions(driver).KeyDown(Keys.Shift).SendKeys("1").KeyUp(Keys.Shift).Perform();
            AssertBackgroundColor(body, Color.Green);

            new Actions(driver).KeyDown(Keys.Alt).SendKeys("1").KeyUp(Keys.Alt).Perform();
            AssertBackgroundColor(body, Color.LightBlue);

            new Actions(driver)
                .KeyDown(Keys.Shift).KeyDown(Keys.Alt)
                .SendKeys("1")
                .KeyUp(Keys.Shift).KeyUp(Keys.Alt)
                .Perform();
            AssertBackgroundColor(body, Color.Silver);
        }

        [Test]
        [IgnoreBrowser(Browser.Firefox, "https://bugzilla.mozilla.org/show_bug.cgi?id=1422583")]
        public void SelectionSelectBySymbol()
        {
            driver.Url = EnvironmentManager.Instance.UrlBuilder.WhereIs("single_text_input.html");

            IWebElement input = driver.FindElement(By.Id("textInput"));

            new Actions(driver).Click(input).SendKeys("abc def").Perform();

            WaitFor(() => input.GetAttribute("value") == "abc def", "did not send initial keys");

            new Actions(driver).Click(input)
                .KeyDown(Keys.Shift)
                .SendKeys(Keys.Left)
                .SendKeys(Keys.Left)
                .KeyUp(Keys.Shift)
                .SendKeys(Keys.Delete)
                .Perform();

            Assert.That(input.GetAttribute("value"), Is.EqualTo("abc d"));
        }

        [Test]
        //[IgnoreBrowser(Browser.IE)]
        [IgnoreBrowser(Browser.Firefox, "https://bugzilla.mozilla.org/show_bug.cgi?id=1422583")]
        public void SelectionSelectByWord()
        {
            driver.Url = EnvironmentManager.Instance.UrlBuilder.WhereIs("single_text_input.html");

            IWebElement input = driver.FindElement(By.Id("textInput"));

            new Actions(driver).Click(input).SendKeys("abc def").Perform();

            WaitFor(() => input.GetAttribute("value") == "abc def", "did not send initial keys");

            new Actions(driver).Click(input)
                .KeyDown(Keys.Shift)
                .KeyDown(Keys.Control)
                .SendKeys(Keys.Left)
                .KeyUp(Keys.Control)
                .KeyUp(Keys.Shift)
                .SendKeys(Keys.Delete)
                .Perform();

            WaitFor(() => input.GetAttribute("value") == "abc ", "did not send editing keys");
        }

        [Test]
        //[IgnoreBrowser(Browser.IE)]
        public void SelectionSelectAll()
        {
            driver.Url = EnvironmentManager.Instance.UrlBuilder.WhereIs("single_text_input.html");

            IWebElement input = driver.FindElement(By.Id("textInput"));

            new Actions(driver).Click(input).SendKeys("abc def").Perform();

            WaitFor(() => input.GetAttribute("value") == "abc def", "did not send initial keys");

            new Actions(driver).Click(input)
                .KeyDown(Keys.Control)
                .SendKeys("a")
                .KeyUp(Keys.Control)
                .SendKeys(Keys.Delete)
                .Perform();

            Assert.That(input.GetAttribute("value"), Is.EqualTo(string.Empty));
        }

        //------------------------------------------------------------------
        // Tests below here are not included in the Java test suite
        //------------------------------------------------------------------
        [Test]
        [IgnoreBrowser(Browser.Firefox, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Chrome, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Remote, "API not implemented in driver")]
        [IgnoreBrowser(Browser.Safari, "API not implemented in driver")]
        public void ShouldAllowSendingKeysWithLeftShiftPressed()
        {
            driver.Url = javascriptPage;

            IWebElement keysEventInput = driver.FindElement(By.Id("theworks"));

            keysEventInput.Click();

            IAction pressShift = new Actions(driver).KeyDown(Keys.LeftShift).Build();
            pressShift.Perform();

            IAction sendLowercase = new Actions(driver).SendKeys("ab").Build();
            sendLowercase.Perform();

            IAction releaseShift = new Actions(driver).KeyUp(Keys.LeftShift).Build();
            releaseShift.Perform();

            AssertThatFormEventsFiredAreExactly("focus keydown keydown keypress keyup keydown keypress keyup keyup"); 

            Assert.AreEqual("AB", keysEventInput.GetAttribute("value"));
        }

        private void AssertThatFormEventsFiredAreExactly(string message, string expected)
        {
            Assert.AreEqual(expected, driver.FindElement(By.Id("result")).Text.Trim(), message);
        }

        private void AssertThatFormEventsFiredAreExactly(string expected)
        {
            AssertThatFormEventsFiredAreExactly(string.Empty, expected);
        }

        private void AssertThatBodyEventsFiredAreExactly(string expected)
        {
            Assert.AreEqual(expected, driver.FindElement(By.Id("body_result")).Text.Trim());
        }

        private Func<bool> BackgroundColorToChangeFrom(IWebElement element, Color currentColor)
        {
            return () =>
            {
                string hexValue = string.Format("#{0:x2}{1:x2}{2:x2}", currentColor.R, currentColor.G, currentColor.B);
                string rgbValue = string.Format("rgb({0}, {1}, {2}", currentColor.R, currentColor.G, currentColor.B);
                string rgbaValue = string.Format("rgba({0}, {1}, {2}, 1)", currentColor.R, currentColor.G, currentColor.B);
                string actual = element.GetCssValue("background-color");
                return actual != hexValue && actual != rgbValue && actual != rgbaValue;
            };
        }

        private void AssertBackgroundColor(IWebElement el, Color expected)
        {
            string hexValue = string.Format("#{0:x2}{1:x2}{2:x2}", expected.R, expected.G, expected.B);
            string rgbValue = string.Format("rgb({0}, {1}, {2}", expected.R, expected.G, expected.B);
            string rgbaValue = string.Format("rgba({0}, {1}, {2}, 1)", expected.R, expected.G, expected.B);
            string actual = el.GetCssValue("background-color");
            Assert.That(actual, Is.EqualTo(hexValue).Or.EqualTo(rgbValue).Or.EqualTo(rgbaValue));
        }
    }
}
