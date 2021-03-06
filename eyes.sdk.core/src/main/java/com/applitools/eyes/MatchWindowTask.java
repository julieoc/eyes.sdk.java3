/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes;

import com.applitools.eyes.capture.AppOutputProvider;
import com.applitools.eyes.capture.AppOutputWithScreenshot;
import com.applitools.eyes.fluent.GetFloatingRegion;
import com.applitools.eyes.fluent.GetRegion;
import com.applitools.eyes.fluent.ICheckSettingsInternal;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MatchWindowTask {

    private static final int MATCH_INTERVAL = 500; // Milliseconds

    private final Logger logger;
    private final ServerConnector serverConnector;
    private final RunningSession runningSession;
    private final int defaultRetryTimeout;
    private final AppOutputProvider appOutputProvider;

    private EyesScreenshot lastScreenshot = null;
    private MatchResult matchResult;
    private Region lastScreenshotBounds;

    /**
     * @param logger            A logger instance.
     * @param serverConnector   Our gateway to the agent
     * @param runningSession    The running session in which we should match the window
     * @param retryTimeout      The default total time to retry matching (ms).
     * @param appOutputProvider A callback for getting the application output when performing match.
     */
    public MatchWindowTask(Logger logger, ServerConnector serverConnector,
                           RunningSession runningSession, int retryTimeout,
                           AppOutputProvider appOutputProvider) {
        ArgumentGuard.notNull(serverConnector, "serverConnector");
        ArgumentGuard.notNull(runningSession, "runningSession");
        ArgumentGuard.greaterThanOrEqualToZero(retryTimeout, "retryTimeout");
        ArgumentGuard.notNull(appOutputProvider, "appOutputProvider");

        this.logger = logger;
        this.serverConnector = serverConnector;
        this.runningSession = runningSession;
        this.defaultRetryTimeout = retryTimeout;
        this.appOutputProvider = appOutputProvider;
    }

    /**
     * Creates the match data and calls the server connector matchWindow method.
     * @param userInputs         The user inputs related to the current appOutput.
     * @param appOutput          The application output to be matched.
     * @param tag                Optional tag to be associated with the match (can be {@code null}).
     * @param ignoreMismatch     Whether to instruct the server to ignore the match attempt in case of a mismatch.
     * @param imageMatchSettings The settings to use.
     * @return The match result.
     */
    protected MatchResult performMatch(Trigger[] userInputs,
                                       AppOutputWithScreenshot appOutput,
                                       String tag, boolean ignoreMismatch,
                                       ImageMatchSettings imageMatchSettings) {

        // Prepare match data.
        MatchWindowData data = new MatchWindowData(userInputs, appOutput
                .getAppOutput(), tag, ignoreMismatch,
                new MatchWindowData.Options(tag, userInputs, ignoreMismatch,
                        false, false, false,
                        imageMatchSettings));

        // Perform match.
        return serverConnector.matchWindow(runningSession, data);
    }

    /**
     * Repeatedly obtains an application snapshot and matches it with the next
     * expected output, until a match is found or the timeout expires.
     * @param userInputs             User input preceding this match.
     * @param region                 Window region to capture.
     * @param tag                    Optional tag to be associated with the match (can be {@code null}).
     * @param shouldRunOnceOnTimeout Force a single match attempt at the end of the match timeout.
     * @param ignoreMismatch         Whether to instruct the server to ignore the match attempt in case of a mismatch.
     * @param checkSettingsInternal     The settings to use.
     * @param retryTimeout           The amount of time to retry matching in milliseconds or a
     *                               negative value to use the default retry timeout.
     * @param eyes                   The current EyesBase instance.
     * @return Returns the results of the match
     */
    public MatchResult matchWindow(Trigger[] userInputs,
                                   Region region, String tag,
                                   boolean shouldRunOnceOnTimeout,
                                   boolean ignoreMismatch,
                                   ICheckSettingsInternal checkSettingsInternal,
                                   int retryTimeout, EyesBase eyes) {

        if (retryTimeout < 0) {
            retryTimeout = defaultRetryTimeout;
        }

        logger.verbose(String.format("retryTimeout = %d", retryTimeout));

        EyesScreenshot screenshot = takeScreenshot(userInputs, region, tag,
                shouldRunOnceOnTimeout, ignoreMismatch, checkSettingsInternal, retryTimeout, eyes);

        if (ignoreMismatch) {
            return matchResult;
        }

        updateLastScreenshot(screenshot);
        updateBounds(region);

        return matchResult;
    }

    private void collectIgnoreRegions(ICheckSettingsInternal checkSettingsInternal,
                                      ImageMatchSettings imageMatchSettings, EyesBase eyes,
                                      EyesScreenshot screenshot) {

        List<Region> ignoreRegions = new ArrayList<>();
        for (GetRegion ignoreRegionProvider : checkSettingsInternal.getIgnoreRegions()) {
            try {
                ignoreRegions.add(ignoreRegionProvider.getRegion(eyes, screenshot));
            }
            catch (OutOfBoundsException ex){
                logger.log("WARNING - ignore region was out of bounds.");
            }
        }
        imageMatchSettings.setIgnoreRegions(ignoreRegions.toArray(new Region[0]));
    }

    private void collectFloatingRegions(ICheckSettingsInternal checkSettingsInternal,
                                        ImageMatchSettings imageMatchSettings, EyesBase eyes,
                                        EyesScreenshot screenshot) {
        List<FloatingMatchSettings> floatingRegions = new ArrayList<>();
        for (GetFloatingRegion floatingRegionProvider : checkSettingsInternal.getFloatingRegions()) {
            floatingRegions.add(floatingRegionProvider.getRegion(eyes, screenshot));
        }
        imageMatchSettings.setFloatingRegions(floatingRegions.toArray(new FloatingMatchSettings[0]));
    }

    /**
     * Build match settings by merging the check settings and the default match settings.
     * @param checkSettingsInternal
     * @param eyes
     * @param screenshot
     * @return Merged match settings.
     */
    private ImageMatchSettings createImageMatchSettings(ICheckSettingsInternal checkSettingsInternal,
                                                               EyesBase eyes, EyesScreenshot screenshot) {
        ImageMatchSettings imageMatchSettings = null;
        if (checkSettingsInternal != null) {


            imageMatchSettings = new ImageMatchSettings(checkSettingsInternal.getMatchLevel(), null);

            collectIgnoreRegions(checkSettingsInternal, imageMatchSettings, eyes, screenshot);
            collectFloatingRegions(checkSettingsInternal, imageMatchSettings, eyes, screenshot);

            imageMatchSettings.setIgnoreCaret(checkSettingsInternal.getIgnoreCaret());
        }
        return imageMatchSettings;
    }

    private EyesScreenshot takeScreenshot(Trigger[] userInputs, Region region, String tag,
                                          boolean shouldMatchWindowRunOnceOnTimeout,
                                          boolean ignoreMismatch, ICheckSettingsInternal checkSettingsInternal,
                                          int retryTimeout, EyesBase eyes) {
        long elapsedTimeStart = System.currentTimeMillis();
        EyesScreenshot screenshot;

        // If the wait to load time is 0, or "run once" is true,
        // we perform a single check window.
        if (0 == retryTimeout || shouldMatchWindowRunOnceOnTimeout) {

            if (shouldMatchWindowRunOnceOnTimeout) {
                GeneralUtils.sleep(retryTimeout);
            }
            screenshot = tryTakeScreenshot(userInputs, region, tag, ignoreMismatch, checkSettingsInternal, eyes);
        } else {
            screenshot = retryTakingScreenshot(userInputs, region, tag, ignoreMismatch, checkSettingsInternal,
                    retryTimeout, eyes);
        }

        double elapsedTime = (System.currentTimeMillis() - elapsedTimeStart) / 1000;
        logger.verbose(String.format("Completed in %.2f seconds", elapsedTime));
        //matchResult.setScreenshot(screenshot);
        return screenshot;
    }

    private EyesScreenshot retryTakingScreenshot(Trigger[] userInputs, Region region, String tag, boolean ignoreMismatch,
                                                 ICheckSettingsInternal checkSettingsInternal, int retryTimeout,
                                                 EyesBase eyes) {
        // Start the retry timer.
        long start = System.currentTimeMillis();

        EyesScreenshot screenshot = null;

        long retry = System.currentTimeMillis() - start;

        // The match retry loop.
        while (retry < retryTimeout) {

            // Wait before trying again.
            GeneralUtils.sleep(MATCH_INTERVAL);

            screenshot = tryTakeScreenshot(userInputs, region, tag, true, checkSettingsInternal, eyes);

            if (matchResult.getAsExpected()) {
                break;
            }

            retry = System.currentTimeMillis() - start;
        }

        // if we're here because we haven't found a match yet, try once more
        if (!matchResult.getAsExpected()) {
            screenshot = tryTakeScreenshot(userInputs, region, tag, ignoreMismatch, checkSettingsInternal, eyes);
        }
        return screenshot;
    }

    private EyesScreenshot tryTakeScreenshot(Trigger[] userInputs, Region region, String tag,
                                             boolean ignoreMismatch, ICheckSettingsInternal checkSettingsInternal,
                                             EyesBase eyes) {
        AppOutputWithScreenshot appOutput = appOutputProvider.getAppOutput(region, lastScreenshot, checkSettingsInternal);
        EyesScreenshot screenshot = appOutput.getScreenshot();
        ImageMatchSettings matchSettings = createImageMatchSettings(checkSettingsInternal, eyes, screenshot);
        matchResult = performMatch(userInputs, appOutput, tag, ignoreMismatch, matchSettings);
        return screenshot;
    }

    private void updateLastScreenshot(EyesScreenshot screenshot) {
        if (screenshot != null) {
            lastScreenshot = screenshot;
        }
    }

    private void updateBounds(Region region) {
        if (region.isSizeEmpty()) {
            if (lastScreenshot == null) {
                // We set an "infinite" image size since we don't know what the screenshot size is...
                lastScreenshotBounds = new Region(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            } else {
                BufferedImage image = lastScreenshot.getImage();
                lastScreenshotBounds = new Region(0, 0, image.getWidth(), image.getHeight());
            }
        } else {
            lastScreenshotBounds = region;
        }
    }

    public Region getLastScreenshotBounds() {
        return lastScreenshotBounds;
    }

}
