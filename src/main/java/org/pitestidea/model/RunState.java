package org.pitestidea.model;

public enum RunState {
    RUNNING(true), COMPLETED(true), CANCELLED(false), FAILED(false);
    final public boolean valid;

    RunState(boolean valid) {
        this.valid = valid;
    }
    public boolean isValid() {
        return valid;
    }
}
