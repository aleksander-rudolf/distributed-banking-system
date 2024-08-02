package server_k;

public class Lock {
    //#region Class Variables
    private String type;
    //#endregion Class Variables

    //#region Constructors
    /**
     * Constructor
     * @param type The type of lock.
     */
    public Lock(String type) {
        this.type = type;
    }
    //#endregion Constructors

    //#region Getters
    /**
     * Get the type of lock.
     * @return String The type of lock.
     */
    public String getType() { return type; }
    //#endregion Getters
}
