package org.pitestidea.model;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMutationsScore implements IMutationScore {
    protected int survived = 0;
    protected int killed = 0;
    protected int noCoverage = 0;
    protected int timedOut = 0;

    private final int order;

    public BaseMutationsScore(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void accountFor(MutationImpact impact) {
        switch (impact) {
            case KILLED -> killed++;
            case SURVIVED -> survived++;
            case NO_COVERAGE -> noCoverage++;
            case TIMED_OUT -> timedOut++;
        }
    }

    @Override
    public int getSurvived() {
        return survived;
    }

    @Override
    public int getKilled() {
        return killed;
    }

    @Override
    public int getNoCoverage() {
        return noCoverage;
    }

    @Override
    public int getTimedOut() {
        return timedOut;
    }

    @Override
    public int getMutationsTotal() {
        return survived + killed + noCoverage + timedOut;
    }

    @Override
    public float getScore() {
        return 100*(float)killed/(float)getMutationsTotal();
    }

}
