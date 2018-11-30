package com.aitank.server.protocol;

import java.util.ArrayList;
import java.util.List;

public class RoomDataList {
    public List<RoomData> list = new ArrayList<>();

    public RoomDataList(List<RoomData> list) {
        this.list = list;
    }
}
