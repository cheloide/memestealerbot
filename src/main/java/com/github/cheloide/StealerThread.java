package com.github.cheloide;

import com.github.cheloide.telegrambotapi.domain.SendPhotoRequest;
import com.github.cheloide.telegrambotapi.model.common.Message;
import com.github.cheloide.telegrambotapi.model.common.Update;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiService;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiUpdateService;
import com.github.cheloide.telegrambotapi.service.TelegramHttpRequestService;
import com.github.cheloide.telegrambotapi.service.impl.TelegramBotApiServiceImpl;
import com.github.cheloide.telegrambotapi.service.impl.TelegramHttpRequestServiceImpl;
import com.github.cheloide.telegrambotapi.updater.TelegramBotApiUpdateServiceImpl;

public class StealerThread implements Runnable {

    private TelegramHttpRequestService  httpRequestService;
    private TelegramBotApiUpdateService UpdateService;
    private TelegramBotApiService       botApi;
    private long                        chatIdDump = 0;
    private long                        chatIdPost = 0;

    public StealerThread() {

        httpRequestService = new TelegramHttpRequestServiceImpl("xxx");
        UpdateService = new TelegramBotApiUpdateServiceImpl(httpRequestService);
        botApi = new TelegramBotApiServiceImpl(httpRequestService);

        UpdateService.startUpdates();
    }

    public void run() {
        while (true) {
            Update update = UpdateService.poll();
            if (update != null) {
                String fileId;
                if (update.getChannelPost() != null) {
                    Message msg = update.getChannelPost();
                    if (msg.getChat() != null) {
                        if (msg.getChat().getId() == chatIdDump) {
                            if (msg.getPhoto() != null && !msg.getPhoto().isEmpty()) {
                                fileId = msg.getPhoto().get(0).getFileId();
                                SendPhotoRequest request = new SendPhotoRequest();
                                request.setChatId(chatIdPost);
                                request.setPhoto(fileId);
                                System.out.println(fileId);
                                botApi.sendPhoto(request);
                            }
                        }
                    }
                }

            }
            try {
                Thread.currentThread();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
