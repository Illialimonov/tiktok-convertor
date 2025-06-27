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

    @NotNull
    private String role;

    @NotNull
    private boolean subs;

    // ✅ Default constructor (required for Spring & Jackson)
    public CreateVideoDTO() {
    }

    // ✅ Constructor with parameters
    public CreateVideoDTO(String youtubeUrl, List<Integer> timingsList, String fillerVideo, String role) {
        this.youtubeUrl = youtubeUrl;
        this.timingsList = timingsList;
        this.fillerVideo = fillerVideo;
        this.role = role;
    }

    // ✅ Getters
    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public boolean isSubs() {
        return subs;
    }

    public void setSubs(boolean subs) {
        this.subs = subs;
    }
}