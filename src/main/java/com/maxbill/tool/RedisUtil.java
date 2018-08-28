package com.maxbill.tool;

import com.maxbill.base.bean.Connect;
import com.maxbill.base.bean.RedisInfo;
import com.maxbill.base.bean.ZTreeBean;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.Slowlog;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class RedisUtil {

    //可用连接实例的最大数目，默认值为8；
    private static int MAX_TOTAL = 100;

    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 50;

    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    private static int MAX_WAIT = 5000;

    //超时时间
    private static final int TIME_OUT = 60000;

    //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;

    private static boolean TEST_ON_RETURN = true;

    private static JedisPool jedisPool;

    private static ReentrantLock lock = new ReentrantLock();

    private RedisUtil() {
    }

    /**
     * 初始化JedisPool
     */
    private static void initJedisPool(Connect connect) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(MAX_TOTAL);
        config.setMaxIdle(MAX_IDLE);
        config.setMaxWaitMillis(MAX_WAIT);
        config.setTestOnBorrow(TEST_ON_BORROW);
        config.setTestOnReturn(TEST_ON_RETURN);
        jedisPool = new JedisPool(config, connect.getHost(), Integer.valueOf(connect.getPort()), TIME_OUT, connect.getPass());
    }

    /**
     * 从JedisPool中获取Jedis
     */
    public static Jedis openJedis(Connect connect) {
        //防止吃初始化时多线程竞争问题
        lock.lock();
        initJedisPool(connect);
        lock.unlock();
        return jedisPool.getResource();
    }

    /**
     * 释放Jedis连接
     */
    public static void closeJedis(Jedis jedis) {
        jedis.close();
    }


    public static List<ZTreeBean> getKeyTree(Jedis jedis, ZTreeBean rootTree) {
        List<ZTreeBean> treeList = new ArrayList<>();
        treeList.add(rootTree);
        Set<String> keySet = jedis.keys("*");
        ZTreeBean zTreeBean = null;
        if (null != keySet) {
            for (String key : keySet) {
                zTreeBean = new ZTreeBean();
                zTreeBean.setId(KeyUtil.getUUIDKey());
                zTreeBean.setPId(rootTree.getId());
                zTreeBean.setName(key);
                treeList.add(zTreeBean);
            }
        }
        return treeList;
    }


    // 获取redis 服务器信息
    public static RedisInfo getRedisInfo(Jedis jedis) {
        RedisInfo redisInfo = null;
        Client client = jedis.getClient();
        client.info();
        String info = client.getBulkReply();
        String[] infos = info.split("#");
        if (null != infos && infos.length > 0) {
            redisInfo = new RedisInfo();
            redisInfo.setServer(infos[1]);
            redisInfo.setClient(infos[2]);
            redisInfo.setMemory(infos[3]);
            redisInfo.setPersistence(infos[4]);
            redisInfo.setStats(infos[5]);
            redisInfo.setReplication(infos[6]);
            redisInfo.setCpu(infos[7]);
            redisInfo.setCluster(infos[8]);
            redisInfo.setKeyspace(infos[9]);
        }
        //jedis.close();
        return redisInfo;
    }

    // 获取redis 服务器信息
    public static RedisInfo getRedisInfoList(Jedis jedis) {
        RedisInfo redisInfo = getRedisInfo(jedis);
        RedisInfo redisInfoTemp = null;
        if (null != redisInfo) {
            //服务端信息
            String[] server = redisInfo.getServer().split("\n");
            StringBuffer serverBuf = new StringBuffer();
            serverBuf.append("服务版本: ").append(StringUtil.getValueString(server[1])).append("</br>");
            serverBuf.append("服务模式: ").append(StringUtil.getValueString(server[5])).append("</br>");
            serverBuf.append("系统版本: ").append(StringUtil.getValueString(server[6])).append("</br>");
            serverBuf.append("系统类型: ").append(StringUtil.getValueString(server[7])).append("</br>");
            serverBuf.append("进程编号: ").append(StringUtil.getValueString(server[9])).append("</br>");
            serverBuf.append("服务端口: ").append(StringUtil.getValueString(server[11])).append("</br>");
            serverBuf.append("运行时间: ").append(StringUtil.getValueString(server[12])).append("</br>");
            //客户端信息
            String[] client = redisInfo.getClient().split("\n");
            StringBuffer clientBuf = new StringBuffer();
            clientBuf.append("当前连接: ").append(StringUtil.getValueString(client[1])).append("</br>");
            clientBuf.append("阻塞连接: ").append(StringUtil.getValueString(client[2])).append("</br>");
            clientBuf.append("最大输入缓存: ").append(StringUtil.getValueString(client[3])).append("</br>");
            clientBuf.append("最长输入列表: ").append(StringUtil.getValueString(client[4])).append("</br>");
            //内存的信息
            String[] memory = redisInfo.getMemory().split("\n");
            StringBuffer memoryBuf = new StringBuffer();
            memoryBuf.append("已占用内存量: ").append(StringUtil.getValueString(memory[1])).append("</br>");
            memoryBuf.append("分配内存总量: ").append(StringUtil.getValueString(memory[3])).append("</br>");
            memoryBuf.append("内存高峰值: ").append(StringUtil.getValueString(memory[5])).append("</br>");
            memoryBuf.append("内存碎片率: ").append(StringUtil.getValueString(memory[7])).append("</br>");
            memoryBuf.append("内存分配器: ").append(StringUtil.getValueString(memory[8])).append("</br>");
            //持久化信息
            String[] persistence = redisInfo.getPersistence().split("\n");
            StringBuffer persistenceBuf = new StringBuffer();
            persistenceBuf.append("加载中数据: ").append(StringUtil.getValueString(persistence[1])).append("</br>");
            //连接的信息
            String[] stats = redisInfo.getStats().split("\n");
            StringBuffer statsBuf = new StringBuffer();
            statsBuf.append("已连接客户端总数: ").append(StringUtil.getValueString(stats[1])).append("</br>");
            statsBuf.append("执行过的命令总数: ").append(StringUtil.getValueString(stats[2])).append("</br>");
            statsBuf.append("服务每秒执行数量: ").append(StringUtil.getValueString(stats[3])).append("</br>");
            statsBuf.append("服务输入网络流量: ").append(StringUtil.getValueString(stats[4])).append("</br>");
            statsBuf.append("服务输出网络流量: ").append(StringUtil.getValueString(stats[5])).append("</br>");
            statsBuf.append("拒绝连接客户端数: ").append(StringUtil.getValueString(stats[8])).append("</br>");
            redisInfoTemp = new RedisInfo();
            redisInfoTemp.setServer(serverBuf.toString());
            redisInfoTemp.setClient(clientBuf.toString());
            redisInfoTemp.setMemory(memoryBuf.toString());
            redisInfoTemp.setPersistence(persistenceBuf.toString());
            redisInfoTemp.setStats(statsBuf.toString());
        }
        return redisInfoTemp;
    }


    // 获取日志列表
    public static List<Slowlog> getRedisLog(Jedis jedis) {
        List<Slowlog> logList = jedis.slowlogGet(10);
        System.out.println(logList);
        return logList;
    }


    // 获取占用内存大小
    public Long getRedisMemoryInfo(Jedis jedis) {
        Client client = jedis.getClient();
        client.dbSize();
        return client.getIntegerReply();
    }


    public static void main(String[] args) {
//        Connect connect = new Connect();
//        connect.setHost("maxbill");
//        connect.setPort("6379");
//        connect.setPass("123456");
//        Jedis jedis = openJedis(connect);
//        jedis.set("a-b-1", "1");
//        jedis.set("a-b-2", "2");
//        jedis.set("a-b-3", "3");
//        jedis.keys("*");
//        closeJedis(jedis);
        Connect connect = new Connect();
        connect.setHost("maxbill");
        connect.setPort("6379");
        connect.setPass("123456");
        Jedis jedis = openJedis(connect);
        for (int i = 0; i < 1000; i++) {
            jedis.set(i + "", i + "");
        }
        closeJedis(jedis);
    }

}
