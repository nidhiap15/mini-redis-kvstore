import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private Store store;

    public ClientHandler(Socket socket, Store store) {
        this.socket = socket;
        this.store = store;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String response = handleCommand(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    private String handleCommand(String line) {
        String[] parts = line.trim().split("\\s+", 3);
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "ERROR: empty command";
        }

        String command = parts[0].toUpperCase();

        switch (command) {
            case "SET":
                if (parts.length < 3) return "ERROR: usage SET key value [EX seconds]";
                String value = parts[2];
                String[] valueAndExpiry = value.split("\\s+EX\\s+");
                if (valueAndExpiry.length == 2) {
                    try {
                        long ttl = Long.parseLong(valueAndExpiry[1].trim());
                        store.setWithExpiry(parts[1], valueAndExpiry[0], ttl);
                    } catch (NumberFormatException e) {
                        return "ERROR: invalid TTL value";
                    }
                } else {
                    store.set(parts[1], value);
                }
                return "OK";

            case "GET":
                if (parts.length < 2) return "ERROR: usage GET key";
                return store.get(parts[1]);

            case "DELETE":
                if (parts.length < 2) return "ERROR: usage DELETE key";
                store.delete(parts[1]);
                return "OK";

            case "KEYS":
                StringBuilder sb = new StringBuilder();
                store.listKeys(sb);
                return sb.toString();

            default:
                return "ERROR: unknown command '" + command + "'";
        }
    }
}