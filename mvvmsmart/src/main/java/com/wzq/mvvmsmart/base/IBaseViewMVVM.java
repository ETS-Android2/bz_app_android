package com.wzq.mvvmsmart.base;

import android.os.Bundle;

public interface IBaseViewMVVM {
    /**
     * 初始化界面传递参数
     */
    void initParam();
    /**
     * 初始化数据
     */
    void initData(Bundle savedInstanceState);
    /**
     * 初始化界面观察者的监听
     */
    void initViewObservable();
    /**
     * 列表无数据点击加载网络功能
     */
    void onContentReload();
}
