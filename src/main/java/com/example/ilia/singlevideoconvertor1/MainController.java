package com.example.ilia.singlevideoconvertor1;


import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        return runCommand(video.getYoutubeUrl(), video.getTimingsList(), video.getFillerVideo(), video.getRole(), video.isSubs());
    }





    public static String runCommand(String youtubeUrl,List<Integer> timingsList,String fillerVideo, String role, boolean subtitles) throws InterruptedException, IOException {
        String hash = generateUniqueHash();
        int dlStart = Math.max(0, timingsList.get(0) - 10);
        int dlEnd = timingsList.get(1) + 10;

        int ffmpegStart = timingsList.get(0) - dlStart; // should be ~10
        int ffmpegEnd = timingsList.get(1) - dlStart;
        int duration = ffmpegEnd - ffmpegStart;

        List<Integer> fillerTimingsList = new ArrayList<>(timingsList);
        adjustForFiller(fillerTimingsList, fillerVideo);// safe fallback for `-t` in ffmpeg

        String subs = "";
        if (subtitles) subs = subsLogicPre(youtubeUrl, hash, dlStart, dlEnd, ffmpegStart, duration, fillerTimingsList);
        String finalVideoLabel = subtitles ? "v" : "padded";
        fillerVideo = checkIfRandom(fillerVideo);

        int rate = getRateBasedOnRole(role);
        int crf = getCrfBaseOnRole(role);

        String format = httpTest.getFormat(youtubeUrl);
        System.out.println("video format ffmpeg = " + format);
        if(format.equals("616") || format.equals("617") || format.equals("614") || format.equals("609") || format.equals("606") || format.equals("605") || format.equals("604")){
            format=httpTest.getNextAvailableFormat(youtubeUrl) +"+bestaudio";

        } else {
            format = "bestvideo[height<=1080]+bestaudio";
        }


        System.out.println("MY RATE  IS " + rate);
        System.out.println("MY RATE  IS " + crf);

        String command = String.format(
                "source /home/ilialimits222/yt-dlp-venv/bin/activate && " +
                        "/home/ilialimits222/yt-dlp-venv/bin/yt-dlp " +
                        "--download-sections \"*%d-%d\" " +
                        "-4 --proxy \"http://user172039:sga9ij@216.74.96.94:4583\" " +
                        "--hls-prefer-ffmpeg " +
                        "--extractor-args \"youtube:po_token=web.main+web\" " +
                        "-f \"%s\" -o - \"%s\" | " +
                        "ffmpeg -thread_queue_size 512 -threads 0 " +
                        "-i pipe:0 -i \"https://storage.googleapis.com/tiktok1234/%s.mp4\" " +
                        "-filter_complex \"[0:v]trim=start=%d:end=%d,setpts=PTS-STARTPTS,scale=1080:-1[yt]; "+
                        "[1:v]trim=start=%d:end=%d,setpts=PTS-STARTPTS,scale=1920:-1,crop=1080:960:420:60[filler]; " +
                        "[yt][filler]vstack=inputs=2[vstacked]; [vstacked]pad=1080:1920:0:176[padded]; " +
                        subs + //"subs "
                        "[0:a]atrim=start=%d:end=%d,asetpts=PTS-STARTPTS[audio]\" " +
                        "-map \"[" + finalVideoLabel + "]\" -map \"[audio]\" -r %d -t %d -c:v libx264 -profile:v baseline -crf %d -preset ultrafast " +
                        "-c:a aac -b:a 192k -movflags frag_keyframe+empty_moov -f mp4 - | " +
                        "gcloud storage cp - gs://tiktok1234/%s.mp4",
                dlStart,
                dlEnd,
                format,
                youtubeUrl,
                fillerVideo,
                ffmpegStart, ffmpegEnd,                          // [0:v] yt trim
                fillerTimingsList.get(0), fillerTimingsList.get(1), // [1:v] filler trim
                ffmpegStart, ffmpegEnd,                          // [0:a] audio trim
                rate,
                duration,
                crf,
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

        process.waitFor();
        System.out.println(Thread.currentThread().getName() + " finished execution.");
        try {
            Path path = Paths.get("subs.ass");
            Files.deleteIfExists(path);
            System.out.println(hash+".m4a deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "https://storage.googleapis.com/tiktok1234/"+ hash +".mp4";
    }

    private static String subsLogicPre(String youtubeUrl, String hash, int dlStart, int dlEnd, int ffmpegStart, int duration, List<Integer> timingList) throws IOException, InterruptedException {
        String command = String.format(
                "source /home/ilialimits222/yt-dlp-venv/bin/activate && " +
                        "/home/ilialimits222/yt-dlp-venv/bin/yt-dlp " +
                        "--download-sections \"*%d-%d\" " +
                        "-4 --proxy \"http://user172039:sga9ij@216.74.96.94:4583\" " +
                        "--hls-prefer-ffmpeg " +
                        "--extractor-args \"youtube:po_token=web.main+web\" " +
                        "-f bestaudio[ext=m4a] -o - \"%s\" | " +
                        "ffmpeg -ss %d -i pipe:0 -t %d -c copy \"%s_trimmed.m4a\"",
                dlStart,                 // 1. yt-dlp padded start
                dlEnd,                   // 2. yt-dlp padded end
                youtubeUrl,              // 3. YouTube video URL
                ffmpegStart,             // 4. how much to skip from pipe
                duration,                // 5. how much audio to extract
                hash                     // 6. output filename prefix
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
        //hi

        sendToWhisperAPI(hash, timingList);

        // Read the large JSON file
        RestTemplate restTemplate = new RestTemplate();

// Allow large payloads (set streaming)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        restTemplate.setRequestFactory(factory);

// Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

// Build multipart request
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource("transcription.json")); // 👈 upload as a file

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

// Send request
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://sub-around-295548041717.us-central1.run.app/convert-json-to-ass",
                requestEntity,
                String.class
        );

// Save response
        Files.writeString(Paths.get("subs.ass"), response.getBody(), StandardCharsets.UTF_8);
        System.out.println("Subtitles saved to subs.ass");

        try {
            Path path = Paths.get("transcription.json");
            Files.deleteIfExists(path);
            System.out.println("transcription deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Path path = Paths.get(hash+".m4a");
            Files.deleteIfExists(path);
            System.out.println(hash+".m4a deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "[padded]subtitles=subs.ass[v]; ";


    }

    private static void sendToWhisperAPI(String hash, List<Integer> timingList) throws IOException, InterruptedException {
        int startPoint = (timingList.get(0) > 10) ? timingList.get(0)-10 : timingList.get(0);
        int endPoint = (timingList.get(1)-10);
        String outputName = hash+"_chopped";
        int duration = endPoint - startPoint;
        String command = String.format(
                "ffmpeg -i \"%s.m4a\" -ss %d -t %d -c copy \"%s.m4a\"",
                hash, startPoint, duration, outputName
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

        System.out.println("start sending");

        String filePath = hash+"_chopped.m4a"; // replace with your file path
        String apiKey = "Bearer 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"; // replace with your key

        FileSystemResource audioFile = new FileSystemResource(new File(filePath));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", audioFile);
        body.add("model", "whisper-1");
        body.add("response_format", "verbose_json");
        body.add("timestamp_granularities[]", "word");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", apiKey);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com/v1/audio/transcriptions";

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        System.out.println("Status code: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());

        if (response.getStatusCode().is2xxSuccessful()) {
            Files.write(Paths.get("transcription.json"), response.getBody().getBytes());
            System.out.println("Working directory: " + new File(".").getAbsolutePath());
            System.out.println("Saved to transcription.json");
        } else {
            System.out.println("Request failed: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
        }

    }



    private static int getCrfBaseOnRole(String role) {
        if (role.equals("PREMIUM")) return 21;
        if (role.equals("PRO")) return 23;
        return 28;
    }

    private static int getRateBasedOnRole(String role) {
        if (role.equals("PREMIUM") || role.equals("PRO")) return 60;
        return 30;
    }

    private static int getQualityBasedOnRole(String role) {
        if (role.equals("PREMIUM")) return 1080;
        if (role.equals("PRO")) return 720;
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