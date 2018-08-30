import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;
import org.powerbot.script.rt4.ClientContext;

import java.sql.*;
import java.util.*;

@Script.Manifest(name="Trade Bot", description="Listens for messages on a queue of trades and performs trades automatically when found")
public class TradeBot extends PollingScript<ClientContext> {
    private List<Task> taskList = new ArrayList<Task>();

    @Override
    public void start() {

        taskList.addAll(Arrays.asList(new Trade(ctx)));

    }

    @Override
    public void poll() {
        for(Task task : taskList) {
            try {
                if (task.activate()) {
                    task.execute();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }
}