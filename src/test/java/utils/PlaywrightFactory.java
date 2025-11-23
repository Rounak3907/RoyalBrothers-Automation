package utils;

import com.microsoft.playwright.*;

public class PlaywrightFactory {

    private static Playwright playwright;
    private static Browser browser;
    private static BrowserContext context;
    private static Page page;

    public static Page initBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
                        .setSlowMo(50)
        );

        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1500, 900)
                        .setLocale("en-US")
                        .setGeolocation(null)
                        .setPermissions(java.util.Collections.emptyList())
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124")
                        .setIgnoreHTTPSErrors(true)
        );

        context.clearCookies();
        context.clearPermissions();
        page = context.newPage();
        return page;
    }

    public static void closeBrowser() {
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
