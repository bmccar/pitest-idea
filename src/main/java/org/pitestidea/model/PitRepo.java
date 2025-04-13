package org.pitestidea.model;

public class PitRepo {
    private static PitExecutionRecorder recorder;
    public static void set(PitExecutionRecorder recorder) {
        PitRepo.recorder = recorder;
    }
    public static PitExecutionRecorder get() {
        return recorder;
    }
}
