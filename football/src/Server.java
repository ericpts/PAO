import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by ericpts on 5/22/17.
 */
public class Server {

    final static int port = 9090;

    static HashSet<Integer> freeSeats = new HashSet<>();
    static ConcurrentHashMap<Integer, Operations.ReservationRequest> reservations = new ConcurrentHashMap<>();
    static AtomicInteger reservationId = new AtomicInteger(0);

    private static void init(int n) {
        for (int i = 1; i <= n; ++i) {
            freeSeats.add(i);
        }
    }

    public static void main(String[] args) {
        init(100);

        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not start up server!");
            e.printStackTrace();
            return;
        }

        System.out.format("Server up and listening on %s:%d\n", socket.getInetAddress().toString(), socket.getLocalPort());

        ServerOps opsDispatcher = new ServerOps();

        while (true) {
            Socket client = null;
            try {
                client = socket.accept();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                continue;
            }

            Socket finalClient = client;
            Thread t = new Thread(() -> {
                ObjectInputStream in = null;
                ObjectOutputStream out = null;
                try {
                    System.out.format("Accepted connection from %s\n", finalClient.getLocalAddress().toString());
                    in = new ObjectInputStream(finalClient.getInputStream());
                    out = new ObjectOutputStream(finalClient.getOutputStream());

                    while(true) {
                        Object obj = in.readObject();

                        if (obj instanceof Operations.ReservationRequest) {
                            Operations.ReservationRequest req = (Operations.ReservationRequest) obj;
                            System.out.format("Client %s wants to reserve %s\n", finalClient.getLocalAddress().toString(), req.seats.toString());
                            Operations.ReservationResponse resp = opsDispatcher.reserve(req);
                            out.writeObject(resp);
                        } else if (obj instanceof Operations.EmptySeatsRequest) {
                            Operations.EmptySeatsRequest req = (Operations.EmptySeatsRequest) obj;
                            System.out.format("Client %s wants to know the empty seats\n", finalClient.getLocalAddress().toString());
                            Operations.EmptySeatsResponse resp = opsDispatcher.emptySeats(req);
                            out.writeObject(resp);
                        } else if (obj instanceof Operations.ReservationDetailsRequest) {
                            Operations.ReservationDetailsRequest req = (Operations.ReservationDetailsRequest) obj;
                            System.out.format("Client %s wants to know about reservation %d\n", finalClient.getLocalAddress().toString(), req.reservationId);
                            Operations.ReservationDetailsResponse resp = opsDispatcher.reservationDetails(req);
                            out.writeObject(resp);
                        } else {
                            System.err.format("Invalid request from %s!\n", finalClient.getLocalAddress().toString());
                        }
                    }

                } catch (Exception e) {
                    // Nothing!
                }

                try {
                    if (out != null)
                        out.close();
                    if (in != null)
                        in.close();
                    finalClient.close();
                } catch (Exception e) {
                    // Nothing!.
                }
            });
            t.start();
        }
    }

    public static Operations.ReservationResponse reserveImpl(Operations.ReservationRequest req) {
        synchronized (freeSeats) {
            for (Integer seat: req.seats) {
                if (!freeSeats.contains(seat)) {
                    return Operations.ReservationResponse.failed();
                }
            }
            for (Integer seat: req.seats)
                freeSeats.remove(seat);
        }

        int id = reservationId.getAndIncrement();
        reservations.put(id, req);
        return Operations.ReservationResponse.success(id);
    }
    public static Operations.EmptySeatsResponse emptySeatsImpl(Operations.EmptySeatsRequest req) {
        List<Integer> seats = new LinkedList<>();
        synchronized (freeSeats) {
            for (Integer seat: freeSeats) {
                seats.add(seat);
            }
        }
        return new Operations.EmptySeatsResponse(seats);
    }
    public static Operations.ReservationDetailsResponse reservationDetailsImpl(Operations.ReservationDetailsRequest req) {
        return new Operations.ReservationDetailsResponse(reservations.get(req.reservationId).seats);
    }

    public static class ServerOps implements Operations {
        @Override
        public ReservationResponse reserve(ReservationRequest req) {
            return reserveImpl(req);
        }

        @Override
        public EmptySeatsResponse emptySeats(EmptySeatsRequest req) {
            return emptySeatsImpl(req);
        }

        @Override
        public ReservationDetailsResponse reservationDetails(ReservationDetailsRequest req) {
            return reservationDetailsImpl(req);
        }
    }
}
