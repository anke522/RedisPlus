package com.maxbill.tool;

import com.maxbill.base.bean.Connect;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.util.Map;

import static com.maxbill.tool.StringUtil.FLAG_COLON;

public class DataUtil {


    public static Connect getCurrentOpenConnect() {
        return (Connect) WebUtil.getSessionAttribute("connect");
    }

    public static Jedis getCurrentJedisObject() {
        Connect connect = getCurrentOpenConnect();
        if (null != connect) {
            Jedis jedis = null;
            if (connect.getIsha().equals("0")) {
                jedis = RedisUtil.getJedis();
            } else {
                JedisCluster cluster = ClusterUtil.openCulter(DataUtil.getCurrentOpenConnect());
                Map<String, JedisPool> clusterNodes = cluster.getClusterNodes();
                for (String nk : clusterNodes.keySet()) {
                    if (StringUtil.getValueString(FLAG_COLON, nk).equals(connect.getRport())) {
                        jedis = clusterNodes.get(nk).getResource();
                    }
                }
                ClusterUtil.closeCulter();
            }
            return jedis;
        } else {
            return null;
        }
    }


    public static JedisCluster getJedisClusterObject() {
        Connect connect = getCurrentOpenConnect();
        if (null != connect) {
            return ClusterUtil.getCluster(connect);
        } else {
            return null;
        }
    }


}
