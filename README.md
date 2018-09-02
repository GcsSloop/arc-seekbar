# ArcSeekBar(圆弧拖动条)

这个是在公司项目中准备使用的一个控件，本以为是一个很简单的控件，本来准备直接在晚上搜一个直接用，可惜找到的很多都是 ProgressBar，并不具备拖拽功能，于是自己动手实现了一个，其功能和特性均仿照 SeekBar 进行设计。

## 效果预览

<img src="pics/arcseekbar.gif" width="300"/>

## 支持的特性

- [x] 进度功能。
- [x] 自定义属性配置。
- [x] 线条宽度。
- [x] 开口角度。
- [x] 旋转角度。
- [x] 渐变色。
- [x] 最大进度值。
- [x] 拖动按钮颜色、大小、样式(填充、描边、填充+描边)。
- [x] 状态变化回调。

## 主要文件

| 名字       | 摘要       |
| ---------- | ---------- |
| ArcSeekBar | 圆弧形状。 |

### 1. 基本用法



```xml
<com.gcssloop.widget.ArcSeekBar
    android:id="@+id/arc_seek_bar"
    android:layout_width="280dp"
    android:layout_height="280dp"
    app:arc_colors="@array/arc_colors_default"
    app:arc_max="100"
    app:arc_open_angle="90"
    app:arc_progress="0"
    app:arc_rotate_angle="90"
    app:arc_thumb_color="#fff"
    app:arc_thumb_mode="STROKE"
    app:arc_thumb_radius="16dp"
    app:arc_thumb_width="3dp"
    app:arc_width="40dp"/>
```

### 2. 配置属性

可以在布局文件中配置的基本属性如下：

| 属性名称         | 摘要                                    | 是否必须设置 | 类型  |
| ---------------- | --------------------------------------- | ------------ | ----- |
| arc_width        | 圆弧的宽度                              | 否           | dp    |
| arc_open_angle   | 圆弧开口的角度                          | 否           | int   |
| arc_rotate_angle | 旋转角度，默认90                        | 否           | int   |
| arc_colors       | 圆弧渐变色                              | 否           | array |
| arc_max          | 进度最大数值                            | 否           | int   |
| arc_progress     | 默认进度                                | 否           | int   |
| arc_thumb_width  | 拖动按钮描边宽度                        | 否           | dp    |
| arc_thumb_color  | 拖动按钮颜色                            | 否           | color |
| arc_thumb_radius | 拖动按钮半径                            | 否           | dp    |
| arc_thumb_mode   | 拖动按钮模式(FILL、STROKE、FILL_STROKE) | 否           | emnu  |

### 3. 属性简介

#### 3.1 圆弧相关属性



### 4.添加方法

#### 4.1 添加仓库

在项目的 `build.gradle` 文件中配置仓库地址。

```groovy
allprojects {
    repositories {
        jcenter()
        // 私有仓库地址
       maven { url "http://lib.gcssloop.com:8081/repository/gcssloop-central/" }
    }
}
```

#### 4.2 添加项目依赖

在需要添加依赖的 Module 下添加以下信息，使用方式和普通的远程仓库一样。

```groovy
implementation 'com.gcssloop.widget:arc-seekbar:1.0.0'
```

## 作者简介

#### 作者微博: [@GcsSloop](http://weibo.com/GcsSloop)

#### 个人网站: http://www.gcssloop.com

<a href="http://www.gcssloop.com/info/about/" target="_blank"> <img src="http://ww4.sinaimg.cn/large/005Xtdi2gw1f1qn89ihu3j315o0dwwjc.jpg" width="300"/> </a>

## 更新日志

#### v1.0.0

允许对每一个角分别设置半径。  
支持padding。

## 版权信息

```
Copyright (c) 2018 GcsSloop

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```