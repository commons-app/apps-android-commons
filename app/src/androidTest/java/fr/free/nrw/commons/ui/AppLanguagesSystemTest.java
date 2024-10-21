package fr.free.nrw.commons.ui;

import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiDevice;
import androidx.test.platform.app.InstrumentationRegistry;
import android.content.Intent;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;

public class AppLanguagesSystemTest {

    private UiDevice device;

    @Before
    public void setUp() {
        // Initiate UI Automator
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Use Intent to start system application
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        InstrumentationRegistry.getInstrumentation().getContext().startActivity(intent);

        // wait Settings starting
        device.waitForIdle();
    }

    @Test
    public void testCommonsAppLanguageOptionExists() throws Exception {
        // 1. Find and click "System"
        UiScrollable settingsList = new UiScrollable(new UiSelector().scrollable(true));
        UiObject systemOption = settingsList.getChildByText(new UiSelector().text("System"), "System");
        systemOption.click();

        // 2. scroll and find "Languages & input"
        UiObject languagesInputOption = device.findObject(new UiSelector()
            .className("android.widget.TextView")
            .textContains("Gboard"));
        languagesInputOption.clickAndWaitForNewWindow();




        // 3. detect "App languages" and click
        UiObject appLanguagesOption = device.findObject(new UiSelector().text("App languages"));
        appLanguagesOption.clickAndWaitForNewWindow();

        // 4. detect "Commons" APP is there
        UiScrollable appLanguagesList = new UiScrollable(new UiSelector().scrollable(true));
        UiObject commonsAppOption = appLanguagesList.getChildByText(new UiSelector().text("Commons"), "Commons");

        assert(commonsAppOption.exists());
    }
}
