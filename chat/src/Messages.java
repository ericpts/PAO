import java.io.Serializable;
import java.util.List;

/**
 * Created by ericpts on 5/21/17.
 */

public class Messages {

    public static class RegisterNewClient implements Serializable {
        String nickname;

        RegisterNewClient(String nickname) {
            this.nickname = nickname;
        }
    }

    public static class Message implements Serializable {
        String text; // The text of the message.
        String from; // Who sent the message.

        Message(String text, String from) {
            this.text = text;
            this.from = from;
        }
    }

    public static class PrivateMessage implements Serializable {
        String text; // The text of the message.
        String to; // The intended receiver.

        PrivateMessage(String text, String to) {
            this.text = text;
            this.to = to;
        }
    }

    public static class BroadcastMessage implements Serializable {
        String text; // The text of the message.

        BroadcastMessage(String text) {
            this.text = text;
        }
    }

    public static class AllNicknamesRequest implements Serializable {
    }

    public static class AllNicknamesResponse implements Serializable {
        List<String> nicknames;

        AllNicknamesResponse(List<String> nicknames) {
            this.nicknames = nicknames;
        }
    }
}
