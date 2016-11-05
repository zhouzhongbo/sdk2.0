package com.droi.common.event;

import com.droi.common.logging.DroiLog;

class LogCatEventRecorder implements EventRecorder {
    @Override
    public void record(final BaseEvent baseEvent) {
        DroiLog.d(baseEvent.toString());
    }
}

