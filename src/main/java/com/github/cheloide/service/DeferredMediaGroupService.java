package com.github.cheloide.service;

public interface DeferredMediaGroupService {

    public void addPhoto(long to, String mediaGroupID, String photoId, long timeout);

    public void addVideo(long to, String mediaGroupID, String videoId, long timeout);
}
