package com.example.ilia.singlevideoconvertor1;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateVideoDTO {
    @NotNull
    private String youtubeUrl;

    @NotNull
    private List<Integer> timingsList;

    @NotNull
    private String fillerVideo;

    // ✅ Default constructor (required for Spring & Jackson)
    public CreateVideoDTO() {
    }

    // ✅ Constructor with parameters
    public CreateVideoDTO(String youtubeUrl, List<Integer> timingsList, String fillerVideo) {
        this.youtubeUrl = youtubeUrl;
        this.timingsList = timingsList;
        this.fillerVideo = fillerVideo;
    }

    // ✅ Getters
    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public List<Integer> getTimingsList() {
        return timingsList;
    }

    public String getFillerVideo() {
        return fillerVideo;
    }

    // ✅ Setters
    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public void setTimingsList(List<Integer> timingsList) {
        this.timingsList = timingsList;
    }

    public void setFillerVideo(String fillerVideo) {
        this.fillerVideo = fillerVideo;
    }
}