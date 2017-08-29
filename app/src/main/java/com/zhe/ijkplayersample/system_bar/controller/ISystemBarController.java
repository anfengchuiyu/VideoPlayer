package com.zhe.ijkplayersample.system_bar.controller;

/**
 * Created by zhe on 2017/8/22.
 * System Bar控制器
 * <p>
 * 可以根据不同需求，创建controller来实现这个接口
 */

public interface ISystemBarController {

    /**
     * 显示system bar
     */
    void show();

    /**
     * 隐藏system bar
     */
    void hide();

    /**
     * 恢复最初的状态
     */
    void recover();

}
