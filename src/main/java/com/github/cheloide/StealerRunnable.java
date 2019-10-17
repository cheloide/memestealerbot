package com.github.cheloide;

import java.util.List;

import com.github.cheloide.service.DeferredMediaGroupService;
import com.github.cheloide.telegrambotapi.domain.SendPhotoRequest;
import com.github.cheloide.telegrambotapi.model.common.Message;
import com.github.cheloide.telegrambotapi.model.common.PhotoSize;
import com.github.cheloide.telegrambotapi.model.common.Update;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiService;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiUpdateService;

class StealerRunnable implements Runnable {

    private static long DEFERRED_TIMEOUT_MS = 3000;

    private TelegramBotApiUpdateService updateService;
    private TelegramBotApiService       botApi;
    private DeferredMediaGroupService   dmgs;
    private long                        dumpChannelId;
    private long                        postChannelId;

    public StealerRunnable(TelegramBotApiUpdateService updateService, TelegramBotApiService botApi, DeferredMediaGroupService dmgs, long dumpChannelId, long postChannelId) {

        this.updateService = updateService;
        this.botApi = botApi;
        this.dmgs = dmgs;
        this.dumpChannelId = dumpChannelId;
        this.postChannelId = postChannelId;

    }

    public void run() {
        Update update = updateService.poll();
        if (update != null && update.getChannelPost() != null && update.getChannelPost().getChat() != null && update.getChannelPost().getChat().getId() == dumpChannelId) {
            Message msg = update.getChannelPost();
            // if (update != null && update.getMessage() != null && update.getMessage().getChat() != null && update.getMessage().getChat().getId() == dumpChannelId) {
            // Message msg = update.getMessage();
            if (msg.getPhoto() != null && !msg.getPhoto().isEmpty()) {
                processPhoto(msg);
            } else if (msg.getVideo() != null) {

            }
        }
    }

    private void processPhoto(Message msg) {
        List<PhotoSize> photo        = msg.getPhoto();
        String          mediaGroupId = msg.getMediaGroupId();
        String          photoId      = photo.get(0).getFileId();

        if (mediaGroupId == null) {
            SendPhotoRequest request = new SendPhotoRequest();
            request.setChatId(postChannelId);
            request.setPhoto(photoId);
            botApi.sendPhoto(request);
        } else {
            dmgs.addPhoto(postChannelId, msg.getMediaGroupId(), photoId, DEFERRED_TIMEOUT_MS);
        }
    }
}