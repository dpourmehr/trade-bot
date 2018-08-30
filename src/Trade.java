import org.powerbot.script.Condition;
import org.powerbot.script.rt4.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by darie on 8/12/2018.
 */
public class Trade extends Task<ClientContext> {
    private int[] treeIds = {1278};
    boolean inTrade = false;
    String currentTradeUser = "";
    String ticketId = null;
    Integer tradeAmount = new Integer(0);
    int gpExpect = 100;
    int currentGp = 0;
    boolean firstTradeAccepted = false;
    boolean tradeLinkClicked = false;
    String ticketType = "deposit";
    boolean acceptedTrade = false;
    boolean coinsOffered = false;
    private Timer t = new Timer();
    boolean tradeDeclined = false;
    Server s = new Server();

    private HashMap<String, Boolean> tradesMap = new HashMap<>();

    public Trade(ClientContext ctx) {
        super(ctx);

        for(Item i : ctx.inventory.items()) {
            if(i.id() == 995) {
                this.currentGp = i.stackSize();
            }
        }

        ctx.widgets.widget(162).component(12).interact("Public: Off");
        ctx.widgets.widget(162).component(24).click();

        this.t = new Timer();

        t.schedule(this.checkBotQueue(), 0, 10000);
    }

    @Override
    public boolean activate() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {

        Component chatMessage = ctx.widgets.widget(162).component(51).components()[0];

        if(chatMessage.text().equalsIgnoreCase("Other player declined trade.")) {
            if(this.tradeDeclined) {
                return false;
            }

            this.s.addSupportTicketMessage("You declined the trade. Please trade again!",
                    this.ticketId);
            this.currentTradeUser = null;
            this.tradeAmount = new Integer(0);
            this.acceptedTrade = false;
            this.inTrade = false;
            this.firstTradeAccepted = false;
            this.tradeLinkClicked = false;
            this.coinsOffered = false;
            this.tradeDeclined = true;
            return false;
        }

        if(chatMessage.text().equalsIgnoreCase("Sending trade offer...") && !ctx.widgets.widget(335).component(25).visible() && !ctx.widgets.widget(334).component(25).visible()) {
            this.tradeLinkClicked = false;
            this.tradeDeclined = false;
            return false;
        }

        if(chatMessage.text().equalsIgnoreCase("Sending trade offer...") && (ctx.widgets.widget(335).component(25).visible() || ctx.widgets.widget(334).component(25).visible())) {
            this.tradeDeclined = false;
            return true;
        }

        if(chatMessage.text().contains("wishes to trade with you") || inTrade) {

            String parsedUser = "";

            if(chatMessage.text().contains("wishes to trade with you")) {
                this.acceptedTrade = false;
                this.inTrade = false;
                parsedUser = chatMessage.text().substring(0, chatMessage.text().indexOf("wishes to trade with you") - 1);
                parsedUser = parsedUser.substring("<col=7f007f>".length());
            }

            if(inTrade) {
                parsedUser = this.currentTradeUser;
            }

            if(ctx.widgets.widget(335).component(25).visible() || ctx.widgets.widget(334).component(25).visible()) {
                return true;
            } else if(this.currentTradeUser != null && !inTrade) {
                if(!parsedUser.equalsIgnoreCase(this.currentTradeUser) && this.currentTradeUser != "") {
                    this.tradeLinkClicked = false;
                } else if(parsedUser.equalsIgnoreCase(this.currentTradeUser) && !this.tradeLinkClicked && !ctx.widgets.widget(335).component(25).visible() && !ctx.widgets.widget(334).component(25).visible()) {
                    if(this.ticketType.equalsIgnoreCase("withdraw")) {
                        if(ctx.inventory.items()[0].stackSize() < this.tradeAmount) {
                            return false;
                        }
                    }
                    this.coinsOffered = false;
                    chatMessage.click();
                    this.tradeLinkClicked = true;
                    this.tradesMap.put(parsedUser, true);
                    this.inTrade = true;
                    this.currentTradeUser = parsedUser;
                    return false;
                }
            }

        } else if(chatMessage.text().contains("Accepted trade") && this.acceptedTrade) {
            boolean accepted = this.s.tradeAccepted(this.ticketId);

            if(!Condition.wait(new Condition.Check() {
                public boolean poll() {
                    return accepted;
                }
            }, 300, 10)) {

            } else {
                this.s.addSupportTicketMessage("Trade successful. Admin is free to resolve coins.",
                        this.ticketId);
                tradesMap.remove(this.currentTradeUser);
                this.currentTradeUser = null;
                this.ticketId = null;
                this.tradeAmount = new Integer(0);
                this.acceptedTrade = false;
                this.inTrade = false;
                this.firstTradeAccepted = false;
                this.coinsOffered = false;
                this.tradeDeclined = false;
            }
        }

        return false;
    }

