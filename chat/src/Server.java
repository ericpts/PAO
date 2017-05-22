import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ericpts on 5/21/17.
 */

public class Server {
    static final int port = 9090;

    static ConcurrentHashMap<String, ClientConnection> clients;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        clients = new ConcurrentHashMap<>();

        ServerSocket listener = new ServerSocket(port);
        System.err.format("Server up and listening on %s:%d\n", listener.getInetAddress().toString(), listener.getLocalPort());
        while (true) {
            Socket socket = listener.accept();
            if (socket == null)
                continue;
            ClientConnection cc = new ClientConnection(socket);;

            clients.put(cc.getNickname(), cc);
            cc.start();
        }
    }

    private static class ClientConnection extends Thread {
        private String nickname;
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        ClientConnection(Socket socket) throws IOException, ClassNotFoundException {
            System.err.format("Creating connection to %s\n", socket.getLocalAddress().toString());
            this.socket = socket;
            this.in = new ObjectInputStream(socket.getInputStream());
            this.out = new ObjectOutputStream(socket.getOutputStream());

            Messages.RegisterNewClient rnc = null;
            while (rnc == null)
                rnc = (Messages.RegisterNewClient) this.in.readObject();
            this.nickname = rnc.nickname;
            System.err.format("%s registered as %s\n", socket.getLocalAddress().toString(), this.getNickname());
        }

        private void handleMessage(Object obj) {
            if (obj instanceof Messages.PrivateMessage) {
                Messages.PrivateMessage pm = (Messages.PrivateMessage) obj;
                System.err.format("Routing private message from %s to %s with text: %s\n", this.getNickname(), pm.to, pm.text);
                ClientConnection to = clients.get(pm.to);
                try {
                    to.write(new Messages.Message(pm.text, this.getNickname()));
                } catch (IOException e) {
                    System.err.format("Could not write to %s: %s\n", to.getNickname(), e.getMessage());
                    e.printStackTrace();
                }
            }
            else if (obj instanceof Messages.BroadcastMessage) {
                Messages.BroadcastMessage bm = (Messages.BroadcastMessage) obj;
                System.err.format("Routing broadcast message from %s with text: %s\n", this.getNickname(), bm.text);
                for (ClientConnection cc: clients.values()) {
                    try {
                        cc.write(new Messages.Message(bm.text, this.getNickname()));
                    } catch (IOException e) {
                        System.err.format("Could not write to %s: %s\n", cc.getNickname(), e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            else if (obj instanceof Messages.AllNicknamesRequest) {
                LinkedList<String> nicknames = new LinkedList<>();
                for (ClientConnection cc: clients.values()) {
                    nicknames.add(cc.getNickname());
                }
                System.err.format("Received a request for all nicknames from %s. Response is %s\n", this.getNickname(), nicknames.toString());
                try {
                    this.write(new Messages.AllNicknamesResponse(nicknames));
                } catch (IOException e) {
                    System.err.format("Could not write to %s: %s\n", this.getNickname(), e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        public void write(Object obj) throws IOException {
            synchronized (this) {
                this.out.writeObject(obj);
            }
        }

        public void run() {
            while (true) {
                Object obj = null;
                try {
                    obj = in.readObject();
                } catch (IOException e) {
                    System.out.format("%s closed the connection.\n", this.getNickname());
                    this.close();
                    return;
                } catch (ClassNotFoundException e) {
                    System.err.format("Class not found in message received from %s\n", this.getNickname());
                    e.printStackTrace();
                }

                if (obj == null)
                    return;

                handleMessage(obj);
            }
        }

        public void close() {
            synchronized (this) {
                clients.remove(this.getNickname());
                try {
                    this.socket.close();
                } catch (IOException e) {
                    // Nothing!
                }
            }
        }

        public String getNickname() {
            return this.nickname;
        }
    }

}
