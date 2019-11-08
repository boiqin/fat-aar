# fat-aar-android

该插件是在kezong和TangHuaiZhe的基础上进行kotlin重写，方便日后维护，有使用方面的问题也可直接向我提出
该插件提供了将library以及它依赖的module一起打包成一个完整aar的解决方案，支持gradle
plugin 3.0.1及以上。（目前测试的版本范围是gradle plugin 3.0.1 -
3.4.2，gradle 4.6 - 5.4.1）

## 如何使用

#### 第一步: Apply plugin

添加以下代码到你工程根目录下的`build.gradle`文件中:

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:xxx'
        classpath 'com.boiqin:fat-aar:1.0.3'
    }
}
```

添加以下代码到你的主library的`build.gradle`中:

```gradle
apply plugin: 'com.boqin.fat-aar'
```

#### 第二步: Embed dependencies
- 将`implementation`或者`api`改成`embed`

代码所示：
```gradle
dependencies {
    implementation fileTree(dir: 'libs', include: '*.jar')
    // java dependency
    embed project(path: ':lib-java', configuration:'default')
    // aar dependency
    embed project(path: ':lib-aar', configuration:'default')
    // aar dependency
    embed project(path: ':lib-aar2', configuration:'default')
    // local full aar dependency
    embed project(path: ':lib-aar-local', configuration:'default')
    // local full aar dependency
    embed (name:'lib-aar-local2',ext:'aar')
    // remote jar dependency
    embed 'com.google.guava:guava:20.0'
    // remote aar dependency
    embed 'com.facebook.fresco:fresco:1.11.0'
    // don't want to embed in
    // 不建议使用implementation，因为该依赖可能与application的依赖版本不一致，使用implementation可能会导致R类找不到的问题
    compileOnly 'com.android.support:appcompat-v7:27.1.1'
}
```

### 多级依赖

#### 本地依赖

如果你想将本地所有相关的依赖项全部包含在最终产物中，你需要在你主library中对所有依赖都加上`embed`关键字

比如，mainLib依赖lib1，lib1依赖lib2，如果你想将所有依赖都打入最终产物，你必须在mainLib的`build.gradle`中对lib1以及lib2都加上`embed`关键字

#### 远程依赖

如果你想将所有远程依赖在pom中声明的依赖项同时打入在最终产物里的话，你需要在`build.gradle`中将`embed`的transitive值改为true，例如：
```gradle
// the default value is false
// invalid for local aar dependency
configurations.embed.transitive = true
```

如果你将transitive的值改成了true，并且想忽略pom文件中的某一个依赖项，你可以添加`exclude`关键字，例如：
```gradle
embed('com.facebook.fresco:fresco:1.11.0') {
    exclude(group:'com.facebook.soloader', module:'soloader')
}
```

## 关于 AAR 文件
AAR是Android提供的一种官方文件形式；
该文件本身是一个Zip文件，并且包含Android里所有的元素；
可以参考 [aar文件详解][2].

**支持功能列表:**

- [x] 支持library以及module中含有flavor
- [x] AndroidManifest合并
- [x] classes以及jar合并
- [x] res合并
- [x] assets合并
- [x] jni合并
- [x] R.txt合并
- [x] R.class合并
- [ ] proguard合并（混淆合并现在看来有些问题，建议将所有混淆文件都写在主Library中）

## Gradle版本支持

| Version | Gradle Plugin |
| :--------: | :--------:|
| 1.0.1 | 3.1.0 - 3.2.1 |
| 1.1.6 | 3.1.0 - 3.4.1 |
| 1.1.10| 3.0.1 - 3.4.1 |
| 1.2.6 | 3.0.1 - 3.5.0 |
  
  
## 常见问题

* **混淆日志：** 当开启proguard时，可能会产生大量的`Note: duplicate definition of library class`日志，如果你想忽略这些日志，你可以在`proguard-rules.pro`中加上`-dontnote`关键字；
* **资源冲突：** 如果library和module中含有同名的资源(比如 `string/app_name`)，编译将会报`duplication resources`的相关错误，有两种方法可以解决这个问题：
  * 考虑将library以及module中的资源都加一个前缀来避免资源冲突； 
  * 在`gradle.properties`中添加`android.disableResourceValidation=true`可以忽略资源冲突的编译错误，程序会采用第一个找到的同名资源作为实际资源，不建议这样做，如果资源同名但实际资源不一样会造成不可预期的问题。
  
## 致谢
- [android-fat-aar][1]
- [fat-aar-plugin][4]
- [fat-aar-plugin][5]
- [combineAar][6]

[1]: https://github.com/adwiv/android-fat-aar
[2]: https://developer.android.com/studio/projects/android-library.html#aar-contents
[3]: https://developer.android.com/studio/releases/gradle-plugin.html
[4]: https://github.com/Vigi0303/fat-aar-plugin
[5]: https://github.com/kezong/fat-aar-android
[6]: https://github.com/TangHuaiZhe/combineAar
