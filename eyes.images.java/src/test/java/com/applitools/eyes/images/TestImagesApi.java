package com.applitools.eyes.images;

import com.applitools.eyes.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

public class TestImagesApi {
    private Eyes eyes;
    private static final String TEST_SUITE_NAME = "Eyes Image SDK";
    private static BatchInfo batchInfo;

    @BeforeClass
    public static void setUpOnce() {
        batchInfo = new BatchInfo(TEST_SUITE_NAME);
    }

    @BeforeMethod
    public void setup(Method method) {
        eyes = new Eyes();

        LogHandler logHandler = new StdoutLogHandler(true);
        eyes.setLogHandler(logHandler);

        eyes.setBatch(batchInfo);

        String testName = method.getName();
        eyes.open(TEST_SUITE_NAME, testName);

        eyes.setDebugScreenshotsPrefix("Java_Images_SDK_" + testName + "_");
    }

    @AfterMethod
    public void tearDown() {
        eyes.close();
        eyes.abortIfNotClosed();
    }

    @Test
    public void TestCheckImage() {
        eyes.checkImage("resources/minions-800x500.jpg");
    }

    @Test
    public void TestCheckImage_Fluent() {
        eyes.check("TestCheckImage_Fluent", Target.image("resources/minions-800x500.jpg"));
    }

    @Test
    public void TestCheckImage_WithIgnoreRegion_Fluent() {
        eyes.check("TestCheckImage_WithIgnoreRegion_Fluent", Target.image("resources/minions-800x500.jpg")
                .ignore(new Region(10, 20, 30, 40)));
    }

    @Test
    public void TestCheckImage_Fluent_CutProvider() {
        eyes.setImageCut(new UnscaledFixedCutProvider(200, 100, 100, 50));
        eyes.check("TestCheckImage_Fluent", Target.image("resources/minions-800x500.jpg"));
    }
}
