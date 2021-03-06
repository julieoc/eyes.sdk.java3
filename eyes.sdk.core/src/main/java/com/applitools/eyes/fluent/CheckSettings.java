package com.applitools.eyes.fluent;

import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * The Match settings object to use in the various Eyes.Check methods.
 */
public class CheckSettings implements ICheckSettings, ICheckSettingsInternal {

    private Region targetRegion;
    private MatchLevel matchLevel = null;
    private Boolean ignoreCaret = null;
    private boolean stitchContent = false;
    private List<GetRegion> ignoreRegions = new ArrayList<>();
    private List<GetFloatingRegion> floatingRegions = new ArrayList<>();
    private int timeout = -1;

    protected CheckSettings() { }

    protected CheckSettings(Region region) {
        this.targetRegion = region;
    }

    /**
     * For internal use only.
     * @param timeout timeout
     */
    public CheckSettings(int timeout) {
        this.timeout = timeout;
    }

    protected void ignore(Region region) {
        this.ignore(new IgnoreRegionByRectangle(region));
    }

    protected void ignore(GetRegion regionProvider) {
        ignoreRegions.add(regionProvider);
    }

    protected void floating_(Region region, int maxUpOffset, int maxDownOffset, int maxLeftOffset, int maxRightOffset) {
        this.floatingRegions.add(
                new FloatingRegionByRectangle(region, maxUpOffset, maxDownOffset, maxLeftOffset, maxRightOffset)
        );
    }

    protected void floating(GetFloatingRegion regionProvider){
        this.floatingRegions.add(regionProvider);
    }

    @Override
    public CheckSettings clone(){
        CheckSettings clone = new CheckSettings();
        populateClone(clone);
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings ignore(Region... regions) {
        CheckSettings clone = clone();
        for (Region r : regions) {
            clone.ignore(r);
        }
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings fully() {
        CheckSettings clone = clone();
        clone.stitchContent = true;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings fully(boolean fully) {
        CheckSettings clone = clone();
        clone.stitchContent = fully;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings floating(int maxOffset, Region... regions) {
        CheckSettings clone = clone();
        for (Region r : regions) {
            clone.floating_(r, maxOffset, maxOffset, maxOffset, maxOffset);
        }
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings floating(Region region, int maxUpOffset, int maxDownOffset, int maxLeftOffset, int maxRightOffset) {
        CheckSettings clone = clone();
        clone.floating_(region, maxUpOffset, maxDownOffset, maxLeftOffset, maxRightOffset);
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings timeout(int timeoutMilliseconds) {
        CheckSettings clone = clone();
        clone.timeout = timeoutMilliseconds;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings layout() {
        CheckSettings clone = clone();
        clone.matchLevel = MatchLevel.LAYOUT;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings exact() {
        CheckSettings clone = clone();
        clone.matchLevel = MatchLevel.EXACT;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings strict() {
        CheckSettings clone = clone();
        clone.matchLevel = MatchLevel.STRICT;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings content() {
        CheckSettings clone = clone();
        clone.matchLevel = MatchLevel.CONTENT;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings matchLevel(MatchLevel matchLevel) {
        CheckSettings clone = clone();
        clone.matchLevel = matchLevel;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings ignoreCaret(boolean ignoreCaret) {
        CheckSettings clone = clone();
        clone.ignoreCaret = ignoreCaret;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICheckSettings ignoreCaret() {
        CheckSettings clone = clone();
        clone.ignoreCaret = true;
        return clone;
    }

    @Override
    public Region getTargetRegion() {
        return this.targetRegion;
    }

    @Override
    public int getTimeout() {
        return this.timeout;
    }

    @Override
    public boolean getStitchContent() {
        return this.stitchContent;
    }

    @Override
    public MatchLevel getMatchLevel() {
        return this.matchLevel;
    }

    @Override
    public GetRegion[] getIgnoreRegions() {
        return this.ignoreRegions.toArray(new GetRegion[0]);
    }

    @Override
    public GetFloatingRegion[] getFloatingRegions() {
        return this.floatingRegions.toArray(new GetFloatingRegion[0]);
    }

    @Override
    public Boolean getIgnoreCaret() {
        return this.ignoreCaret;
    }

    protected void updateTargetRegion(Region region) {
        this.targetRegion = region;
    }

    protected void populateClone(CheckSettings clone) {
        clone.targetRegion = this.targetRegion;
        clone.matchLevel = this.matchLevel;
        clone.stitchContent = this.stitchContent;
        clone.timeout = this.timeout;

        clone.ignoreRegions.addAll(this.ignoreRegions);
        clone.floatingRegions.addAll(this.floatingRegions);
    }
}
