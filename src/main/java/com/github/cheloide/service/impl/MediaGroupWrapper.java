package com.github.cheloide.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.github.cheloide.telegrambotapi.domain.SendMediaGroupRequest;
import com.github.cheloide.telegrambotapi.model.common.InputMedia;

public class MediaGroupWrapper {

    private Instant               timeout;
    private long                  delay;
    private SendMediaGroupRequest request;
    private String                mediaGroupId;

    public MediaGroupWrapper(long chatId, long delay, String mediaGroupId) {

        List<InputMedia> media = new ArrayList<>();
        request = new SendMediaGroupRequest();
        request.setChatId(chatId);
        request.setMedia(media);
        this.delay = delay;
        this.timeout = Instant.now().plus(delay, ChronoUnit.MILLIS);
        this.mediaGroupId = mediaGroupId;
    }

    public void addMedia(InputMedia media) {
        request.getMedia().add(media);
        this.timeout = Instant.now().plus(delay, ChronoUnit.MILLIS);
    }

    public boolean isDue() {
        return timeout.isAfter(Instant.now());
    }

    public boolean hasValidSize() {
        return request.getMedia().size() > 1;
    }

    public SendMediaGroupRequest getRequest() {
        return request;
    }

    /**
     * @return the mediaGroupId
     */
    public String getMediaGroupId() {
        return mediaGroupId;
    }

}
