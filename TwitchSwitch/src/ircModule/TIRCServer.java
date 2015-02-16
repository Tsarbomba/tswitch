package ircModule;

public class TIRCServer {
    /** The server address. */
    private final String address;
    /** The server port. */
    private final int port;
    /** The server password (or null if there is none). */
    private final String password;
    /** The nick used on server. */
    private final String nick;
    /** Default port (6667) */
    public static final int DEFAULT_PORT = 6667;

    /**
     * Creates a new TIRCServer.
     * 
     * @param address
     *            The server address.
     * @param port
     *            The server port.
     * @param password
     *            The password to use.
     */
    public TIRCServer(final String address, final int port,
            final String password, final String nick) {
        this.address = address;
        this.port = port;
        this.password = password;
        this.nick = nick;

    }

    /**
     * Retrieves the server address.
     * 
     * @return The server address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Retrieves the password.
     * 
     * @return The server password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Retrieves the port number.
     * 
     * @return The server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves nickname used on the server.
     * 
     * @return The nick.
     */
    public String getNick() {
        return nick;
    }
}
