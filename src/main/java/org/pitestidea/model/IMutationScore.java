package org.pitestidea.model;

public interface IMutationScore {
    void accountFor(MutationImpact impact);
    float getScore();
    int getSurvived();
    int getKilled();
    int getNoCoverage();
    int getTimedOut();
    int getMutationsTotal();
}
