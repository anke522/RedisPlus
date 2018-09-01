package com.maxbill.base.controller;

import com.maxbill.base.bean.*;
import com.maxbill.base.service.DataService;
import com.maxbill.tool.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.maxbill.tool.RedisUtil.getRedisInfo;

@Controller
@RequestMapping("/api")
public class ApiController {


    @Autowired
    private DataService dataService;

    @ResponseBody
    @RequestMapping("/test")
    public String test() {
        try {
            this.dataService.createConnectTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "test";
    }

    @ResponseBody
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

    @ResponseBody
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
            responseBean.setCode(500);
            responseBean.setMsgs("新增连接异常");
        }
        return responseBean;
    }

    @ResponseBody
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

    @ResponseBody
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

    @ResponseBody
    @RequestMapping("/connect/create")
    public ResponseBean createConnect(String id) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Connect connect = this.dataService.selectConnectById(id);
            Jedis jedis = RedisUtil.openJedis(connect);
            if (null != jedis) {
                WebUtil.setSessionAttribute("jedis", jedis);
                WebUtil.setSessionAttribute("connect", connect);
                responseBean.setData("已经连接到： " + connect.getName());
            } else {
                responseBean.setCode(0);
                responseBean.setMsgs("打开连接失败");
                responseBean.setData("未连接服务");
            }
        } catch (Exception e) {
            responseBean.setCode(500);
            responseBean.setMsgs("打开连接异常");
            responseBean.setData("未连接服务");
        }
        return responseBean;
    }

    @ResponseBody
    @RequestMapping("/connect/isopen")
    public Integer isopenConnect() {
        Jedis jedis = DataUtil.getCurrentJedisObject();
        if (null != jedis) {
            return 1;
        } else {
            return 0;
        }
    }


    @RequestMapping("/connect/export")
    public void exportConnect() {
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
            String fileName = DateUtil.formatDateTime(new Date()) + "-redis-connect" + ".xlsx";
            ExcelUtil.exportExcel(excelBean, filePath + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ResponseBody
    @RequestMapping("/data/treeInit")
    public ResponseBean treeInit() {
        ResponseBean responseBean = new ResponseBean();
        try {
            List<ZTreeBean> treeList = new ArrayList<>();
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                for (int i = 0; i < 16; i++) {
                    ZTreeBean zTreeBean = new ZTreeBean();
                    zTreeBean.setId(KeyUtil.getUUIDKey());
                    zTreeBean.setName("DB" + i + " (" + RedisUtil.dbSize(jedis, i) + ")");
                    zTreeBean.setParent(true);
                    zTreeBean.setIndex(i);
                    treeList.add(zTreeBean);
                }
                responseBean.setData(treeList);
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

    @ResponseBody
    @RequestMapping("/data/treeData")
    public ResponseBean treeData(String id, Integer index) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                List<ZTreeBean> treeList = RedisUtil.getKeyTree(jedis, index, id);
                responseBean.setData(treeList);
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

    @ResponseBody
    @RequestMapping("/data/keysData")
    public ResponseBean keysData(String key, Integer index) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                responseBean.setData(RedisUtil.getKeyInfo(jedis, key, index));
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

    @ResponseBody
    @RequestMapping("/data/renameKey")
    public ResponseBean renameKey(String oldKey, String newKey, Integer index) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, oldKey, index)) {
                    if (!RedisUtil.existsKey(jedis, newKey, index)) {
                        RedisUtil.renameKey(jedis, oldKey, newKey, index);
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
            e.printStackTrace();
            responseBean.setCode(500);
            responseBean.setMsgs("重命名操作异常");
        }
        return responseBean;
    }

    @ResponseBody
    @RequestMapping("/data/deleteKey")
    public ResponseBean deleteKey(String key, Integer index) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                if (RedisUtil.existsKey(jedis, key, index)) {
                    RedisUtil.deleteKey(jedis, key, index);
                } else {
                    responseBean.setCode(0);
                    responseBean.setMsgs("'" + key + "' 该key不存在");
                }
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

    @ResponseBody
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
                String val01 = StringUtil.getValueString(memory[1]).replace("\r", "");
                String[] cpu = redisInfo.getCpu().split("\n");
                String val02 = StringUtil.getValueString(cpu[1]).replace("\r", "");
                String val03 = StringUtil.getValueString(cpu[2]).replace("\r", "");
                resultMap.put("val01", Long.valueOf(val01));
                resultMap.put("val02", Float.valueOf(val02));
                resultMap.put("val03", Float.valueOf(val03));
                responseBean.setData(resultMap);
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


    @ResponseBody
    @RequestMapping("/conf/confInfo")
    public ResponseBean confInfo() {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                responseBean.setData(RedisUtil.getRedisConfig(jedis));
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

    @ResponseBody
    @RequestMapping("/conf/editInfo")
    public ResponseBean editInfo(HttpServletRequest request) {
        ResponseBean responseBean = new ResponseBean();
        try {
            Jedis jedis = DataUtil.getCurrentJedisObject();
            if (null != jedis) {
                RedisUtil.setRedisConfig(jedis, request.getParameterMap());
                responseBean.setMsgs("修改配置成功");
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


    @ResponseBody
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
