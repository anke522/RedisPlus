package com.maxbill.base.service;

import com.maxbill.base.bean.Connect;
import com.maxbill.base.dao.DataMapper;
import com.maxbill.tool.DateUtil;
import com.maxbill.tool.KeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DataServiceImpl implements DataService {

    @Autowired
    private DataMapper dataMapper;

    public void createConnectTable() throws Exception {
        this.dataMapper.createConnectTable();
    }

    public int isExistsTable(String tableName) {
        return this.dataMapper.isExistsTable(tableName);
    }

    public Connect selectConnectById(String id) {
        return this.dataMapper.selectConnectById(id);
    }

    public List<Connect> selectConnect() throws Exception {
        return this.dataMapper.selectConnect();
    }

    public int insertConnect(Connect obj) throws Exception {
        obj.setId(KeyUtil.getUUIDKey());
        obj.setTime(DateUtil.formatDateTime(new Date()));
        if ("0".equals(obj.getType())) {
            obj.setSname("--");
        } else {
            obj.setRhost("127.0.0.1");
        }
        return this.dataMapper.insertConnect(obj);
    }

    public int updateConnect(Connect obj) throws Exception {
        obj.setTime(DateUtil.formatDateTime(new Date()));
        if ("0".equals(obj.getType())) {
            obj.setSname("--");
        } else {
            obj.setRhost("127.0.0.1");
        }
        return this.dataMapper.updateConnect(obj);
    }

    public int deleteConnectById(String id) throws Exception {
        return this.dataMapper.deleteConnectById(id);
    }

}
