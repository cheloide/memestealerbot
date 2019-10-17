package com.github.cheloide;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cheloide.service.DeferredMediaGroupService;
import com.github.cheloide.service.impl.DeferredMediaGroupServiceImpl;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiService;
import com.github.cheloide.telegrambotapi.service.TelegramBotApiUpdateService;
import com.github.cheloide.telegrambotapi.service.TelegramHttpRequestService;
import com.github.cheloide.telegrambotapi.service.impl.TelegramBotApiServiceImpl;
import com.github.cheloide.telegrambotapi.service.impl.TelegramHttpRequestServiceImpl;
import com.github.cheloide.telegrambotapi.updater.TelegramBotApiUpdateServiceImpl;

public class MemeStealerBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemeStealerBot.class);
    private static String       botToken;
    private static long         dumpChannelId;
    private static long         postChannelId;
    private static long         updateTimeout;
    private static long         stealTimeout;

    static {
        Properties properties = new Properties();

        try (FileInputStream config = new FileInputStream("memestealerbot.properties")) {
            properties.load(config);
        } catch (IOException e) {
            LOGGER.error("Mandatory file memestealerbot.properties file not found in working directory, exiting with error", e);
            System.exit(1);
        }
        botToken = properties.getProperty("com.github.cheloide.memestealerbot.bottoken");
        try {
            dumpChannelId = Long.parseLong((String) properties.getProperty("com.github.cheloide.memestealerbot.dumpchannelid"));
            postChannelId = Long.parseLong((String) properties.getProperty("com.github.cheloide.memestealerbot.postchannelid"));
        } catch (NumberFormatException e) {
            LOGGER.error("Property com.github.cheloide.memestealerbot.dumpchannelid or com.github.cheloide.memestealerbot.postchannelid does not contain a valid value, exiting with error", e);
            System.exit(1);
        }
        updateTimeout = Long.parseLong((String) properties.getOrDefault("com.github.cheloide.memestealerbot.updatetimeout", "10000"));
        stealTimeout = Long.parseLong((String) properties.getOrDefault("com.github.cheloide.memestealerbot.stealtimeout", "300"));

        if (StringUtils.isBlank(botToken)) {
            LOGGER.error("Property com.github.cheloide.memestealerbot.bottoken can be blank, exiting with error");
            System.exit(1);

        }
    }

    public static void main(String[] args) {

        TelegramHttpRequestService  httpRequestService = new TelegramHttpRequestServiceImpl(botToken);
        TelegramBotApiUpdateService updateService      = new TelegramBotApiUpdateServiceImpl(httpRequestService, updateTimeout);
        DeferredMediaGroupService   dmgs               = DeferredMediaGroupServiceImpl.getInstance(botToken);
        TelegramBotApiService       botApi             = new TelegramBotApiServiceImpl(httpRequestService);
        updateService.startUpdates();

        ScheduledExecutorService scheduleExecutor = Executors.newSingleThreadScheduledExecutor();
        final ExecutorService    stealerExecutor  = Executors.newCachedThreadPool();

        scheduleExecutor.scheduleAtFixedRate(() -> stealerExecutor.submit(new StealerRunnable(updateService, botApi, dmgs, dumpChannelId, postChannelId)), 0, stealTimeout, TimeUnit.MILLISECONDS);
    }
}
