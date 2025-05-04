package org.pitestidea.model;

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
        int total = getMutationsTotal();
        return total==0 ? 0 : (100*(float)killed/(float)total);
    }

    @Override
    public String getScoreDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("How this score is calculated:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;");
        sb.append(String.format("%.2f%%",getScore()));
        sb.append(" = (");
        sb.append(killed);
        sb.append(" killed) over (");
        sb.append(getMutationsTotal());
        sb.append(" total mutations).<br><br>That total is the sum of:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;");
        sb.append(killed);
        sb.append(" killed +<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        sb.append(survived);
        sb.append(" survived +<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        sb.append(noCoverage);
        sb.append(" no coverage +<br>&nbsp;&nbsp;&nbsp;&nbsp;");
        sb.append(timedOut);
        sb.append(" time outs");
        return sb.toString();
    }
}
