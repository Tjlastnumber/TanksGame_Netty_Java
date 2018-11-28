package com.aitank.server.handler;

import com.aitank.server.component.ObjectMultiAction;
import com.aitank.server.context.SpringContext;
import com.aitank.server.dao.UserDao;
import com.aitank.server.enums.EventType2;
import com.aitank.server.protocol.PlayInfoData;
import com.aitank.server.protocol.RoomData;
import com.aitank.server.protocol.SocketModel;
import com.aitank.utils.ChannelHandlerContextInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class TcpHandler extends ChannelInboundHandlerAdapter {


    public static Map<String, ChannelHandlerContextInfo> players = new HashMap<String, ChannelHandlerContextInfo>();
    public static Map<String, RoomData> rooms = new HashMap<>();
    public static final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public Map<EventType2, BaseHandler> handlerMap = new HashMap<>();

    /***
     * @Description: 获取消息从客户端
     * @param: [ctx, msg] |长度(4byte) | 服务id | 数据 |
     * @return: void
     * @author: lhz
     * @Date: 2018/10/26
    */
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception {
        SocketModel message = (SocketModel) msg;
        String name = message.getProtocolName();

        //SpringContext.getListenerManager().triggerEvent(name,ctx,message);  luo


        name = EventType2.getName(name);

        //第一种方式获取Class对象
        ObjectMultiAction objectMultiAction = SpringContext.getObjectMultiAction();
        Class<? extends ObjectMultiAction> stuClass = objectMultiAction.getClass();//获取Class对象
        Method m = stuClass.getMethod(name, ChannelHandlerContext.class,SocketModel.class);
        m.invoke(objectMultiAction, ctx,message);

    }

    /***
     * @Description: 获取客户端连接状态
     * @param: [ctx]
     * @return: void
     * @author: lhz
     * @Date: 2018/10/26
    */

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("clinet:" + ctx.channel().id() + " join server");
    }
    /***
     * @Description: 获取客户端关闭连接状态
     * @param: [ctx]
     * @return: void
     * @author: lhz
     * @Date: 2018/10/26
    */

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("room:" + ctx.channel().id() + " leave room");
        System.out.println("clinet:" + ctx.channel().id() + " leave server");
        super.channelInactive(ctx);

        String userId = ctx.channel().id().toString();

        if (TcpHandler.players.containsKey(userId)) {
            TcpHandler.rooms.remove(userId);
            TcpHandler.players.remove(userId);
            System.out.println("PlayerNumbers : " + TcpHandler.players.size());
            SocketModel response = new SocketModel();
            response.setProtocolName(EventType2.MsgOnLogout.getName());//("MsgOnLogout");

            PlayInfoData playInfoData = new PlayInfoData();
            playInfoData.userId = userId;

            response.serialize(playInfoData);

            /*
             * 退出房间
             */
            SocketModel room = new SocketModel();
            room.setProtocolName(EventType2.MsgExitRoom);
            room.serialize(userId);

            for (String cid : TcpHandler.players.keySet()) {
                ChannelHandlerContextInfo player = TcpHandler.players.get(cid);
                player.getChx().writeAndFlush(room);
                player.getChx().writeAndFlush(response);
            }

        }
    }

    /***
     * @Description: 获取客户端关闭异常
     * @param: [ctx, cause]
     * @return: void
     * @author: lhz
     * @Date: 2018/10/26
    */

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.add(channel);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        group.remove(channel);
    }

}
