import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = 6379;
        Store store = new Store();

        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    store.purgeExpired();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleaner.setDaemon(true);
        cleaner.start();

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket.getInetAddress());

            ClientHandler handler = new ClientHandler(clientSocket, store);
            new Thread(handler).start();
        }
    }
}