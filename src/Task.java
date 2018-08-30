import org.powerbot.script.ClientAccessor;
import org.powerbot.script.ClientContext;

import java.sql.SQLException;

/**
 * Created by darie on 8/12/2018.
 */
public abstract class Task<C extends ClientContext> extends ClientAccessor<C> {
    public Task(C ctx) {
        super(ctx);
    }

    public abstract boolean activate() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException;

    public abstract void execute();
}