package com.example.base.constant;

public enum Dictionary {
    AUDIT_COURSE_NOT_PASS("202001", "审核未通过"),
    AUDIT_COURSE_NOT_COMMIT("202002", "未提交"),
    AUDIT_COURSE_COMMIT("202003", "已提交"),
    AUDIT_COURSE_PASS("202004", "审核通过"),
    COURSE_FREE("201000", "免费"),
    COURSE_CHARGE("201001", "收费"),
    PUBLISH_NOT_PUB("203001", "未发布"),
    PUBLISH_PUB("203002", "已发布"),
    PUBLISH_OFFLINE("203003", "下线"),
    AUDIT_OBJECT_NOT_PASS("002001", "审核未通过"),
    AUDIT_OBJECT_NOT_COMMIT("002002", "未审核"),
    AUDIT_OBJECT_PASS("002003", "审核通过"),
    RESOURCE_TYPE_IMAGE("001001", "图片"),
    RESOURCE_TYPE_VIDEO("001002", "视频"),
    RESOURCE_TYPE_OTHERS("001003", "其它"),
    CHOOSE_COURSE_TYPE_FREE("700001", "免费课程"),
    CHOOSE_COURSE_TYPE_CHARGE("700002", "收费课程"),
    CHOOSE_COURSE_STATUS_SUCCESS("701001", "选课成功"),
    CHOOSE_COURSE_STATUS_UNPAID("701002", "待支付"),
    LEARNING_QUA_NORMAL("702001", "正常学习"),
    LEARNING_QUA_REJECT("702002", "没有选课或选课后没有支付"),
    LEARNING_QUA_EXPIRED("702003", "已过期需要申请续期或重新支付"),
    PAYING_METHOD_WECHAT("603001", "微信支付"),
    PAYING_METHOD_ALIPAY("603002", "支付宝"),
    PAYING_STATUS_UNPAID("601001", "未支付"),
    PAYING_STATUS_PAID("601002", "已支付"),
    PAYING_STATUS_REFUNDED("601003", "已退款"),
    ORDER_PAYING_STATUS_UNPAID("600001", "未支付"),
    ORDER_PAYING_STATUS_PAID("600002", "已支付"),
    ORDER_PAYING_STATUS_CLOSED("600003", "已关闭"),
    ORDER_PAYING_STATUS_REFUNDED("600004", "已退款"),
    ORDER_PAYING_STATUS_FINISHED("600005", "已完成"),
    ORDER_TYPE_COURSE("60201", "购买课程"),
    ORDER_TYPE_LEARNING_MATERIAL("60202", "学习资料");
    
    private final String code;
    private final String description;

    Dictionary(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}