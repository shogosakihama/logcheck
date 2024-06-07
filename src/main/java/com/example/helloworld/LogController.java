package com.example.helloworld;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LogController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @GetMapping("/")
    public String index() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("file1") MultipartFile file1,
                              @RequestParam("file2") MultipartFile file2,
                              Model model) throws IOException {
        List<LogEntry> log1 = readLogEntries(file1);
        List<LogEntry> log2 = readLogEntries(file2);

        List<MergedLogEntry> mergedLogs = mergeLogEntries(log1, log2);

        model.addAttribute("mergedLogs", mergedLogs);
        return "result";
    }

    private List<LogEntry> readLogEntries(MultipartFile file) throws IOException {
        List<LogEntry> logEntries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            LogEntry currentEntry = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} .*")) {
                    if (currentEntry != null) {
                        logEntries.add(currentEntry);
                    }
                    currentEntry = parseLogEntry(line);
                } else if (currentEntry != null) {
                    currentEntry.appendMessage(line);
                }
            }
            if (currentEntry != null) {
                logEntries.add(currentEntry);
            }
        }
        return logEntries;
    }

    private LogEntry parseLogEntry(String line) {
        String[] parts = line.split("\\s+", 4);
        LocalDateTime timestamp = LocalDateTime.parse(parts[0] + " " + parts[1], DATE_TIME_FORMATTER);
        String message = parts[2] + " " + parts[3];
        return new LogEntry(timestamp, message);
    }

    private List<MergedLogEntry> mergeLogEntries(List<LogEntry> log1, List<LogEntry> log2) {
        List<MergedLogEntry> mergedLogs = new ArrayList<>();
        int i = 0, j = 0;

        while (i < log1.size() && j < log2.size()) {
            LogEntry entry1 = log1.get(i);
            LogEntry entry2 = log2.get(j);

            if (entry1.getTimestamp().isBefore(entry2.getTimestamp())) {
                mergedLogs.add(new MergedLogEntry(entry1.getTimestamp(), entry1.getMessage(), ""));
                i++;
            } else if (entry2.getTimestamp().isBefore(entry1.getTimestamp())) {
                mergedLogs.add(new MergedLogEntry(entry2.getTimestamp(), "", entry2.getMessage()));
                j++;
            } else {
                mergedLogs.add(new MergedLogEntry(entry1.getTimestamp(), entry1.getMessage(), entry2.getMessage()));
                i++;
                j++;
            }
        }

        while (i < log1.size()) {
            LogEntry entry1 = log1.get(i);
            mergedLogs.add(new MergedLogEntry(entry1.getTimestamp(), entry1.getMessage(), ""));
            i++;
        }

        while (j < log2.size()) {
            LogEntry entry2 = log2.get(j);
            mergedLogs.add(new MergedLogEntry(entry2.getTimestamp(), "", entry2.getMessage()));
            j++;
        }

        return mergedLogs;
    }

    public static class LogEntry {
        private LocalDateTime timestamp;
        private String message;

        public LogEntry(LocalDateTime timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public void appendMessage(String additionalMessage) {
            this.message += "\n" + additionalMessage;
        }
    }

    public static class MergedLogEntry {
        private LocalDateTime timestamp;
        private String log1Message;
        private String log2Message;

        public MergedLogEntry(LocalDateTime timestamp, String log1Message, String log2Message) {
            this.timestamp = timestamp;
            this.log1Message = log1Message;
            this.log2Message = log2Message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getLog1Message() {
            return log1Message;
        }

        public String getLog2Message() {
            return log2Message;
        }
    }
}
