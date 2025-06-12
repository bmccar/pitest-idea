package org.pitestidea.model;

public abstract class BaseMutationsScore implements IMutationScore {
    protected int survived = 0;
    protected int killed = 0;
    protected int noCoverage = 0;
    protected int timedOut = 0;
    protected int runError = 0;

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
            case RUN_ERROR -> runError++;
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
        return survived + killed + noCoverage + timedOut + runError;
    }

    @Override
    public float getScore() {
        int total = getMutationsTotal();
        return total==0 ? 0 : (100*(float)killed/(float)total);
    }

    @Override
    public String getScoreDescription() {
        String description =  "How this score is calculated:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;" +
                String.format("%.2f%%", getScore()) +
                " = (" +
                killed +
                " killed) over (" +
                getMutationsTotal() +
                " total mutations).<br><br>That total is the sum of:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;" +
                killed +
                " killed +<br>&nbsp;&nbsp;&nbsp;&nbsp;" +
                survived +
                " survived +<br>&nbsp;&nbsp;&nbsp;&nbsp;" +
                noCoverage +
                " no coverage +<br>&nbsp;&nbsp;&nbsp;&nbsp;" +
                timedOut +
                " time outs";
        if (runError > 0) {
            description += "<br><br>&nbsp;&nbsp;&nbsp;&nbsp;" + runError + " run errors";
        }
        return description;
    }
}
