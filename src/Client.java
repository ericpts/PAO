import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by ericpts on 5/22/17.
 */
public class Client {

    /*
    Operations:
    reserve 1 2 3 4 <seats>
    empty_seats
    query_reservation <reservation_id>
     */

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: prog <server_ip>");
            return;
        }

        String IP = args[0];
        Socket socket = null;
        try {
            socket = new Socket(IP, Server.port);
        } catch (IOException e) {
            System.err.println("Could not start server!");
            System.err.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.format("Connected to server! Running under %s.\n", socket.getLocalAddress().toString());

        ClientOps opsDispatcher = new ClientOps(socket);
        System.err.println("Here!");

        Scanner sc = new Scanner(System.in);
        while (true) {
            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(sc.nextLine().split(" ")));
            System.err.format("Tokens: %s\n", tokens.toString());
            if (tokens.get(0).equals("reserve")) {
                List<Integer> seats =
                        tokens.subList(1, tokens.size())
                                .stream()
                                .map(s -> Integer.valueOf(s))
                                .collect(Collectors.toList());

                Operations.ReservationRequest req = new Operations.ReservationRequest(seats);
                Operations.ReservationResponse resp = opsDispatcher.reserve(req);
                if (!resp.successful) {
                    System.out.println("Could not reserve!");
                } else {
                    System.out.format("Reserved under id %d", resp.reservartionId);
                }
            } else if (tokens.get(0).equals("empty_seats")) {
                Operations.EmptySeatsResponse resp = opsDispatcher.emptySeats(new Operations.EmptySeatsRequest());
                System.out.format("Empty seats: %s\n", resp.emptySeats.toString());
            } else if (tokens.get(0).equals("query_reservation")) {
                Integer id = Integer.valueOf(tokens.get(1));
                Operations.ReservationDetailsResponse resp = opsDispatcher.reservationDetails(new Operations.ReservationDetailsRequest(id));
                System.out.format("Reservation %d corresponds to %s\n", id, resp.seats.toString());
            } else {
                System.out.println("Invalid operation! Try reserve <seats> OR empty_seats OR query_reservation <id>!\n");
            }
        }
    }

    private static class ClientOps implements Operations{
        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;

        ClientOps(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
            this.in = new ObjectInputStream(this.socket.getInputStream());
        }

        @Override
        public synchronized ReservationResponse reserve(ReservationRequest req) {
            try {
                out.writeObject(req);
                return (ReservationResponse) in.readObject();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public synchronized EmptySeatsResponse emptySeats(EmptySeatsRequest req) {
            try {
                out.writeObject(req);
                return (EmptySeatsResponse) in.readObject();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public synchronized ReservationDetailsResponse reservationDetails(ReservationDetailsRequest req) {
            try {
                out.writeObject(req);
                return (ReservationDetailsResponse) in.readObject();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }
}
