package clientManagement;

import baseNetwork.MessageType;
import baseNetwork.SimulationManager;
import baseNetwork.WebServer;
import clientManagement.commands.PromptCommand;

/**
 * Class that provides the link between an account and a client connection
 * @author Logan Earl
 */
public class Client {
    private SimulationManager provider;
    private ClientStatus status;
    private Account associatedAccount;
    private String address;

    public Client(SimulationManager provider, String address){
        this.provider = provider;
        this.status = ClientStatus.UNAUTHENTICATED;
        this.address = address;
        provider.scheduleCommand(new PromptCommand(
                "Hello, Welcome to the project.\n" +
                "Please use the (login 'username' 'password') or the (register 'username' 'password' 'email' commands to login or register respectively.",
                provider.getServer(), address));
    }

    public void setStatus(ClientStatus newStatus){
        this.status = newStatus;
    }

    public void registerMessage(WebServer.ClientMessage message){
        if(message.getMessageType() == MessageType.CLIENT_LOGIN_MESSAGE && status == ClientStatus.UNAUTHENTICATED){
            //TODO authenticate and change state if authenticated
        }
        if(message.getMessageType() == MessageType.CLIENT_ACCOUNT_UPDATE_MESSAGE &&
                (status == ClientStatus.ACCOUNT_CREATION || status == ClientStatus.ACTIVE)){
            //TODO authenticate login info if ACTIVE, else check if account exists already. If ACTIVE and account exists, update info, if ACCOUNT_CREATION and doesn't exist create new account. All else, send failure message
        }
    }

    public String getAddress(){
        return this.address;
    }

    /**Enumeration of possible client states*/
    public enum ClientStatus{
        /**client has either logged out or was kicked at some point. Has no open connections associated with client account*/
        INACTIVE,
        /**client has just connected. We have an internet address but client has not yet provided any login info so we don't know who it is yet*/
        UNAUTHENTICATED,
        /**turns out the client is new, there is not an account on record for the client yet and the client is currently making one*/
        ACCOUNT_CREATION,
        /**client is both logged in and active, free to interact with the system normally*/
        ACTIVE
    }
}
