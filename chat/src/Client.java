import com.sun.xml.internal.ws.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created by ericpts on 5/21/17.
 */
public class Client {

    static ObjectOutputStream out;
    static ObjectInputStream in;

    public static void main(String[] args) throws IOException {
        System.out.println("Enter server IP: ");
        Scanner sc = new Scanner(System.in);
        Socket socket = new Socket(sc.next(), Server.port);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        System.out.println("Enter nickname: ");
        String nickname = sc.next();

        out.writeObject(new Messages.RegisterNewClient(nickname));


        Thread serverThread = new Thread(() -> {
            while (true) {
                Object obj = null;
                try {
                    obj = in.readObject();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                if (obj == null)
                    return;


                if (obj instanceof Messages.Message) {
                    Messages.Message msg = (Messages.Message) obj;
                    System.out.format("%s: %s\n", msg.from, msg.text);
                } else if (obj instanceof Messages.AllNicknamesResponse) {
                    Messages.AllNicknamesResponse anr = (Messages.AllNicknamesResponse) obj;
                    System.out.println("Nicknames: ");
                    for (String nick : anr.nicknames) {
                        System.out.print(nick + ", ");
                    }
                    System.out.println();
                }
            }
        });

        Thread consoleThread = new Thread(() -> {
            while (true) {
                String cmd = sc.nextLine();
                final ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(cmd.split(" ")));

                System.err.format("Tokens are: %s\n", tokens.toString());
                if (tokens.get(0).compareTo("/all_nicknames") == 0) {
                    try {
                        out.writeObject(new Messages.AllNicknamesRequest());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (tokens.get(0).compareTo("/broadcast") == 0) {
                    String text = String.join(" ", tokens.subList(1, tokens.size()));
                    try {
                        out.writeObject(new Messages.BroadcastMessage(text));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (tokens.get(0).compareTo("/pm") == 0) {
                    String whom = tokens.get(1);
                    String text = String.join(" ", tokens.subList(2, tokens.size()));
                    try {
                        out.writeObject(new Messages.PrivateMessage(text, whom));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Command not recognized! Try /all_nicknames, /broadcast or /pm");
                }
            }
        });

        serverThread.start();
        consoleThread.start();

        try {
            serverThread.join();
            consoleThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
