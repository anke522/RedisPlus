package com.maxbill.base.controller;

import com.maxbill.base.bean.*;
import com.maxbill.base.service.DataService;
import com.maxbill.tool.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.maxbill.tool.DateUtil.DATE_STR;
import static com.maxbill.tool.RedisUtil.getRedisInfo;

@RestController
@RequestMapping("/api")
public class ApiController {

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
            tableData.setCode(500);
            tableData.setMsgs("加载数据失败");
        }
        return tableData;
    }


    @RequestMapping("/connect/insert")
    public ResponseBean insertConnect(Connect connect) {
        ResponseBean responseBean = new ResponseBean();
        try {
            connect.setId(KeyUtil.getUUIDKey());
            connect.setTime(DateUtil.formatDateTime(new Date()));
            int insFlag = this.dataService.insertConnect(connect);
            if (insFlag != 1) {
                responseBean.setCode(201);
                responseBean.setMsgs("新增连接失败");
            }
        } catch (Exception e) {
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
            boolean openFlag = JschUtil.openSSH(connect);
            if (openFlag) {
                Connect connect1 = new Connect();
                connect1.setHost("127.0.0.1");
                connect1.setPort("55555");
                connect1.setPass("123456");
                Jedis jedis = RedisUtil.openJedis(connect1);
                if (null != jedis) {
                    WebUtil.setSessionAttribute("connect", connect);
                    responseBean.setData("已经连接到： " + connect.getName());
                    RedisUtil.closeJedis(jedis);
                } else {
                    WebUtil.setSessionAttribute("connect", null);
                    responseBean.setCode(0);
                    responseBean.setMsgs("打开连接失败");
                    responseBean.setData("未连接服务");
                }
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("SSH打开失败");
                responseBean.setData("未连接服务");
            }
        } catch (Exception e) {
            e.printStackTrace();
            WebUtil.setSessionAttribute("connect", null);
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
            responseBean.setData("未连接服务");
        }
        return responseBean;
    }


    @RequestMapping("/connect/isopen")
    public Integer isopenConnect() {
        Jedis jedis = DataUtil.getCurrentJedisObject();
        if (null != jedis) {
            RedisUtil.closeJedis(jedis);
            return 1;
        } else {
            return 0;
        }
    }

    @RequestMapping("/connect/import")
    public ResponseBean importConnect(MultipartFile file) {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<Connect> dataLIst = ExcelUtil.importExcel(file.getInputStream());
            if (!dataLIst.isEmpty()) {
                for (Connect connect : dataLIst) {
                    this.dataService.insertConnect(connect);
                }
                responseBean.setMsgs("导入连接成功");
            } else {
                responseBean.setMsgs("导入连接为空");
            }
        } catch (Exception e) {
            responseBean.setCode(500);
            responseBean.setMsgs("导入连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/connect/export")
    public ResponseBean exportConnect() {
        ResponseBean responseBean = new ResponseBean();
        try {
            ExcelBean excelBean = new ExcelBean();
            excelBean.setName("连接信息");
            List<String> titles = new ArrayList();
            titles.add("连接名称");
            titles.add("连接主机");
            titles.add("连接端口");
            titles.add("连接密码");
            titles.add("创建时间");
            excelBean.setTitles(titles);
            excelBean.setRows(this.dataService.selectConnect());
            String filePath = System.getProperty("user.home") + "/";
            String fileName = DateUtil.formatDate(new Date(), DATE_STR) + "-redis-connect" + ".xlsx";
            boolean exportFlag = ExcelUtil.exportExcel(excelBean, filePath + fileName);
            if (exportFlag) {
                responseBean.setMsgs("成功导出连接至用户目录");
            } else {
                responseBean.setMsgs("导出连接失败");
            }
        } catch (Exception e) {
            responseBean.setCode(500);
            responseBean.setMsgs("导出连接异常");
        }
        return responseBean;
    }


    @RequestMapping("/data/treeInit")
    public ResponseBean treeInit() {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<ZTreeBean> treeList = new ArrayList<>();
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                for (int i = 0; i < 16; i++) {
                    long dbSize = RedisUtil.dbSize(jedis, i);
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
            e.printStackTrace();
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
            e.printStackTrace();
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            responseBean.setCode(500);
            responseBean.setMsgs("发送异常，请检查网络");
        }
        return responseBean;
    }


}
