package com.aitank.server.component;

import com.aitank.server.enums.EventType2;
import com.aitank.server.handler.TcpHandler;
import com.aitank.server.protocol.*;
import com.aitank.utils.ChannelHandlerContextInfo;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ObjectMultiAction {

    /**
     * 用户登录
     *
     * @param ctx
     * @param message
     */
    public void MsgLogin(ChannelHandlerContext ctx, SocketModel message) {
        UserLoginData UserLoginData = message.deserialize(UserLoginData.class);

        //获取数值
        System.out.println("clinet:" + ctx.channel().id() + " 用户名:" + UserLoginData.getUserName());
        System.out.println("clinet:" + ctx.channel().id() + " 密码:" + UserLoginData.getPassWord());

        //数据库查询用户是否存在   试验过可行
//        if(SpringContext.getUserService().selectUserByName(UserLoginData.getUserName())==null)
//        {
//            System.out.println("此用户不在数据库中："+ UserLoginData.getUserName());
//        }


        TcpHandler.players.put(
                ctx.channel().id().toString(),
                new ChannelHandlerContextInfo(UserLoginData.getUserName(), ctx)
        );
        System.out.println("PlayerNumbers : " + TcpHandler.players.size());

        //构建返回协议
        UserLoginData.setUserId(ctx.channel().id().toString());

        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgLogin.getName());//("MsgLogin");
        response.serialize(UserLoginData);
        ctx.writeAndFlush(response);
    }

    /**
     * 用户信息
     *
     * @param ctx
     * @param message
     */
    public void MsgPlayInfoData(ChannelHandlerContext ctx, SocketModel message) {
        PlayInfoData playInfoData = message.deserialize(PlayInfoData.class);

        ChannelHandlerContextInfo currentPlayer = TcpHandler.players.get(ctx.channel().id().toString());
        currentPlayer.setPosition(new float[]{playInfoData.position1, playInfoData.position2, playInfoData.position3});
        currentPlayer.setRotation(new float[]{playInfoData.rotation1, playInfoData.rotation2, playInfoData.rotation3, playInfoData.rotation4});
        currentPlayer.setShoot(playInfoData.shoot);
        currentPlayer.serverHealth = playInfoData.serverHealth;
        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgPlayInfoData.getName());//("MsgPlayInfoData");

        response.serialize(playInfoData);
        for (String cid : TcpHandler.players.keySet()) {
            if (cid.equals(ctx.channel().id().toString()))
                continue;
            ChannelHandlerContextInfo player = TcpHandler.players.get(cid);
            player.getChx().writeAndFlush(response);
        }
    }

    /**
     * 角色信息
     *
     * @param ctx
     * @param message
     */
    public void MsgInitPlay(ChannelHandlerContext ctx, SocketModel message) {
        PlayInfoDataList playInfoDataList = new PlayInfoDataList();

        for (String cid : TcpHandler.players.keySet()) {
            ChannelHandlerContextInfo player = TcpHandler.players.get(cid);
            PlayInfoData playInfoData = new PlayInfoData();
            playInfoData.position1 = player.getPosition()[0];
            playInfoData.position2 = player.getPosition()[1];
            playInfoData.position3 = player.getPosition()[2];
            playInfoData.rotation1 = player.getRotation()[0];
            playInfoData.rotation2 = player.getRotation()[1];
            playInfoData.rotation3 = player.getRotation()[2];
            playInfoData.rotation4 = player.getRotation()[3];
            playInfoData.shoot = player.isShoot();
            playInfoData.force = player.getForce();
            playInfoData.serverHealth = player.serverHealth;
            playInfoData.userId = player.getChx().channel().id().toString();
            playInfoData.playName = player.getName();
            playInfoData.playerNumber = playInfoDataList.playInfoDataList.size();

            playInfoDataList.playInfoDataList.add(playInfoData);
        }

        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgInitPlay.getName());//("MsgInitPlay");
        //response.serializeList(playInfoDataList);
        response.serialize(playInfoDataList);

        ctx.writeAndFlush(response);
    }

    /**
     * 创建信息
     */
    public void MsgCreateRoom(ChannelHandlerContext ctx, SocketModel msg) {
        RoomData room = msg.deserialize(RoomData.class);
        String userId = ctx.channel().id().toString();

        room.setId(UUID.randomUUID().toString());
        room.setMasterId(userId);
        room.join(userId);

        // 设置用户所在房间 id
        TcpHandler.players.get(userId).setRoomId(room.getId());

        System.out.println("Create Room: " + userId + "->" + room.getName());
        /*
         * 维护房间 Map
         */
        TcpHandler.rooms.put(room.getId(), room);
        System.out.println("Room Number: " + TcpHandler.rooms.size());

        /*
         * 通知创建用户结果
         */
        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgCreateRoom);
        response.serialize(room);
        ctx.writeAndFlush(response);

        RoomDataList roomList = new RoomDataList(new ArrayList<>(TcpHandler.rooms.values()));
        SocketModel notifyRoomListRefresh = new SocketModel();
        notifyRoomListRefresh.setProtocolName(EventType2.MsgRoomList);
        notifyRoomListRefresh.serialize(roomList);
        notificationPlayers(userId, notifyRoomListRefresh);
    }

    /**
     * 加入房间
     */
    public void MsgJoinRoom(ChannelHandlerContext ctx, SocketModel msg) {
        String userId = ctx.channel().id().toString();
        Map<String, RoomData> rooms = TcpHandler.rooms;

        System.out.println("Join Room: " + userId);

        RoomData room = msg.deserialize(RoomData.class);
        room = rooms.get(room.getId());
        room.join(userId);
        TcpHandler.players.get(userId).setRoomId(room.getId());

        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgJoinRoom);
        response.serialize(room);
        notificationPlayers(ctx.channel().id().toString(), room.getPlayers(), response);
    }

    /**
     * 离开房间
     */
    public void MsgExitRoom(ChannelHandlerContext ctx, SocketModel msg) {
        String userId = ctx.channel().id().toString();
        Map<String, RoomData> rooms = TcpHandler.rooms;
        ChannelHandlerContextInfo userInfo = TcpHandler.players.get(userId);

        RoomData room = rooms.get(userInfo.getRoomId());
        room.exit(userId);
        userInfo.setRoomId(null);

        System.out.println("Exit Room: " + userId);
        if (room.getPlayers().size() == 0) {
            rooms.remove(room.getId());
            RoomDataList roomList = new RoomDataList(new ArrayList<>(rooms.values()));
            SocketModel notifyRoomListRefresh = new SocketModel();
            notifyRoomListRefresh.setProtocolName(EventType2.MsgRoomList);
            notifyRoomListRefresh.serialize(roomList);
            notificationPlayers(userId, notifyRoomListRefresh);
            System.out.println("===" + room.getName() + " Room Close ===");
        }

        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgExitRoom);
        response.serialize(room);
        notificationPlayers(ctx.channel().id().toString(), room.getPlayers(), response);
    }

    /**
     * 发送房间列表
     */
    public void MsgRoomList(ChannelHandlerContext ctx, SocketModel msg) {
        Map<String, RoomData> roomMap = TcpHandler.rooms;
        List<RoomData> rooms = new ArrayList<>(roomMap.values());
        RoomDataList roomList = new RoomDataList(rooms);

        SocketModel response = new SocketModel();
        response.setProtocolName(EventType2.MsgRoomList);
        response.serialize(roomList);
        ctx.writeAndFlush(response);
    }

    /**
     * 通知所有玩家（排除当前会话玩家）
     * @param currentPlayer
     * @param msg
     */
    private void notificationPlayers(String currentPlayer, SocketModel msg) {
        for (String cid : TcpHandler.players.keySet()) {
            if (cid.equals(currentPlayer)) continue;
            ChannelHandlerContextInfo player = TcpHandler.players.get(cid);
            player.getChx().writeAndFlush(msg);
        }
    }

    /**
     * 通知所有玩家
     * @param msg
     */
    private void notificationPlayers(SocketModel msg) {
        for (String cid : TcpHandler.players.keySet()) {
            ChannelHandlerContextInfo player = TcpHandler.players.get(cid);
            player.getChx().writeAndFlush(msg);
        }
    }

    /**
     * 通知所有房间玩家
     * @param currentPlayer
     * @param userIds
     * @param msg
     */
    private void notificationPlayers(String currentPlayer, List<String> userIds, SocketModel msg) {
        for (String cid : userIds) {
            if (cid.equals(currentPlayer)) continue;
            ChannelHandlerContextInfo player = TcpHandler.players.get(cid);
            player.getChx().writeAndFlush(msg);
        }
    }
}
