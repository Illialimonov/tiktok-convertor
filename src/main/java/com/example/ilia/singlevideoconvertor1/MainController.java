package com.example.ilia.singlevideoconvertor1;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String subs = "";
        if (subtitles) subs = subsLogicPre(youtubeUrl, hash);
        fillerVideo = checkIfRandom(fillerVideo);
        List<Integer> fillerTimingsList = new ArrayList<>(timingsList);
        adjustForFiller(fillerTimingsList, fillerVideo);
        int rate = getRateBasedOnRole(role);
        int crf = getCrfBaseOnRole(role);
        int dlStart = Math.max(0, timingsList.get(0) - 10);
        int dlEnd = timingsList.get(1) + 10;

        int ffmpegStart = timingsList.get(0) - dlStart; // should be ~10
        int ffmpegEnd = timingsList.get(1) - dlStart;   // should be (end - start) + 10
        int duration = ffmpegEnd - ffmpegStart;         // safe fallback for `-t` in ffmpeg
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
                        "[yt][filler]vstack=inputs=2[vstacked]; [vstacked]pad=1080:1920:0:176[v]; " +
                        subs +
                        "[0:a]atrim=start=%d:end=%d,asetpts=PTS-STARTPTS[audio]\" " +
                        "-map \"[v]\" -map \"[audio]\" -r %d -t %d -c:v libx264 -profile:v baseline -crf %d -preset ultrafast " +
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
        return "https://storage.googleapis.com/tiktok1234/"+ hash +".mp4";
    }

    private static String subsLogicPre(String youtubeUrl, String hash) throws IOException, InterruptedException {
        String command = String.format(
                "source /home/ilialimits222/yt-dlp-venv/bin/activate && " +
                        "/home/ilialimits222/yt-dlp-venv/bin/yt-dlp -x --audio-format m4a -o '%s' '%s'",
                hash,youtubeUrl
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

        sendToWhisperAPI(hash);

        HttpClient client = HttpClient.newHttpClient();
        String jsonToProcess = new String(Files.readAllBytes(Paths.get("transcription.json")), StandardCharsets.UTF_8);
        String requestBody = "{\"jsonsubs\":" + jsonToProcess + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sub-around-295548041717.us-central1.run.app/convert-json-to-ass"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Path path = Paths.get("subs.ass");
        Files.writeString(path, response.body());

        Files.deleteIfExists(Paths.get("subs.ass"));
        Files.deleteIfExists(Paths.get("transcription.json"));


        return "[padded]ass=subs.ass[v]; ";


    }

    private static void sendToWhisperAPI(String hash) throws IOException, InterruptedException {
        System.out.println("start sending");

        String boundary = "----WhisperBoundary" + UUID.randomUUID();
        String apiKey = "your-api-key-here";  // Replace with your key
        Path audioFilePath = Path.of(hash + ".m4a");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // File part
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + hash + ".m4a\"\r\n");
        out.writeBytes("Content-Type: audio/mpeg\r\n\r\n");
        out.write(Files.readAllBytes(audioFilePath));
        out.writeBytes("\r\n");

        // Model part
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        out.writeBytes("whisper-1\r\n");

        // Response format
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n");
        out.writeBytes("verbose_json\r\n");

        // Granularities
        for (String granularity : List.of("segment", "word")) {
            out.writeBytes("--" + boundary + "\r\n");
            out.writeBytes("Content-Disposition: form-data; name=\"timestamp_granularities[]\"\r\n\r\n");
            out.writeBytes(granularity + "\r\n");
        }

        // End boundary
        out.writeBytes("--" + boundary + "--\r\n");
        out.flush();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/audio/transcriptions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bos.toByteArray()))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());

        Files.writeString(Paths.get("transcription.json"), response.body(), StandardCharsets.UTF_8);
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