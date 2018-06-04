using System;
using System.Collections.Generic;
using NUnit.Framework;
using System.Collections.ObjectModel;
using OpenQA.Selenium.Environment;

namespace OpenQA.Selenium
{
    [TestFixture]
    public class ElementPropertyTest : DriverTestFixture
    {
        [Test]
        [IgnoreBrowser(Browser.Chrome, "Chrome does not support get element property command")]
        [IgnoreBrowser(Browser.Opera)]
        [IgnoreBrowser(Browser.Remote)]
        [IgnoreBrowser(Browser.Safari, "Safari does not support get element property command")]
        public void ShouldReturnNullWhenGettingTheValueOfAPropertyThatIsNotListed()
        {
            driver.Url = simpleTestPage;
            IWebElement head = driver.FindElement(By.XPath("/html"));
            string attribute = head.GetProperty("cheese");
            Assert.IsNull(attribute);
        }

        [Test]
        [IgnoreBrowser(Browser.Chrome, "Chrome does not support get element property command")]
        [IgnoreBrowser(Browser.Opera)]
        [IgnoreBrowser(Browser.Remote)]
        [IgnoreBrowser(Browser.Safari, "Safari does not support get element property command")]
        public void CanRetrieveTheCurrentValueOfAProperty()
        {
            driver.Url = formsPage;
            IWebElement element = driver.FindElement(By.Id("working"));
            Assert.AreEqual(string.Empty, element.GetProperty("value"));
            element.SendKeys("hello world");
            Assert.AreEqual("hello world", element.GetProperty("value"));
        }
    }
}
