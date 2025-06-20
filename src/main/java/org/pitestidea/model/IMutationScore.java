package org.pitestidea.model;

public interface IMutationScore {
    String getName();
    String getQualifiedName();
    void accountFor(MutationImpact impact);
    float getScore();
    String getScoreDescription();
    int getSurvived();
    int getKilled();
    int getNoCoverage();
    int getTimedOut();
    int getRunErrors();
    int getMutationsTotal();
    int getOrder();
    IMutationScore getLastScore();
}
