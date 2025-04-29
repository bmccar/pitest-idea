package org.pitestidea.toolwindow;

public record DisplayChoices(
        Viewing.PackageChoice packageChoice,
        Sorting.By sortBy,
        Sorting.Direction sortDirection) {
}
