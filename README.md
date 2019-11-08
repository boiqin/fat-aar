# fat-aar-android

- [中文文档](./README_CN.md)

The solution of merging aar works with [the android gradle plugin][3], the android plugin's version of the development is `3.0.1` and higher. (Tested in gradle plugin 3.0.1 - 3.4.2, and gradle 4.6 - 5.5)

## Getting Started

### Step 1: Apply plugin

Add snippet below to your root build script file:

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

Add snippet below to the `build.gradle` of your android library:

```gradle
apply plugin: 'com.boiqin.fat-aar'
```

### Step 2: Embed dependencies

change `implementation` or `api` to `embed` while you want to embed the dependency in the library. Like this:

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
    // implementation is not recommended because the dependency may be different with the version in application, resulting in the R class not found.
    compileOnly 'com.android.support:appcompat-v7:27.1.1'
}
```

### Transitive

#### Local Dependency
If you want to including local transitive dependencies in final artifact, you must add `embed` for transitive dependencies in your main library. 

For example, mainLib depend on subLib1, subLib1 depend on subLib2, If you want including all dependencies in final artifact, you must add `embed` for subLib1 and subLib2 in mainLib `build.gradle`

#### Remote Dependency
If you want to including all remote transitive dependencies which in pom file, you need change the `embed`'s transitive value to true in your `build.gradle`, like this:
```gradle
// the default value is false
// invalid for local aar dependency
configurations.embed.transitive = true
```
If you change the transitive value to true,and want to ignore a dependency in its POM file, you can add exclude keywords, like this:
```gradle
embed('com.facebook.fresco:fresco:1.11.0') {
    exclude(group:'com.facebook.soloader', module:'soloader')
}
```

**More usage see [example](./example).**

## About AAR File

AAR is a file format for android library.
The file itself is a zip file that containing useful stuff in android.
See [anatomy of an aar file here][2].

**support list for now:**

- [x] productFlavors
- [x] manifest merge
- [x] classes jar and external jars merge
- [x] res merge
- [x] assets merge
- [x] jni libs merge
- [x] proguard.txt merge
- [x] R.txt merge
- [x] R.class merge

## Gradle Version Support

| Version | Gradle Plugin |
| :--------: | :--------:|
| 1.0.1 | 3.1.0 - 3.2.1 |
| 1.1.6 | 3.1.0 - 3.4.1 |
| 1.1.10| 3.0.1 - 3.4.1 |
| 1.2.6 | 3.0.1 - 3.5.0 |


## Known Defects or Issues

- **Proguard note.** Produce lots of(maybe) `Note: duplicate definition of library class`, while proguard is on. A workaround is to add `-dontnote` in `proguard-rules.pro`.
- **The overlay order of res merge is changed:** Embedded dependency has higher priority than other dependencies.
- **Res merge conflicts.** If the library res folder and embedded dependencies res have the same res Id(mostly `string/app_name`). A duplicate resources build exception will be thrown. To avoid res conflicts:
  - consider using a prefix to each res Id, both in library res and aar dependencies if possible. 
  - Adding "android.disableResourceValidation=true" to "gradle.properties" can do a trick to skip the exception, but is not recommended.
  
## Thanks

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
