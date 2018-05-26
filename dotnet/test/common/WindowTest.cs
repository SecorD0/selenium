using System;
using System.Drawing;
using NUnit.Framework;

namespace OpenQA.Selenium
{
    [TestFixture]
    //[IgnoreBrowser(Browser.Chrome, "Not implemented in driver")]
    public class WindowTest : DriverTestFixture
    {
        private Size originalWindowSize;

        [SetUp]
        public void GetBrowserWindowSize()
        {
            this.originalWindowSize = driver.Manage().Window.Size;

        }

        [TearDown]
        public void RestoreBrowserWindow()
        {
            driver.Manage().Window.Size = originalWindowSize;
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToGetTheSizeOfTheCurrentWindow()
        {
            Size size = driver.Manage().Window.Size;
            Assert.Greater(size.Width, 0);
            Assert.Greater(size.Height, 0);
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToSetTheSizeOfTheCurrentWindow()
        {
            IWindow window = driver.Manage().Window;
            Size size = window.Size;

            // resize relative to the initial size, since we don't know what it is
            Size targetSize = new Size(size.Width - 20, size.Height - 20);
            window.Size = targetSize;

            Size newSize = window.Size;
            Assert.AreEqual(targetSize.Width, newSize.Width);
            Assert.AreEqual(targetSize.Height, newSize.Height);
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToSetTheSizeOfTheCurrentWindowFromFrame()
        {
            IWindow window = driver.Manage().Window;
            Size size = window.Size;
            driver.Url = framesetPage;
            driver.SwitchTo().Frame("fourth");

            try
            {
                // resize relative to the initial size, since we don't know what it is
                Size targetSize = new Size(size.Width - 20, size.Height - 20);
                window.Size = targetSize;


                Size newSize = window.Size;
                Assert.AreEqual(targetSize.Width, newSize.Width);
                Assert.AreEqual(targetSize.Height, newSize.Height);
            }
            finally
            {
                driver.SwitchTo().DefaultContent();
            }
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToSetTheSizeOfTheCurrentWindowFromIFrame()
        {
            IWindow window = driver.Manage().Window;
            Size size = window.Size;
            driver.Url = iframePage;
            driver.SwitchTo().Frame("iframe1-name");

            try
            {
                // resize relative to the initial size, since we don't know what it is
                Size targetSize = new Size(size.Width - 20, size.Height - 20);
                window.Size = targetSize;


                Size newSize = window.Size;
                Assert.AreEqual(targetSize.Width, newSize.Width);
                Assert.AreEqual(targetSize.Height, newSize.Height);
            }
            finally
            {
                driver.SwitchTo().DefaultContent();
            }
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToGetThePositionOfTheCurrentWindow()
        {
            Point position = driver.Manage().Window.Position;
            Assert.Greater(position.X, 0);
            Assert.Greater(position.Y, 0);
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToMaximizeTheCurrentWindow()
        {
            Size targetSize = new Size(450, 275);

            ChangeSizeTo(targetSize);

            Maximize();

            IWindow window = driver.Manage().Window;
            Assert.Greater(window.Size.Height, targetSize.Height);
            Assert.Greater(window.Size.Width, targetSize.Width);
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToMaximizeTheWindowFromFrame()
        {
            driver.Url = framesetPage;
            ChangeSizeTo(new Size(450, 275));

            driver.SwitchTo().Frame("fourth");
            try
            {
                Maximize();
            }
            finally
            {
                driver.SwitchTo().DefaultContent();
            }
        }

        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToMaximizeTheWindowFromIframe()
        {
            driver.Url = iframePage;
            ChangeSizeTo(new Size(450, 275));

            driver.SwitchTo().Frame("iframe1-name");
            try
            {
                Maximize();
            }
            finally
            {
                driver.SwitchTo().DefaultContent();
            }
        }

        //------------------------------------------------------------------
        // Tests below here are not included in the Java test suite
        //------------------------------------------------------------------
        [Test]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToSetThePositionOfTheCurrentWindow()
        {
            IWindow window = driver.Manage().Window;
            Point position = window.Position;

            Point targetPosition = new Point(position.X + 10, position.Y + 10);
            window.Position = targetPosition;

            Point newLocation = window.Position;

            Assert.AreEqual(targetPosition.X, newLocation.X);
            Assert.AreEqual(targetPosition.Y, newLocation.Y);
        }


        [Test]
        [IgnoreBrowser(Browser.Edge, "Not implemented in driver")]
        [IgnoreBrowser(Browser.Firefox, "Not implemented in driver")]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToFullScreenTheCurrentWindow()
        {
            Size targetSize = new Size(450, 275);

            ChangeSizeTo(targetSize);

            FullScreen();

            IWindow window = driver.Manage().Window;
            Size windowSize = window.Size;
            Point windowPosition = window.Position;
            Assert.Greater(windowSize.Height, targetSize.Height);
            Assert.Greater(windowSize.Width, targetSize.Width);
        }

        [Test]
        [IgnoreBrowser(Browser.Edge, "Not implemented in driver")]
        [IgnoreBrowser(Browser.Chrome, "Not implemented in driver")]
        [IgnoreBrowser(Browser.Firefox, "Not implemented in driver")]
        [IgnoreBrowser(Browser.Opera, "Not implemented in driver")]
        public void ShouldBeAbleToMinimizeTheCurrentWindow()
        {
            Size targetSize = new Size(450, 275);

            ChangeSizeTo(targetSize);

            Minimize();

            IWindow window = driver.Manage().Window;
            Size windowSize = window.Size;
            Point windowPosition = window.Position;
            Assert.Less(windowSize.Height, targetSize.Height);
            Assert.Less(windowSize.Width, targetSize.Width);
            Assert.Less(windowPosition.X, 0);
            Assert.Less(windowPosition.Y, 0);
        }

        private void FullScreen()
        {
            IWindow window = driver.Manage().Window;
            Size currentSize = window.Size;
            window.FullScreen();
        }

        private void Maximize()
        {
            IWindow window = driver.Manage().Window;
            Size currentSize = window.Size;
            window.Maximize();
            WaitFor(WindowHeightToBeGreaterThan(currentSize.Height), "Window height was not greater than " + currentSize.Height.ToString());
            WaitFor(WindowWidthToBeGreaterThan(currentSize.Width), "Window width was not greater than " + currentSize.Width.ToString());
        }

        private void Minimize()
        {
            IWindow window = driver.Manage().Window;
            Size currentSize = window.Size;
            window.Minimize();
        }

        private void ChangeSizeTo(Size targetSize)
        {
            IWindow window = driver.Manage().Window;
            window.Size = targetSize;
            WaitFor(WindowHeightToBeEqualTo(targetSize.Height), "Window height was not " + targetSize.Height.ToString());
            WaitFor(WindowWidthToBeEqualTo(targetSize.Width), "Window width was not " + targetSize.Width.ToString());
        }

        private void ChangeSizeBy(int deltaX, int deltaY)
        {
            IWindow window = driver.Manage().Window;
            Size size = window.Size;
            ChangeSizeTo(new Size(size.Width + deltaX, size.Height + deltaY));
        }

        private Func<bool> WindowHeightToBeEqualTo(int height)
        {
            return () => { return driver.Manage().Window.Size.Height == height; };
        }

        private Func<bool> WindowWidthToBeEqualTo(int width)
        {
            return () => { return driver.Manage().Window.Size.Width == width; };
        }

        private Func<bool> WindowHeightToBeGreaterThan(int height)
        {
            return () => { return driver.Manage().Window.Size.Height > height; };
        }

        private Func<bool> WindowWidthToBeGreaterThan(int width)
        {
            return () => { return driver.Manage().Window.Size.Width > width; };
        }
    }
}
