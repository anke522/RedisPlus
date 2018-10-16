package com.maxbill.base.controller;

import com.maxbill.base.bean.*;
import com.maxbill.base.service.DataService;
import com.maxbill.tool.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.maxbill.tool.RedisUtil.getRedisInfo;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static Logger log = Logger.getLogger(ApiController.class);

    @Autowired
    private DataService dataService;

    @RequestMapping("/connect/select")
    public DataTable selectConnect() {
        DataTable tableData = new DataTable();
        try {
            List dataList = this.dataService.selectConnect();
            tableData.setCode(200);
            tableData.setCount(dataList.size());
            tableData.setData(dataList);
        } catch (Exception e) {
            log.error(e);
            tableData.setCode(500);
            tableData.setMsgs("加载数据失败");
        }
        return tableData;
    }


    @RequestMapping("/connect/insert")
    public ResponseBean insertConnect(Connect connect) {
        ResponseBean responseBean = new ResponseBean();
        try {
            int insFlag = this.dataService.insertConnect(connect);
            if (insFlag != 1) {
                responseBean.setCode(201);
                responseBean.setMsgs("新增连接失败");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("新增连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/connect/update")
    public ResponseBean updateConnect(Connect connect) {
        ResponseBean responseBean = new ResponseBean();
        try {
            int updFlag = this.dataService.updateConnect(connect);
            if (updFlag != 1) {
                responseBean.setCode(201);
                responseBean.setMsgs("修改连接失败");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("修改连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/connect/delete")
    public ResponseBean deleteConnect(String id) {
        ResponseBean responseBean = new ResponseBean();
        try {
            int delFlag = this.dataService.deleteConnectById(id);
            if (delFlag != 1) {
                responseBean.setCode(201);
                responseBean.setMsgs("删除连接失败");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("删除连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/connect/create")
    public ResponseBean createConnect(String id) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Connect connect = this.dataService.selectConnectById(id);
            if ("1".equals(connect.getType())) {
                JschUtil.openSSH(connect);
            }
            if (connect.getIsha().equals("0")) {
                Jedis jedis = RedisUtil.openJedis(connect);
                if (null != jedis) {
                    WebUtil.setSessionAttribute("connect", connect);
                    responseBean.setData("已经连接到： " + connect.getText());
                    RedisUtil.closeJedis(jedis);
                } else {
                    WebUtil.setSessionAttribute("connect", null);
                    responseBean.setCode(0);
                    responseBean.setMsgs("打开连接失败");
                    responseBean.setData("未连接服务");
                }
            } else {
                JedisCluster cluster = ClusterUtil.openCulter(connect);
                if (null == cluster || cluster.getClusterNodes().size() == 0) {
                    responseBean.setCode(0);
                    responseBean.setMsgs("连接集群失败，请检查连接信息!");
                    responseBean.setData("未连接服务");
                    WebUtil.setSessionAttribute("connect", null);
                } else {
                    WebUtil.setSessionAttribute("connect", connect);
                    responseBean.setData("已经连接到： " + connect.getText());
                }
            }
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
            WebUtil.setSessionAttribute("connect", null);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
            responseBean.setData("未连接服务");
        }
        return responseBean;
    }

    @RequestMapping("/connect/discon")
    public ResponseBean disconConnect(String id) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Connect connect = DataUtil.getCurrentOpenConnect();
            if (null != connect) {
                if (connect.getIsha().equals("0")) {
                    Jedis jedis = RedisUtil.openJedis(connect);
                    if (null != jedis) {
                        RedisUtil.closeJedis(jedis);
                    }
                } else {
                    ClusterUtil.closeCulter();
                }
                WebUtil.setSessionAttribute("connect", null);
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
        }
        return responseBean;
    }


    @RequestMapping("/connect/isopen")
    public Integer isopenConnect() {
        try {
            Connect connect = DataUtil.getCurrentOpenConnect();
            if (null != connect) {
                if (connect.getIsha().equals("0")) {
                    Jedis jedis = DataUtil.getCurrentJedisObject();
                    if (null != jedis) {
                        RedisUtil.closeJedis(jedis);
                        return 1;
                    } else {
                        return 0;
                    }
                } else {
                    JedisCluster jedisCluster = DataUtil.getJedisClusterObject();
                    if (null != jedisCluster) {
                        ClusterUtil.closeCulter();
                        return 1;
                    } else {
                        return 0;
                    }
                }
            } else {
                return 0;
            }
        } catch (Exception e) {
            log.error(e);
            return 0;
        }
    }

    @RequestMapping("/data/treeInit")
    public ResponseBean treeInit() {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<ZTreeBean> treeList = new ArrayList<>();
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                String role = jedis.info("server");
                for (int i = 0; i < 16; i++) {
                    long dbSize = 0l;
                    if (i > 0 && role.indexOf("redis_mode:cluster") > -1) {
                        break;
                    }
                    if (role.indexOf("redis_mode:cluster") > -1) {
                        dbSize = RedisUtil.dbSize(jedis, null);
                    } else {
                        dbSize = RedisUtil.dbSize(jedis, i);
                    }
                    ZTreeBean zTreeBean = new ZTreeBean();
                    zTreeBean.setId(KeyUtil.getUUIDKey());
                    zTreeBean.setName("DB" + i + " (" + dbSize + ")");
                    zTreeBean.setPattern("");
                    zTreeBean.setParent(true);
                    zTreeBean.setCount(dbSize);
                    zTreeBean.setPage(1);
                    zTreeBean.setIndex(i);
                    treeList.add(zTreeBean);
                }
                responseBean.setData(treeList);
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/treeInit")
    public ResponseBean manyTreeInit() {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<ZTreeBean> treeList = new ArrayList<>();
            List<RedisNode> nodeList = ClusterUtil.getClusterNode(DataUtil.getCurrentOpenConnect());
            Map<String, RedisNode> masterNode = ClusterUtil.getMasterNode(nodeList);
            JedisCluster cluster = ClusterUtil.openCulter(DataUtil.getCurrentOpenConnect());
            Map<String, JedisPool> clusterNodes = cluster.getClusterNodes();
            long total = 0l;
            for (String nk : clusterNodes.keySet()) {
                if (masterNode.keySet().contains(nk)) {
                    Jedis jedis = clusterNodes.get(nk).getResource();
                    total = total + jedis.dbSize();
                    RedisUtil.closeJedis(jedis);
                }
            }
            ZTreeBean zTreeBean = new ZTreeBean();
            zTreeBean.setId(KeyUtil.getUUIDKey());
            zTreeBean.setName("全部集群节点的KEY" + "(" + total + ")");
            zTreeBean.setPattern("");
            zTreeBean.setParent(true);
            zTreeBean.setCount(total);
            zTreeBean.setPage(1);
            zTreeBean.setIndex(0);
            treeList.add(zTreeBean);
            responseBean.setData(treeList);
            ClusterUtil.closeCulter();
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/likeInit")
    public ResponseBean likeInit(int index, String pattern) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                long keysCount = RedisUtil.getKeysCount(jedis, index, pattern);
                ZTreeBean zTreeBean = new ZTreeBean();
                zTreeBean.setId(KeyUtil.getUUIDKey());
                zTreeBean.setName("DB" + index + " (" + keysCount + ")");
                zTreeBean.setParent(true);
                zTreeBean.setCount(keysCount);
                zTreeBean.setPage(1);
                zTreeBean.setPattern(pattern);
                zTreeBean.setIndex(index);
                responseBean.setData(zTreeBean);
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/likeInit")
    public ResponseBean manyLikeInit(String pattern) {
        ResponseBean responseBean = new ResponseBean();
        try {
            if (StringUtils.isEmpty(pattern)) {
                pattern = "*";
            }
            List<ZTreeBean> treeList = new ArrayList<>();
            List<RedisNode> nodeList = ClusterUtil.getClusterNode(DataUtil.getCurrentOpenConnect());
            Map<String, RedisNode> masterNode = ClusterUtil.getMasterNode(nodeList);
            JedisCluster cluster = ClusterUtil.openCulter(DataUtil.getCurrentOpenConnect());
            Map<String, JedisPool> clusterNodes = cluster.getClusterNodes();
            long total = 0l;
            for (String nk : clusterNodes.keySet()) {
                if (masterNode.keySet().contains(nk)) {
                    Jedis jedis = clusterNodes.get(nk).getResource();
                    total = total + jedis.keys(pattern).size();
                    RedisUtil.closeJedis(jedis);
                }
            }
            ZTreeBean zTreeBean = new ZTreeBean();
            zTreeBean.setId(KeyUtil.getUUIDKey());
            zTreeBean.setName("全部集群节点的KEY" + "(" + total + ")");
            zTreeBean.setPattern(pattern);
            zTreeBean.setParent(true);
            zTreeBean.setCount(total);
            zTreeBean.setPage(1);
            zTreeBean.setIndex(0);
            treeList.add(zTreeBean);
            responseBean.setData(treeList);
            ClusterUtil.closeCulter();
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/treeData")
    public ResponseBean treeData(String id, int index, int page, int count, String pattern) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                List<ZTreeBean> treeList = RedisUtil.getKeyTree(jedis, index, id, pattern);
                int startIndex = (page - 1) * 50;
                int endIndex = page * 50;
                if (endIndex > count) {
                    endIndex = count;
                }
                responseBean.setData(treeList.subList(startIndex, endIndex));
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/treeData")
    public ResponseBean manyTreeData(String id, int page, int count, String pattern) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (StringUtils.isEmpty(pattern)) {
                    pattern = "*";
                }
                List<RedisNode> nodeList = ClusterUtil.getClusterNode(DataUtil.getCurrentOpenConnect());
                Map<String, RedisNode> masterNode = ClusterUtil.getMasterNode(nodeList);
                Map<String, JedisPool> clusterNodes = cluster.getClusterNodes();
                List<ZTreeBean> treeList = new ArrayList<>();
                List<String> keyList = new ArrayList<>();
                for (String nk : clusterNodes.keySet()) {
                    if (masterNode.keySet().contains(nk)) {
                        Jedis jedis = clusterNodes.get(nk).getResource();
                        keyList.addAll(jedis.keys(pattern));
                        RedisUtil.closeJedis(jedis);
                    }
                }
                int startIndex = (page - 1) * 50;
                int endIndex = page * 50;
                if (endIndex > count) {
                    endIndex = count;
                }
                keyList = keyList.subList(startIndex, endIndex);
                for (String key : keyList) {
                    ZTreeBean zTreeBean = new ZTreeBean();
                    zTreeBean.setId(KeyUtil.getUUIDKey());
                    zTreeBean.setPId(id);
                    zTreeBean.setName(key);
                    zTreeBean.setParent(false);
                    zTreeBean.setIcon("../image/data-key.png");
                    treeList.add(zTreeBean);
                }
                responseBean.setData(treeList);
                ClusterUtil.closeCulter();
            } else {
                responseBean.setCode(500);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }

    @RequestMapping("/data/keysData")
    public ResponseBean keysData(int index, String key) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    responseBean.setData(RedisUtil.getKeyInfo(jedis, index, key));
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/keysData")
    public ResponseBean manyKeysData(String key) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    responseBean.setData(ClusterUtil.getKeyInfo(cluster, key));
                    ClusterUtil.closeCulter();
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/nodeInfo")
    public ResponseBean manyNodeInfo() {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<RedisNode> nodeList = ClusterUtil.getClusterNode(DataUtil.getCurrentOpenConnect());
            if (null != nodeList) {
                responseBean.setData(nodeList);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("重命名操作异常");
        }
        return responseBean;
    }

    @RequestMapping("/data/renameKey")
    public ResponseBean renameKey(int index, String oldKey, String newKey) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, oldKey)) {
                    if (!RedisUtil.existsKey(jedis, index, newKey)) {
                        RedisUtil.renameKey(jedis, index, oldKey, newKey);
                    } else {
                        responseBean.setCode(0);
                        responseBean.setMsgs("'" + newKey + "' 该key已存在");
                    }
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + oldKey + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("重命名操作异常");
        }
        return responseBean;
    }

    @RequestMapping("/many/renameKey")
    public ResponseBean manyRenameKey(String oldKey, String newKey) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, oldKey)) {
                    if (!ClusterUtil.existsKey(cluster, newKey)) {
                        ClusterUtil.renameKey(cluster, oldKey, newKey);
                    } else {
                        responseBean.setCode(0);
                        responseBean.setMsgs("'" + newKey + "' 该key已存在");
                    }
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + oldKey + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("重命名操作异常");
        }
        return responseBean;
    }

    @RequestMapping("/data/retimeKey")
    public ResponseBean retimeKey(int index, String key, int time) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.retimeKey(jedis, index, key, time);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("重命名操作异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/retimeKey")
    public ResponseBean manyRetimeKey(String key, int time) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.retimeKey(cluster, key, time);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("重命名操作异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/deleteKey")
    public ResponseBean deleteKey(int index, String key) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.deleteKey(jedis, index, key);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/deleteKey")
    public ResponseBean manyDeleteKey(String key) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.deleteKey(cluster, key);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/updateStr")
    public ResponseBean updateStr(int index, String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.updateStr(jedis, index, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }

    @RequestMapping("/many/updateStr")
    public ResponseBean manyUpdateStr(String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.updateStr(cluster, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }

    @RequestMapping("/data/insertSet")
    public ResponseBean insertSet(int index, String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.insertSet(jedis, index, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/insertSet")
    public ResponseBean manyInsertSet(String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.insertSet(cluster, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/insertZset")
    public ResponseBean insertZset(int index, String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.insertZset(jedis, index, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }

    @RequestMapping("/many/insertZset")
    public ResponseBean manyInsertZset(String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.insertZset(cluster, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/insertList")
    public ResponseBean insertList(int index, String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.insertList(jedis, index, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/insertList")
    public ResponseBean manyInsertList(String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.insertList(cluster, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/insertHash")
    public ResponseBean insertHash(int index, String key, String mapKey, String mapVal) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.insertHash(jedis, index, key, mapKey, mapVal);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }

    @RequestMapping("/many/insertHash")
    public ResponseBean manyInsertHash(String key, String mapKey, String mapVal) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.insertHash(cluster, key, mapKey, mapVal);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/deleteSet")
    public ResponseBean deleteSet(int index, String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.deleteSet(jedis, index, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/deleteSet")
    public ResponseBean manyDeleteSet(String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.deleteSet(cluster, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/deleteZset")
    public ResponseBean deleteZset(int index, String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.deleteZset(jedis, index, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/deleteZset")
    public ResponseBean manyDeleteZset(String key, String val) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.deleteZset(cluster, key, val);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/deleteList")
    public ResponseBean deleteList(int index, String key, long keyIndex) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.deleteList(jedis, index, key, keyIndex);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/deleteList")
    public ResponseBean manyDeleteList(String key, long keyIndex) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.deleteList(cluster, key, keyIndex);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/deleteHash")
    public ResponseBean deleteHash(int index, String key, String mapKey) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, index, key)) {
                    RedisUtil.deleteHash(jedis, index, key, mapKey);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/many/deleteHash")
    public ResponseBean manyDeleteHash(String key, String mapKey) {
        ResponseBean responseBean = new ResponseBean();
        try {
            JedisCluster cluster = DataUtil.getJedisClusterObject();
            if (null != cluster) {
                if (ClusterUtil.existsKey(cluster, key)) {
                    ClusterUtil.deleteHash(cluster, key, mapKey);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/info/realInfo")
    public ResponseBean realInfo() {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                Map resultMap = new HashMap();
                resultMap.put("key", DateUtil.formatDate(new Date(), DateUtil.TIME_STR));
                RedisInfo redisInfo = getRedisInfo(jedis);
                String[] memory = redisInfo.getMemory().split("\n");
                String val01 = StringUtil.getValueString(":", memory[1]).replace("\r", "");
                String[] cpu = redisInfo.getCpu().split("\n");
                String val02 = StringUtil.getValueString(":", cpu[1]).replace("\r", "");
                resultMap.put("val01", (float) (Math.round((Float.valueOf(val01) / 1048576) * 100)) / 100);
                resultMap.put("val02", Float.valueOf(val02));
                responseBean.setData(resultMap);
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
            log.error(e);
        }
        return responseBean;
    }


    @RequestMapping("/conf/confInfo")
    public ResponseBean confInfo() {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                responseBean.setData(RedisUtil.getRedisConfig(jedis));
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("请求数据异常");
        }
        return responseBean;
    }


    @RequestMapping("/conf/editInfo")
    public ResponseBean editInfo(HttpServletRequest request) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                RedisUtil.setRedisConfig(jedis, request.getParameterMap());
                responseBean.setMsgs("修改配置成功");
                RedisUtil.closeJedis(jedis);
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("请求数据异常");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("修改配置异常");
        }
        return responseBean;
    }


    @RequestMapping("/self/sendMail")
    public ResponseBean sendMail(String mailAddr, String mailText) {
        ResponseBean responseBean = new ResponseBean();
        try {
            boolean sendFlag = new MailUtil().sendMail(mailAddr, mailText);
            if (sendFlag) {
                responseBean.setMsgs("发送成功");
            } else {
                responseBean.setMsgs("发送失败");
            }
        } catch (Exception e) {
            log.error(e);
            responseBean.setCode(500);
            responseBean.setMsgs("发送异常，请检查网络");
        }
        return responseBean;
    }


}
