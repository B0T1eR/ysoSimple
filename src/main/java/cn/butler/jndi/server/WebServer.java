package cn.butler.jndi.server;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer implements Runnable {
    public String ip;
    public int port;
    private HttpServer httpServer;
    private static WebServer INSTANCE;
    public static WebServer getInstance() {
        return INSTANCE;
    }
    public WebServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void run() {
        System.out.println("[*] [HTTP-Server] Start Listening on " + ip + ":" + port);
        try {
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            httpServer.start();
            if (INSTANCE == null) {
                INSTANCE = this;
            } else {
                throw new RuntimeException("WebServer has already been started");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void serveFile(String path, byte[] data) {
        httpServer.createContext(path, exchange -> {
            System.out.println("[*] [HTTP-Server] Start Receive request: " + exchange.getRequestURI());
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
    }

    public void serveMessage(String path, String message) {
        httpServer.createContext(path, exchange -> {
            System.out.println("[*] [HTTP-Server] Start Receive request: " + exchange.getRequestURI());
            byte[] response = message.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        });
    }
}
