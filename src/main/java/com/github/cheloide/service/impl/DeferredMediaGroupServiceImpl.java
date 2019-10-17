package com.github.cheloide.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cheloide.service.DeferredMediaGroupService;
import com.github.cheloide.telegrambotapi.model.common.InputMedia;
import com.github.cheloide.telegrambotapi.model.common.InputMediaPhoto;
import com.github.cheloide.telegrambotapi.model.common.InputMediaVideo;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiService;
import com.github.cheloide.telegrambotapi.service.TelegramHttpRequestService;
import com.github.cheloide.telegrambotapi.service.impl.TelegramBotApiServiceImpl;
import com.github.cheloide.telegrambotapi.service.impl.TelegramHttpRequestServiceImpl;

public class DeferredMediaGroupServiceImpl implements DeferredMediaGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeferredMediaGroupServiceImpl.class);

    private static Map<String, DeferredMediaGroupService> instances = new HashMap<>();
    private static Lock                                   lock      = new ReentrantLock();

    private ScheduledExecutorService       scheduleExecutorService;
    private ExecutorService                executorService;
    private Lock                           instanceLock;
    private Map<String, MediaGroupWrapper> mediaGroupMap;

    private DeferredMediaGroupServiceImpl(String token) {
        scheduleExecutorService = Executors.newSingleThreadScheduledExecutor();
        executorService = Executors.newCachedThreadPool();
        instanceLock = new ReentrantLock();

        TelegramHttpRequestService httpRequestService = new TelegramHttpRequestServiceImpl(token);
        TelegramBotApiService      botApi             = new TelegramBotApiServiceImpl(httpRequestService);
        mediaGroupMap = new HashMap<>();
        scheduleExecutorService.scheduleAtFixedRate(() -> {

            executorService.submit(() -> {

                LOGGER.info("Processing Deferred Media groups");
                if (instanceLock.tryLock()) {
                    try {
                        List<MediaGroupWrapper> requestWrappers = mediaGroupMap.values().stream().filter(MediaGroupWrapper::isDue).collect(Collectors.toList());

                        LOGGER.debug("Found {} valid deferred mediaGroup messages", requestWrappers.size());
                        for (MediaGroupWrapper requestWrapper : requestWrappers) {
                            if (requestWrapper.hasValidSize()) {
                                botApi.sendMediaGroup(requestWrapper.getRequest());

                            } else {
                                mediaGroupMap.remove(requestWrapper.getMediaGroupId());
                            }
                        }

                    } finally {
                        instanceLock.unlock();
                    }
                }
            });
        }, 0, 3000, TimeUnit.MILLISECONDS);
    }

    public static DeferredMediaGroupService getInstance(String token) {
        lock.lock();
        try {
            if (!instances.containsKey(token)) {
                DeferredMediaGroupService service = new DeferredMediaGroupServiceImpl(token);
                instances.put(token, service);
            }
            return instances.get(token);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addPhoto(long chatId, String mediaGroupId, String photoId, long timeout) {
        InputMediaPhoto media = new InputMediaPhoto();
        media.setMedia(photoId);
        addMedia(chatId, mediaGroupId, timeout, media);
    }

    @Override
    public void addVideo(long chatId, String mediaGroupId, String videoId, long timeout) {
        InputMediaVideo media = new InputMediaVideo();
        media.setMedia(videoId);
        addMedia(chatId, mediaGroupId, timeout, media);
    }

    private void addMedia(long chatId, String mediaGroupId, long timeout, InputMedia media) {
        instanceLock.lock();
        try {
            MediaGroupWrapper wrapper = mediaGroupMap.getOrDefault(mediaGroupId, new MediaGroupWrapper(chatId, timeout, mediaGroupId));
            wrapper.addMedia(media);
            mediaGroupMap.put(mediaGroupId, wrapper);
        } finally {
            instanceLock.unlock();
        }
    }

}
