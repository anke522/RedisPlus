var $;
var form;
var layer;
var basePath;

layui.use(['form', 'jquery', 'layer'], function () {
    $ = layui.jquery;
    form = layui.form;
    layer = layui.layer;
    basePath = $("#basePath").val();
    form.render();
    //监听提交
    form.on('submit(editBtn)', function (data) {
        submitRedisInfo();
        return false;
    });
    getRedisInfo();
});


function getRedisInfo() {
    var xhr = $.ajax({
        type: "get",
        url: basePath + '/api/conf/confInfo',
        sync: false,
        timeout: 10000,
        success: function (data) {
            var html = '';
            for (var i = 0; i < data.data.length; i++) {
                var conf = data.data[i];
                html += '<tr>';
                html += '<td>' + conf.key + ': </td>';
                html += '<td>' + conf.value + '</td>';
                html += '</tr>';
            }
            $("#tbody").html(html);
            setRedisInfo(data.data);
        },
        complete: function (XMLHttpRequest, status) {
            //请求完成后最终执行参数
            if (status == 'timeout') {
                //超时,status还有success,error等值的情况
                xhr.abort();
                layer.alert("请求超时，请检查网络连接", {
                    skin: 'layui-layer-lan',
                    closeBtn: 0
                });
            }
        }
    });
}

function setRedisInfo(data) {
    for (var i = 0; i < data.length; i++) {
        var conf = data[i];
        var currObj = $("#" + conf.key)[0];
        if (currObj) {
            $("#" + conf.key).val(conf.value);
        }
    }
    form.render();
    form.render('select');
}


function submitRedisInfo() {
    var xhr = $.ajax({
        type: "post",
        url: basePath + '/api/conf/editInfo',
        data: $('#editRedisForm').serialize(),
        success: function (data) {
            if (data.code == 200) {
                var index = layer.alert(data.msgs, {
                    skin: 'layui-layer-lan',
                    closeBtn: 0
                }, function () {
                    layer.close(index);
                    getRedisInfo();
                });
            } else {
                layer.alert(data.msgs, {
                    skin: 'layui-layer-lan',
                    closeBtn: 0
                });
            }
        },
        complete: function (XMLHttpRequest, status) {
            //请求完成后最终执行参数
            if (status == 'timeout') {
                //超时,status还有success,error等值的情况
                xhr.abort();
                layer.alert("请求超时，请检查网络连接", {
                    skin: 'layui-layer-lan',
                    closeBtn: 0
                });
            }
        }
    });
}
