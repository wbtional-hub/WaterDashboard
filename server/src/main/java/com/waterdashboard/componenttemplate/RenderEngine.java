package com.waterdashboard.componenttemplate;

import java.util.Arrays;

public enum RenderEngine {
    TEXT_CARD,
    ECHARTS_LINE,
    ECHARTS_BAR,
    TABLE_LIST,
    OPENLAYERS_MAP,
    BABYLON_SCENE,
    G6_TOPOLOGY;

    public static RenderEngine parse(String value) {
        return Arrays.stream(values())
                .filter(engine -> engine.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的渲染引擎类型"));
    }
}
