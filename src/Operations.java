import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Created by ericpts on 5/22/17.
 */
public interface Operations {
    class ReservationRequest implements Serializable {
        List<Integer> seats;
        ReservationRequest(List<Integer> seats) {
            this.seats = seats;
        }
    }
    class ReservationResponse implements Serializable {
        Boolean successful;
        Integer reservartionId;

        public static ReservationResponse failed() {
            ReservationResponse ret = new ReservationResponse();
            ret.successful = false;
            ret.reservartionId = null;
            return ret;
        }

        public static ReservationResponse success(int reservationId) {
            ReservationResponse ret = new ReservationResponse();
            ret.successful = true;
            ret.reservartionId = reservationId;
            return ret;
        }
    }

    public ReservationResponse reserve(ReservationRequest req);

    class EmptySeatsRequest implements  Serializable {

    }

    class EmptySeatsResponse implements Serializable {
        List<Integer> emptySeats;

        EmptySeatsResponse(List<Integer> seats) {
            this.emptySeats = seats;
        }
    }

    public EmptySeatsResponse emptySeats(EmptySeatsRequest req);


    class ReservationDetailsRequest implements Serializable {
        Integer reservationId;
        ReservationDetailsRequest(int reservationId) {
            this.reservationId = reservationId;
        }
    }
    class ReservationDetailsResponse implements Serializable {
        List<Integer> seats;
        ReservationDetailsResponse(List<Integer> seats) {
            this.seats = seats;
        }
    }

    public ReservationDetailsResponse reservationDetails(ReservationDetailsRequest req);
}
