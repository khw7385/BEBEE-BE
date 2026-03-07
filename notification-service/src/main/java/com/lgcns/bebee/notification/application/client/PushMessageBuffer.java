package com.lgcns.bebee.notification.application.client;

import com.lgcns.bebee.notification.core.dto.PushMessageDTO;

import java.util.List;
import java.util.Map;

public interface PushMessageBuffer {

    void add(String token, String title, String body, Map<String, String> data);

    List<PushMessageDTO> drain(int maxSize);

    int size();

    boolean isEmpty();
}
