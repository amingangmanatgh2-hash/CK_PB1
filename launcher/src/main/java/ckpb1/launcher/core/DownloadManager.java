package ckpb1.launcher.core;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CK_PB1 launcher download manager: parallel downloads with progress, speed,
 * pause/resume (HTTP range when supported), cancel, retry and sha1 checks.
 */
public final class DownloadManager {

    public enum Status {
        QUEUED, RUNNING, PAUSED, DONE, FAILED, CANCELLED
    }

    public static final class Task {
        public final String id = UUID.randomUUID().toString().substring(0, 8);
        public final String name;
        public final String url;
        public final Path target;
        public final String sha1;
        public final long size;
        public final AtomicLong downloaded = new AtomicLong();
        public volatile Status status = Status.QUEUED;
        public volatile String error = "";
        public volatile long speed; // bytes/s
        public volatile boolean resumeSupported;

        Task(String name, String url, Path target, String sha1, long size) {
            this.name = name;
            this.url = url;
            this.target = target;
            this.sha1 = sha1;
            this.size = size;
        }

        public int progressPercent() {
            long s = size > 0 ? size : downloaded.get();
            if (s <= 0) {
                return status == Status.DONE ? 100 : 0;
            }
            return (int) Math.min(100, downloaded.get() * 100 / s);
        }
    }

    public static final class Model extends AbstractTableModel {
        private final String[] columns = {"File", "Progress", "Size", "Speed", "Status"};
        public final List<Task> tasks = new ArrayList<>();

        void add(Task t) {
            SwingUtilities.invokeLater(() -> {
                tasks.add(t);
                int row = tasks.size() - 1;
                fireTableRowsInserted(row, row);
            });
        }

        void updated(Task t) {
            SwingUtilities.invokeLater(() -> {
                int row = tasks.indexOf(t);
                if (row >= 0) {
                    fireTableRowsUpdated(row, row);
                }
            });
        }

        @Override
        public int getRowCount() {
            return tasks.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int col) {
            return columns[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            Task t = tasks.get(row);
            return switch (col) {
                case 0 -> t.name;
                case 1 -> t.progressPercent();
                case 2 -> human(t.size > 0 ? t.size : t.downloaded.get());
                case 3 -> t.status == Status.RUNNING ? human(t.speed) + "/s" : "";
                case 4 -> t.status + (t.error.isEmpty() ? "" : " - " + t.error);
                default -> "";
            };
        }

        public Task taskAt(int row) {
            return tasks.get(row);
        }
    }

    private final Model model = new Model();
    private final ExecutorService executor;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public DownloadManager(int threads) {
        this.executor = Executors.newFixedThreadPool(Math.max(1, threads));
    }

    public Model model() {
        return model;
    }

    /** Enqueues a download and returns its task immediately. */
    public Task enqueue(String name, String url, Path target, String sha1, long size) {
        Task task = new Task(name, url, target, sha1, size);
        model.add(task);
        executor.submit(() -> run(task));
        return task;
    }

    public void retry(Task task) {
        if (task.status == Status.FAILED || task.status == Status.CANCELLED || task.status == Status.PAUSED) {
            task.status = Status.QUEUED;
            task.error = "";
            model.updated(task);
            executor.submit(() -> run(task));
        }
    }

    public void cancel(Task task) {
        if (task.status == Status.QUEUED || task.status == Status.RUNNING || task.status == Status.PAUSED) {
            task.status = Status.CANCELLED;
            model.updated(task);
        }
    }

    private void run(Task task) {
        if (task.status == Status.CANCELLED) {
            return;
        }
        task.status = Status.RUNNING;
        model.updated(task);
        try {
            Files.createDirectories(task.target.getParent());
            long offset = 0;
            if (Files.exists(task.target) && task.resumeSupported) {
                offset = Files.size(task.target);
            }
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(task.url))
                    .timeout(Duration.ofMinutes(10))
                    .header("User-Agent", "CK_PB1-Launcher/" + ckpb1.common.CKPB1.VERSION)
                    .GET();
            if (offset > 0) {
                request.header("Range", "bytes=" + offset + "-");
            }
            HttpResponse<InputStream> response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode());
            }
            boolean resumed = response.statusCode() == 206;
            if (!resumed) {
                offset = 0; // server ignored range; restart
            }
            task.resumeSupported = response.headers().firstValue("Accept-Ranges").orElse("")
                    .equalsIgnoreCase("bytes") || resumed;
            long finalOffset = offset;
            task.downloaded.set(finalOffset);
            MessageDigest digest = task.sha1 != null ? MessageDigest.getInstance("SHA-1") : null;

            try (InputStream in = response.body();
                 OutputStream out = openOutput(task.target, resumed)) {
                byte[] buffer = new byte[8192];
                long lastTime = System.currentTimeMillis();
                long lastBytes = finalOffset;
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    if (task.status == Status.CANCELLED || task.status == Status.PAUSED) {
                        out.flush();
                        model.updated(task);
                        return;
                    }
                    out.write(buffer, 0, read);
                    if (digest != null) {
                        digest.update(buffer, 0, read);
                    }
                    long now = task.downloaded.addAndGet(read);
                    long t = System.currentTimeMillis();
                    if (t - lastTime >= 400) {
                        task.speed = (now - lastBytes) * 1000 / (t - lastTime);
                        lastTime = t;
                        lastBytes = now;
                        model.updated(task);
                    }
                }
                out.flush();
                if (digest != null && task.sha1 != null) {
                    String actual = hex(digest.digest());
                    if (!actual.equalsIgnoreCase(task.sha1)) {
                        Files.deleteIfExists(task.target);
                        throw new IOException("sha1 mismatch");
                    }
                }
            }
            task.speed = 0;
            task.status = Status.DONE;
            model.updated(task);
        } catch (Exception e) {
            task.speed = 0;
            task.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            task.status = Status.FAILED;
            model.updated(task);
        }
    }

    /** Waits (blocking) until all given tasks are finished or failed. */
    public static boolean awaitAll(List<Task> tasks) throws InterruptedException {
        boolean allOk = true;
        while (true) {
            boolean anyRunning = false;
            for (Task t : tasks) {
                if (t.status == Status.QUEUED || t.status == Status.RUNNING || t.status == Status.PAUSED) {
                    anyRunning = true;
                }
                if (t.status == Status.FAILED) {
                    allOk = false;
                }
            }
            if (!anyRunning) {
                return allOk;
            }
            Thread.sleep(120);
        }
    }

    private static OutputStream openOutput(Path target, boolean append) throws IOException {
        java.util.List<StandardOpenOption> options = new ArrayList<>();
        if (append) {
            options.add(StandardOpenOption.APPEND);
        } else {
            options.add(StandardOpenOption.CREATE);
            options.add(StandardOpenOption.TRUNCATE_EXISTING);
        }
        options.add(StandardOpenOption.WRITE);
        return Files.newOutputStream(target, options.toArray(new StandardOpenOption[0]));
    }

    public static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024);
        return String.format("%.2f GB", bytes / 1024.0 / 1024 / 1024);
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