    @Override
    public void execute() {

        if(this.ticketType.equalsIgnoreCase("deposit")) {

            /**
             * widget: 335 = trade window
             * component 25: my trade offer
             * component 24: my offer string: example: You offer:<br>(Value: <col=ffffff>0</col> coins)
             * component 28: their trade offer
             * componet 28: component 0 = first space: has an item with an quantity
             * component 27: their offer string: example: n0t_that_guy offers:<br>(Value: <col=ffffff>24</col> coins)
             * itemId 995 = gold, quantity should be associated
             *
             * 1st: accept: component 12
             * 1st: decline: component 15
             * 1st other player name: component 31 "Trading With: n0t_that_guy"
             *
             * widget: 334 = 2nd trade window
             * component 28 = my offer list
             * space 0 text = 'Absolutely nothing!'
             * component 29 = their offer list
             * space 0 text = Coins<col=ffffff> x </col=ffff00>5
             *
             * 2nd accept: component 25
             * 2nd decline: component 26
             * 2nd other play name: component 30 "Trading with:<br>n0t_that_guy
             */

            /**
             * if the first trade window has component 25 visible
             * we can assume a trade is open and we know what window we're on
             *
             * we will loop through all the item components on that window
             * and check for item id 995 (gp)
             * if the amount on that stack is equal to the amount
             * from the database entry, then we know it's valid
             * and we continue with the trade by clicking accept
             *
             * if it's not visible but the 2nd trade window's is
             * we know we're on the confirmation step of the trade
             *
             * we do another loop through line item components for
             * another layer of validation, and if it's not happy with
             * the amount shown, it will decline and send a message
             * back to our node server
             */
            if(ctx.widgets.widget(335).component(25).visible()) {
                this.tradeLinkClicked = false;
                for(Component c : ctx.widgets.widget(335).component(28).components()) {
                    if(c.itemId() == 995) {
                        if(c.itemStackSize() == this.tradeAmount) {
                            ctx.widgets.widget(335).component(12).click();
                        } else if(c.itemStackSize() != this.tradeAmount) {
                            ctx.widgets.widget(335).component(15).click();
                            this.s.addSupportTicketMessage("Your deposit does not match the amount listed on the ticket. Trade declined.", this.ticketId);
                        }
                    }
                }
            } else if(ctx.widgets.widget(334).component(25).visible()) {
                this.tradeLinkClicked = false;
                boolean found = false;
                boolean acceptAttempted = false;
                for(Component c : ctx.widgets.widget(334).component(29).components()) {
                    String coinText = c.text();
                    if(coinText.contains("Coins<")) {
                        found = true;
                        if(coinText.contains("(")) {
                            coinText = coinText.substring(coinText.indexOf("(") + 1);
                            coinText = coinText.replace(")", "").replace(",", "");
                        } else {
                            coinText = coinText.substring(coinText.indexOf("0>") + 2).replace(",",  "");
                        }

                        if(Integer.parseInt(coinText) == this.tradeAmount) {
                            acceptAttempted = true;
                            ctx.widgets.widget(334).component(25).click();
                            this.acceptedTrade = true;
                            this.tradeFinished();
                            return;
                        } else {
                            ctx.widgets.widget(334).component(26).click();
                            this.s.addSupportTicketMessage("Your deposit does not match the amount listed on the ticket. Trade declined.",
                                    this.ticketId);
                        }
                    }
                }
                if(found == true && acceptAttempted == false) {
                    ctx.widgets.widget(334).component(26).click();
                    this.s.addSupportTicketMessage("Your deposit does not match the amount listed on the ticket. Trade declined.",
                            this.ticketId);
                }
            }
        } else { //same as above but for a user taking gold from us instead of giving it
            if(ctx.widgets.widget(335).component(25).visible()) {
                if(!this.coinsOffered) {
                    ctx.inventory.items()[0].interact("Offer-X");
                    /**
                     * use condition.wait to make sure we're prompted to offer-x
                     * before automatically inputting the amount from the database ticket
                     */
                    if(!Condition.wait(new Condition.Check() {
                        public boolean poll() {
                            return ctx.chat.pendingInput();
                        }
                    }, 300, 10)) {

                    } else {
                        Condition.sleep();
                        ctx.input.sendln(String.valueOf(this.tradeAmount));
                        this.coinsOffered = true;
                    }

                }
                this.tradeLinkClicked = false;
                for(Component c : ctx.widgets.widget(335).component(25).components()) {
                    if(c.itemId() == 995) {
                        if(c.itemStackSize() == this.tradeAmount) {
                            ctx.widgets.widget(335).component(12).click();
                        } else if(c.itemStackSize() != this.tradeAmount) {
                            ctx.widgets.widget(335).component(15).click();
                        }
                    }
                }
            } else if(ctx.widgets.widget(334).component(25).visible()) {
                this.tradeLinkClicked = false;
                boolean found = false;
                for(Component c : ctx.widgets.widget(334).component(28).components()) {
                    String coinText = c.text();
                    if(coinText.contains("Coins<")) {
                        found = true;
                        if(coinText.contains("(")) {
                            coinText = coinText.substring(coinText.indexOf("(") + 1);
                            coinText = coinText.replace(")", "").replace(",", "");
                        } else {
                            coinText = coinText.substring(coinText.indexOf("0>") + 2).replace(",",  "");
                        }

                        if(Integer.parseInt(coinText) == this.tradeAmount) {
                            ctx.widgets.widget(334).component(25).click();
                            this.acceptedTrade = true;
                            this.tradeFinished();
                            return;
                        }
                    }
                }
                if(found == true) {
                    ctx.widgets.widget(334).component(26).click();
                }
            }
        }


    }

    private void tradeFinished() {
        this.currentTradeUser = null;
        this.inTrade = false;
    }

    private TimerTask checkBotQueue() {
        return new TimerTask() {
            @Override
            public void run() {
                if(ticketId == null) {
                    String response = s.getTopOfQueue();

                    if (response == null) {
                        ctx.game.logout();
                        ctx.properties.setProperty("login.disable", "true");
                    } else {

                        ctx.properties.setProperty("login.disable", "false");
                        String[] responseColumns = response.split(",");

                        String rsn = responseColumns[0];
                        String amount = responseColumns[1];
                        String ticketId2 = responseColumns[2];
                        ticketType = responseColumns[3];

                        currentTradeUser = rsn;
                        tradesMap.put(rsn, true);
                        ticketId = ticketId2;
                        tradeAmount = new Integer(Integer.parseInt(amount));
                    }
                }
            }
        };
    }
}
