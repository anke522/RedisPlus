package com.maxbill.base.controller;

import com.alibaba.fastjson.JSON;
import com.maxbill.base.bean.Connect;
import com.maxbill.core.desktop.Desktop;
import com.maxbill.tool.DataUtil;
import com.maxbill.tool.ItemUtil;
import com.maxbill.tool.MailUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.maxbill.tool.DataUtil.getCurrentOpenConnect;

@Component
public class OtherController {


    public void changeWebview(int pageNo) {
        switch (pageNo) {
            case 1:
                Desktop.setWebViewPage(ItemUtil.PAGE_CONNECT);
                break;
            case 2:
                Connect connect = getCurrentOpenConnect();
                if (connect.getIsha().equals("0")) {
                    Desktop.setWebViewPage(ItemUtil.PAGE_DATA_SINGLES);
                }
                if (connect.getIsha().equals("1")) {
                    Desktop.setWebViewPage(ItemUtil.PAGE_DATA_CLUSTER);
                }
                break;
            case 3:
                Desktop.setWebViewPage(ItemUtil.PAGE_INFO);
                break;
            case 4:
                Desktop.setWebViewPage(ItemUtil.PAGE_CONF);
                break;
            case 5:
                Desktop.setWebViewPage(ItemUtil.PAGE_MONITOR);
                break;
        }
    }

    public String sendMail(String mailAddr, String mailText) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            boolean sendFlag = MailUtil.sendMail(mailAddr, mailText);
            if (sendFlag) {
                resultMap.put("code", 200);
                resultMap.put("msgs", "发送邮件成功");
            } else {
                resultMap.put("code", 500);
                resultMap.put("msgs", "发送邮件失败");
            }
        } catch (Exception e) {
            resultMap.put("code", 500);
            resultMap.put("msgs", "发送邮件异常");
        }
        return JSON.toJSONString(resultMap);
    }

}
