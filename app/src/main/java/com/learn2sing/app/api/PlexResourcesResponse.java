package com.learn2sing.app.api;

import java.util.List;

/**
 * Represents one entry from GET /api/v2/resources.
 *
 * Example:
 * {
 *   "name": "MyServer",
 *   "provides": "server",
 *   "connections": [
 *     { "uri": "https://1-2-3-4.abc.plex.direct:32400", "local": false, "relay": false },
 *     { "uri": "https://relay.plex.direct/...",          "local": false, "relay": true  },
 *     { "uri": "http://192.168.1.5:32400",               "local": true,  "relay": false }
 *   ]
 * }
 */
public class PlexResourcesResponse {

    public String name;
    public String provides;     // e.g. "server", "client"

    public List<Connection> connections;

    public static class Connection {
        public String  uri;
        public boolean local;
        public boolean relay;
    }
}
