import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by darie on 8/17/2018.
 */
public class Server {

    public String getTopOfQueue() {
        String url = "http://localhost:3000/getBotQueue";

        return this.buildGetRequest(url);
    }

    public void addSupportTicketMessage(String message, String ticketId) {
        String url = "http://localhost:3000/newTicketBotMessage?ticketId=" + ticketId + "&message=" + message.replace(" ", "-") + "&username=RuneDuels-Bot";

        this.buildGetRequest(url);
    }

    public boolean tradeAccepted(String ticketId) {
        String url = "http://localhost:3000/takeFirstOff?ticketId=" + ticketId;

        return this.buildPostMessage(url);
    }

    private String buildGetRequest(String url) {
        URL obj = null;
        try {
            obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Connection", "close");

            int responseCode = con.getResponseCode();

            if(responseCode != 200) {
                return null;
            } else {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String response2 = response.toString();
                return response2;
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean buildPostMessage(String url) {


        URL obj = null;
        try {
            obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("POST");
            con.setRequestProperty("Connection", "close");

            int responseCode = con.getResponseCode();

            if(responseCode != 200) {
                return false;
            }

            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

}
