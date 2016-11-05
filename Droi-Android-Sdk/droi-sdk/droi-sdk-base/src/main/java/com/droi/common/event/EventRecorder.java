package com.droi.common.event;

/**
 * This interface represents a backend to which Droi client events are logged.
 */
public interface EventRecorder {
    void record(BaseEvent baseEvent);
}
