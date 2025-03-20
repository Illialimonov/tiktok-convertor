package com.example.ilia.singlevideoconvertor1;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

@RestController
public class MainController {
    private static final SecureRandom random = new SecureRandom();

    @GetMapping("/debug/cookies")
    public ResponseEntity<String> checkCookies() {
        File file = new File("/app/cookies_new.txt");
        if (file.exists()) {
            return ResponseEntity.ok("cookies_new.txt is present!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("cookies_new.txt NOT found!");
        }
    }


    @PostMapping("/create")
    public String getVideo(@RequestBody CreateVideoDTO video) {
        System.out.println(video.getYoutubeUrl());
        return runCommand(video.getYoutubeUrl(), video.getTimingsList(), video.getFillerVideo());
    }


    public static String runCommand(String youtubeUrl,List<Integer> timingsList,String fillerVideo) {
        List<Integer> fillerTimingsList = new ArrayList<>(timingsList);
        adjustForFiller(fillerTimingsList, fillerVideo);
        String hash = generateUniqueHash();

        try {
            String command = String.format(
                     // Upload cookies before execution
                            "yt-dlp --proxy \"https://customer-lymonov_RgfaH-sessid-0050345537-sesstime-10:Ilialimonov05+@pr.oxylabs.io:7777\" -f bestvideo+bestaudio -o - \"%s\" | " +
                            "ffmpeg -i pipe:0 -i \"https://storage.googleapis.com/tiktok1234/%s.mp4\" " +
                            "-filter_complex \"[0:v]trim=start=%s:end=%s,setpts=PTS-STARTPTS,scale=1080:-1[yt]; " +
                            "[1:v]trim=start=%s:end=%s,setpts=PTS-STARTPTS,scale=1920:-1,crop=1080:960:420:60[filler]; " +
                            "[yt][filler]vstack=inputs=2[vstacked]; [vstacked]pad=1080:1920:0:(1920-1568)/2[v]; " +
                            "[0:a]atrim=start=%s:end=%s,asetpts=PTS-STARTPTS[audio]\" " +
                            "-map \"[v]\" -map \"[audio]\" -r 30 -t %s -c:v libx264 -profile:v baseline -crf 23 -preset fast " +
                            "-c:a aac -b:a 192k -movflags frag_keyframe+empty_moov -f mp4 - | " +
                            "gcloud storage cp - gs://tiktok1234/%s_%d.mp4",
                    youtubeUrl,
                    fillerVideo,
                    timingsList.get(0), timingsList.get(1),  // First trim
                    fillerTimingsList.get(0), fillerTimingsList.get(1),  // Second trim
                    timingsList.get(0), timingsList.get(1),  // Audio trim
                    (timingsList.get(1) - timingsList.get(0)),  // Duration for -t
                    hash, 1
            );






            ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(Thread.currentThread().getName() + ": " + line);
                }
            }

            process.waitFor();
            System.out.println(Thread.currentThread().getName() + " finished execution.");
            return "https://storage.googleapis.com/tiktok1234/"+ hash + "_"+ 1 +".mp4";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void adjustForFiller(List<Integer> timings, String fillerVideo) {
        if (timings.size() < 2) return; // Safety check

        HashMap<String, Integer> fillerTimings = getCurrentFillerTimings();

        if(fillerTimings.get(fillerVideo)<= timings.get(1) && timings.get(0) >= 60){
            while (timings.get(1) > fillerTimings.get(fillerVideo) && timings.get(0) >= 60) {
                timings.set(0, timings.get(0) - 60);
                timings.set(1, timings.get(1) - 60);
            }
        }
    }

    private static HashMap<String, Integer> getCurrentFillerTimings() {
        HashMap<String, Integer> timingsMap = new HashMap<>();

        try {
            URL url = new URL("https://storage.googleapis.com/tiktok1234/fillerTimings.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line.toString());
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    Integer value = Integer.parseInt(parts[1].trim());
                    timingsMap.put(key, value);
                }
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return timingsMap;
    }
    public static String generateUniqueHash() {
        byte[] bytes = new byte[9]; // 9 bytes ≈ 12 Base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, 12);
    }


}

