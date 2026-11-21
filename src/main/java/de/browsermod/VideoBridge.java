package de.browsermod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Spielt Videos ab, die Chromium selbst nicht kann (H.264: Twitch, TikTok, ...).
 *
 * Ablauf: yt-dlp holt den Stream, ffmpeg wandelt ihn live in WebM (VP8 + Opus) um, ein winziger
 * lokaler Server (nur 127.0.0.1) reicht das Ergebnis an eine Seite mit einem Video-Element weiter,
 * die dann im Browser-Bildschirm der Mod geladen wird.
 *
 * Voraussetzung: yt-dlp und ffmpeg sind installiert und im PATH.
 */
public final class VideoBridge {
    private static ServerSocket server;
    private static volatile String targetUrl = "";
    private static final List<Process> RUNNING = new ArrayList<>();

    private VideoBridge() {
    }

    /** Startet bei Bedarf den lokalen Server und liefert die Adresse der Abspiel-Seite. */
    public static synchronized String playerUrl(String pageUrl) throws IOException {
        targetUrl = pageUrl;
        if (server == null || server.isClosed()) {
            server = new ServerSocket(0, 20, InetAddress.getLoopbackAddress());
            Thread t = new Thread(VideoBridge::acceptLoop, "browsermod-video-server");
            t.setDaemon(true);
            t.start();
            Runtime.getRuntime().addShutdownHook(new Thread(VideoBridge::stopStream));
        }
        return "http://127.0.0.1:" + server.getLocalPort() + "/player?t=" + System.nanoTime();
    }

    private static void acceptLoop() {
        while (true) {
            try {
                Socket s = server.accept();
                Thread t = new Thread(() -> handle(s), "browsermod-video-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private static void handle(Socket s) {
        try (s) {
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            String path = readRequestPath(in);
            if (path.startsWith("/stream")) {
                stream(out);
            } else {
                byte[] body = playerPage().getBytes(StandardCharsets.UTF_8);
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n"
                        + "Content-Length: " + body.length + "\r\nConnection: close\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                out.write(body);
                out.flush();
            }
        } catch (IOException ignored) {
            // Browser hat die Verbindung geschlossen (z. B. Seite verlassen)
        }
    }

    private static String readRequestPath(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        int last4 = 0;
        while ((c = in.read()) != -1 && sb.length() < 16384) {
            sb.append((char) c);
            last4 = (last4 << 8) | c;
            if (last4 == 0x0D0A0D0A) break; // Ende der Header
        }
        String first = sb.toString().split("\r\n", 2)[0]; // "GET /pfad HTTP/1.1"
        String[] parts = first.split(" ");
        return parts.length >= 2 ? parts[1] : "/";
    }

    private static void stream(OutputStream out) throws IOException {
        List<Process> ps;
        try {
            ps = startPipeline(targetUrl);
        } catch (IOException e) {
            out.write(("HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n"
                    + "Connection: close\r\n\r\nyt-dlp oder ffmpeg wurde nicht gefunden.").getBytes(StandardCharsets.US_ASCII));
            return;
        }
        Process ffmpeg = ps.get(ps.size() - 1);
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: video/webm\r\nCache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        try (InputStream video = ffmpeg.getInputStream()) {
            byte[] buf = new byte[32 * 1024];
            int n;
            while ((n = video.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } finally {
            for (Process p : ps) p.destroy();
        }
    }

    private static synchronized List<Process> startPipeline(String url) throws IOException {
        stopStream();
        ProcessBuilder ytdlp = new ProcessBuilder("yt-dlp", "--no-playlist", "-q",
                "-f", "best[height<=720]/best", "-o", "-", url)
                .redirectError(ProcessBuilder.Redirect.DISCARD);
        ProcessBuilder ffmpeg = new ProcessBuilder("ffmpeg", "-loglevel", "error", "-i", "pipe:0",
                "-c:v", "libvpx", "-deadline", "realtime", "-cpu-used", "8", "-b:v", "3M",
                "-c:a", "libopus", "-f", "webm", "pipe:1")
                .redirectError(ProcessBuilder.Redirect.DISCARD);
        List<Process> ps = ProcessBuilder.startPipeline(List.of(ytdlp, ffmpeg));
        RUNNING.addAll(ps);
        return ps;
    }

    private static synchronized void stopStream() {
        for (Process p : RUNNING) p.destroy();
        RUNNING.clear();
    }

    private static String playerPage() {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>Video</title><style>"
                + "html,body{margin:0;height:100%;background:#000;color:#ddd;font-family:sans-serif}"
                + "video{width:100%;height:100%;background:#000}"
                + "#msg{position:fixed;top:12px;left:0;right:0;text-align:center;font-size:18px;pointer-events:none}"
                + "</style></head><body><div id=\"msg\">Video wird geladen ... (das dauert ein paar Sekunden)</div>"
                + "<video id=\"v\" src=\"/stream\" autoplay controls></video><script>"
                + "var v=document.getElementById('v'),m=document.getElementById('msg');"
                + "v.addEventListener('playing',function(){m.style.display='none'});"
                + "v.addEventListener('error',function(){m.style.display='block';"
                + "m.innerHTML='Das Video konnte nicht geladen werden.<br>Sind yt-dlp und ffmpeg installiert? Gibt es das Video / den Stream?'});"
                + "</script></body></html>";
    }
}
