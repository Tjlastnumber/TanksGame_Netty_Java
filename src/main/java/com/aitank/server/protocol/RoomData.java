package com.aitank.server.protocol;

import java.util.ArrayList;
import java.util.List;

public class RoomData {
    private String id;
    private String Name;
    private String masterId;
    private String masterName;
    private List<String> players = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String value) { id = value; }

    public String getName() { return Name; }
    public void setName(String value) { Name = value; }

    public String getMasterId() { return masterId; }
    public void setMasterId(String value) { masterId = value; }

    public String getMasterName() { return masterName; }
    public void setMasterName(String value) { masterName = value; }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> value) { players = value; }

    public void join(String userId) {
        players.add(userId);
    }

    public void exit(String userId) {
        players.remove(userId);
    }
}

