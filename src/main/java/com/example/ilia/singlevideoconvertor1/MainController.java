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
import java.util.*;

@RestController
public class MainController {
    private static final SecureRandom random = new SecureRandom();

    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        try {
            return ResponseEntity.ok("?Application is running");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Health check failed: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public String getVideo(@RequestBody CreateVideoDTO video) throws IOException, InterruptedException {
        System.out.println(video.getYoutubeUrl());
        return runCommand(video.getYoutubeUrl(), video.getTimingsList(), video.getFillerVideo(), video.getRole());
    }

    @PostMapping("/picture")
    public String pic(@RequestBody PictureDTO pic) throws IOException, InterruptedException {
        System.out.println(pic.getVideoURL());
        return getPic(pic.getVideoURL());
    }

    private String getPic(String videoURL) throws IOException {
        String hash = generateUniqueHash();
        String command = String.format("ffmpeg -i \"%s\"" +
                " -frames:v 1 -q:v 2 -f image2pipe -vcodec png - | gsutil cp - gs://tiktok1234/%s.png"
                ,videoURL,
                hash
                );
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(Thread.currentThread().getName() + ": " + line);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return "https://storage.googleapis.com/tiktok1234/"+ hash +".png";

    }


    public static String runCommand(String youtubeUrl,List<Integer> timingsList,String fillerVideo, String role) throws InterruptedException, IOException {
        fillerVideo = checkIfRandom(fillerVideo);
        List<Integer> fillerTimingsList = new ArrayList<>(timingsList);
        adjustForFiller(fillerTimingsList, fillerVideo);
        String hash = generateUniqueHash();
        int quality = getQualityBasedOnRole(role);
        int rate = getRateBasedOnRole(role);
        int crf = getCrfBaseOnRole(role);

        String command = String.format(
                "source /home/ilialimits222/yt-dlp-venv/bin/activate && " +
                        "/home/ilialimits222/yt-dlp-venv/bin/yt-dlp " +
                        "-4 --proxy \"https://customer-lymonov_RgfaH-sessid-0050345537-sesstime-10:Ilialimonov05+@pr.oxylabs.io:7777\" " +
                        "--hls-prefer-ffmpeg " +
                        "--extractor-args \"youtube:po_token=web.main+web\" " +
                        "-f \"bestvideo[height<=%d]+bestaudio\" -o - \"%s\" | " +
                        "ffmpeg " + // <-- add space here!
                        "-i pipe:0 -i \"https://storage.googleapis.com/tiktok1234/%s.mp4\" " +
                        "-filter_complex \"[0:v]trim=start=%s:end=%s,setpts=PTS-STARTPTS,scale=1080:-1[yt]; " +
                        "[1:v]trim=start=%s:end=%s,setpts=PTS-STARTPTS,scale=1920:-1,crop=1080:960:420:60[filler]; " +
                        "[yt][filler]vstack=inputs=2[vstacked]; [vstacked]pad=1080:1920:0:176[v]; " +
                        "[0:a]atrim=start=%s:end=%s,asetpts=PTS-STARTPTS[audio]\" " +
                        "-map \"[v]\" -map \"[audio]\" -r %d -t %s -c:v libx264 -profile:v baseline -crf %d -preset ultrafast " +
                        "-c:a aac -b:a 192k -movflags frag_keyframe+empty_moov -f mp4 - | " +
                        "gcloud storage cp - gs://tiktok1234/%s.mp4",
                quality,
                youtubeUrl,
                fillerVideo,
                timingsList.get(0), timingsList.get(1),
                fillerTimingsList.get(0), fillerTimingsList.get(1),
                timingsList.get(0), timingsList.get(1),
                rate,
                (timingsList.get(1) - timingsList.get(0)),
                crf,
                hash
        );

        System.out.println(command);






        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(Thread.currentThread().getName() + ": " + line);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        process.waitFor();
        System.out.println(Thread.currentThread().getName() + " finished execution.");
        return "https://storage.googleapis.com/tiktok1234/"+ hash +".mp4";
    }

    private static int getCrfBaseOnRole(String role) {
        if (role.equals("PREMIUM")) return 23;
        if (role.equals("PRO")) return 21;
        return 28;
    }

    private static int getRateBasedOnRole(String role) {
        if (role.equals("PREMIUM") || role.equals("PRO")) return 60;
        return 30;
    }

    private static int getQualityBasedOnRole(String role) {
        if (role.equals("PREMIUM")) return 720;
        if (role.equals("PRO")) return 1080;
        return 480;
    }

    private static String checkIfRandom(String fillerVideo) {
        ArrayList<String> change = new ArrayList<>();
        change.add("bmx");
        change.add("gta5");
        change.add("minecraft");
        change.add("truck_jumping");
        change.add("press");
        if(fillerVideo.equals("random")){
            Random random = new Random();
            fillerVideo = change.get(random.nextInt(change.size()));
        }
        return fillerVideo;
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