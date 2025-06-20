package org.pitestidea.model;

import java.util.function.Function;

public abstract class BaseMutationsScore implements IMutationScore {
    private BaseMutationsScore lastScore;
    protected int survived = 0;
    protected int killed = 0;
    protected int noCoverage = 0;
    protected int timedOut = 0;
    protected int runError = 0;

    private final int order;

    public BaseMutationsScore(int order, BaseMutationsScore lastScore) {
        this.order = order;
        this.lastScore = lastScore;
        if (lastScore != null) {
            lastScore.lastScore = null; // Free this memory which is no longer useful
        }
    }

    @Override
    public IMutationScore getLastScore() {
        return lastScore;
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
    public int getRunErrors() {
        return runError;
    }

    @Override
    public int getMutationsTotal() {
        return survived + killed + noCoverage + timedOut + runError;
    }

    @Override
    public float getScore() {
        int total = getMutationsTotal();
        return total == 0 ? 0 : (100 * (float) killed / (float) total);
    }

    public static void fmtFloat(StringBuilder sb, IMutationScore score, Function<IMutationScore, Float> fn) {
        float fv = fn.apply(score);
        sb.append(String.format("%.0f&#37;", fv));
        if (score.getLastScore() != null) {
            boolean needsClosingParen = true;
            float lv = fn.apply(score.getLastScore());
            if (fv > lv) {
                sb.append("&nbsp;<small>(<span style='color:green'>&#8593;</span>");
            } else if (fv < lv) {
                sb.append("&nbsp;<small>(<span style='color:red'>&#8595;</span>");
            } else {
                needsClosingParen = false;
            }
            float diff = Math.abs(fv - lv);
            if (diff > 1.0) {
                sb.append("<i>");
                sb.append(String.format("%.0f&#37;", diff));
                sb.append("</i>");
            }
            if (needsClosingParen) {
                sb.append(')');
            }
            sb.append("</small>");
        }
    }

    private static void fmtInt(StringBuilder sb, String prefix, IMutationScore score, boolean greenPointsUp, Function<IMutationScore, Integer> fn) {
        final int iv = fn.apply(score);
        sb.append(iv);
        sb.append("&nbsp;");
        sb.append(prefix);
        if (score.getLastScore() != null) {
            final int lv = fn.apply(score.getLastScore());
            String color;
            String arrow;
            if (iv > lv) {
                color = greenPointsUp ? "green" : "red";
                arrow = "8593";
            } else if (iv < lv) {
                color = greenPointsUp ? "red" : "green";
                arrow = "8595";
            } else {
                return;
            }
            sb.append("&nbsp;<small><i>(<span style='color:");
            sb.append(color);
            sb.append("'>&#");
            sb.append(arrow);
            sb.append(';');
            sb.append(Math.abs(iv - lv));
            sb.append("</span>)</i></small>");
        }
    }

    private static void line(StringBuilder sb, IMutationScore score, boolean greenPointsUP, String prefix, Function<IMutationScore, Integer> fn) {
        sb.append("&nbsp;");
        fmtInt(sb, prefix, score, greenPointsUP, fn);
        sb.append("<br>&nbsp;&nbsp;&nbsp;&nbsp;");
    }

    @Override
    public String getScoreDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("This score is calculated as:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;");
        fmtFloat(sb, this, IMutationScore::getScore);
        sb.append(" = ");
        sb.append(getKilled());
        sb.append(" killed over ");
        sb.append(getMutationsTotal());
        sb.append(" total mutations");
        IMutationScore last = getLastScore();
        if (last != null) {
            float thisScore = getScore();
            float lastScore = last.getScore();
            if (thisScore > lastScore) {
                sb.append(", an increase from the previous run, as indicated by the set of green arrows.");
            } else if (thisScore < lastScore) {
                sb.append(", a decrease from the previous run, as indicated by the set of red arrows.");
            } else {
                sb.append('.');
            }
        }
        sb.append("<br><br>The total mutation count is the sum of:<br><br>&nbsp;&nbsp;&nbsp;&nbsp;");
        line(sb, this, true, "killed +", IMutationScore::getKilled);
        line(sb, this, false, "survived +", IMutationScore::getSurvived);
        line(sb, this, false, "no coverage +", IMutationScore::getNoCoverage);
        line(sb, this, false, "time outs", IMutationScore::getTimedOut);
        if (runError > 0) {
            sb.append(" +");
            line(sb, this, false, "run errors", IMutationScore::getRunErrors);
        }
        return sb.toString();
    }
}
