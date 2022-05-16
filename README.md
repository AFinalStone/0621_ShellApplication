## 一、项目主要包含Dex加密和Res加密两大块

### 1.1 dex文件的加密

- dex文件的加密逻辑主要在Proxy_Guard_Tools模块的Main_Dex类中
- dex文件的解密逻辑主要在Proxy_Guard_Core模块的ProxyApplicationDex类中

![Dex加固的方案](picture/01_加固的方案.png)

![Dex加固的总体框架](picture/02_加固的整个流程.png)

### 1.2 res文件的加密

- res文件的加密逻辑主要在Proxy_Guard_Tools模块的Main_Res类中
- res文件的解密逻辑主要在Proxy_Guard_Core模块的ProxyApplicationRes类中

## 一、加固常见的方案和手段

- 反模拟器
- 虚拟机保护
- Dex文件加密

## 二、需要用到的原理

### 1.1 Framework层Application的启动流程

### 1.2 APK的打包，签名流程

### 1.3 Dex文件结构解析

### 1.4 Dex文件的加载流程，ClassLoader类加载器的加载原理

### 1.5 Resource.arsc文件结构解析

### 1.6 Res资源文件的加载流程，Resource和AssetManager的加载原理

### 1.7 反射的使用，通过反射hook系统层类对象

### 1.8 自定义加固和热修复的区别，热修复的原理，bisdiff算法产生差分包