package com.example.ilia.singlevideoconvertor1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class httpTest {

    public static String getNextAvailableFormat(String videoUrl) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "bash", "-c",
                    "source /home/ilialimits222/yt-dlp-venv/bin/activate && /home/ilialimits222/yt-dlp-venv/bin/yt-dlp -F \"" + videoUrl + "\""
            );


            builder.redirectErrorStream(true); // merge stderr with stdout
            Process process = builder.start();

            try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("\nError: yt-dlp exited with code ").append(exitCode);
            }

        } catch (IOException | InterruptedException e) {
            output.append("Exception: ").append(e.getMessage());
            Thread.currentThread().interrupt();
        }

        List<Integer> forbiddenFormats = List.of(602, 604, 605, 606, 609, 614, 616, 617);
        String[] out = output.toString().split("\n");
        List<String> change = List.of(out);
        for (int i = 1; i < change.size(); i++) {
            String lastLine = change.get(change.size()-i);
            String lastFormatLine = lastLine.split("   ")[0];
            String trueFormat = lastFormatLine.split(" ")[0];
            System.out.println(trueFormat);
            if(!(forbiddenFormats.contains(Integer.valueOf(trueFormat)))){
                return trueFormat;
            }
        }

        return "100";
    }

    public static String getFormat(String videoUrl) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "bash", "-c",
                    "source /home/ilialimits222/yt-dlp-venv/bin/activate && /home/ilialimits222/yt-dlp-venv/bin/yt-dlp -F \"" + videoUrl + "\""
            );
            builder.redirectErrorStream(true); // merge stderr with stdout
            Process process = builder.start();

            try (
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append("\nError: yt-dlp exited with code ").append(exitCode);
            }

        } catch (IOException | InterruptedException e) {
            output.append("Exception: ").append(e.getMessage());
            Thread.currentThread().interrupt();
        }

        String[] out = output.toString().split("\n");
        List<String> change = List.of(out);
        String lastLine = change.get(change.size()-1);
        String lastFormatLine = lastLine.split("   ")[0];
        return lastFormatLine.split(" ")[0];
    }


}
