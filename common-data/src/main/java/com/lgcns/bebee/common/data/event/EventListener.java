package com.lgcns.bebee.common.data.event;


public interface EventListener {

    void handleEvent(String payload, String eventType);
}