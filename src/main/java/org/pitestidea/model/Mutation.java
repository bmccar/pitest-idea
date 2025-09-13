package org.pitestidea.model;

public record Mutation(
        String method,
        int lineNumber,
        MutationImpact mutationImpact,
        String description) {
}
