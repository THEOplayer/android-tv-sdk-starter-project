# THEOplayer Android TV SDK - Starter project

## License

This projects falls under the license as defined in https://github.com/THEOplayer/license-and-disclaimer.

## Getting Started

This Android project is an example how to integrate [THEOplayer](https://www.theoplayer.com) into an Android TV app.
There is a step-by-step [guide](https://support.theoplayer.com/hc/en-us/articles/360000779729-Android-Starter-Guide) for this project, we suggest you to follow it for better insights.

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* Download and install Android Studio. 
* Obtain a THEOplayer [Android TV SDK](https://portal.theoplayer.com/register) license. 
If you don't have a license yet, contact your sales contact or email us at [support@theoplayer.com](mailto:support@theoplayer.com).

### Include THEOplayer Android TV SDK in the project

Once you obtained the license, you need copy it into the ``` app/libs ``` folder of the project.

### Link your SDK file in ```build.gradle```

In the module-level ```build.gradle``` file (```app/build.gradle```) use the proper name of your SDK file

```java
dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation files('libs/theoplayer.aar')

    def leanback_version = "1.0.0"
    implementation 'com.google.code.gson:gson:2.8.2'
    implementation "androidx.leanback:leanback:$leanback_version"
    implementation 'androidx.constraintlayout:constraintlayout:2.0.0-rc1'
    implementation 'androidx.appcompat:appcompat:1.1.0-rc01'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test.ext:junit:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'
    
}
```

* Note: For example if the SDK file is called ```theoplayer.aar```, then your ```build.gradle``` file should look like above.



## Build the project

### In Android Studio

Just open the project, let Android Studio to install the dependencies and build the project.

### With Gradle

In a terminal navigate to the project folder and run:

```
./gradle assembleDebug
```

The generated APK file (what you need to install on your device) will be available in the ```app/build/outputs/apk/``` folder.
