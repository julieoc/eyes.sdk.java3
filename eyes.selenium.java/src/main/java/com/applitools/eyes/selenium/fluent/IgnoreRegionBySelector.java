package com.applitools.eyes.selenium.fluent;

import com.applitools.eyes.*;
import com.applitools.eyes.fluent.GetRegion;
import com.applitools.eyes.selenium.Eyes;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

public class IgnoreRegionBySelector implements GetRegion {
    private By selector;

    public IgnoreRegionBySelector(By selector) {
        this.selector = selector;
    }

    @Override
    public Region getRegion(EyesBase eyesBase, EyesScreenshot screenshot) {
        WebElement element = ((Eyes)eyesBase).getDriver().findElement(this.selector);
        Point locationAsPoint = element.getLocation();
        Dimension size = element.getSize();

        // Element's coordinates are context relative, so we need to convert them first.
        Location adjustedLocation = screenshot.getLocationInScreenshot(new Location(locationAsPoint.getX(), locationAsPoint.getY()),
                CoordinatesType.CONTEXT_RELATIVE);

        return new Region(adjustedLocation, new RectangleSize(size.getWidth(), size.getHeight()),
                CoordinatesType.SCREENSHOT_AS_IS);
    }
}
