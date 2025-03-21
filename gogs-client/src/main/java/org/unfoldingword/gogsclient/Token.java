package org.unfoldingword.gogsclient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an application authentication token
 */
public class Token {
    private String name = null;
    private String sha1 = null;
    private String[] scopes = new String[]{"all"};

    private Token() {}

    public Token(String name) {
        this.name = name;
    }

    public Token(String name, String[] scopes) {
        this.name = name;
        this.scopes = scopes;
    }

    public Token(String name, String sha1) {
        this.name = name;
        this.sha1 = sha1;
    }

    /**
     * Returns a token parsed from json
     * @param json
     * @return
     */
    public static Token fromJSON(JSONObject json) {
        if(json != null) {
            Token token = new Token();
            token.name = (String)Util.getFromJSON(json, "name", null);
            token.sha1 = (String)Util.getFromJSON(json, "sha1", null);
            token.scopes = (String[])Util.getFromJSON(json, "scopes", new String[]{"all"});
            return token;
        }
        return null;
    }

    /**
     * Returns the json form of the token
     * @return
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", this.name);
        json.put("sha1", this.sha1);
        json.put("scopes", this.scopes);
        return json;
    }

    @Override
    public String toString() {
        return this.sha1;
    }

    /**
     * Returns the name of the token
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the scopes of the token
     * @return
     */
    public String[] getScopes() {
        return scopes;
    }
}
