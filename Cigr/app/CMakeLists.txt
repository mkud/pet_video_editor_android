# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library( avcodec-57 SHARED IMPORTED )
set_target_properties( # Specifies the target library.
                       avcodec-57

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libavcodec-57.so )

add_library( avutil-55 SHARED IMPORTED )
set_target_properties( # Specifies the target library.
                       avutil-55

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libavutil-55.so )

add_library( avformat-57 SHARED IMPORTED )
set_target_properties( # Specifies the target library.
                       avformat-57

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libavformat-57.so )

add_library( libswscale-4 SHARED IMPORTED )
set_target_properties( # Specifies the target library.
                       libswscale-4

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libswscale-4.so )

add_library( opencv SHARED IMPORTED )
set_target_properties( # Specifies the target library.
                       opencv

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_java3.so )

include_directories( jniLibs/include/ )


add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/Encoder.cpp
             src/main/cpp/Decoder.cpp
             src/main/cpp/CGlobalBuffer.cpp
             src/main/cpp/CMatFrame.cpp
             src/main/cpp/PartialStabilizer.cpp
             src/main/cpp/vs.cpp
             src/main/cpp/native-lib.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                       avutil-55
                       avcodec-57
                       avformat-57
                       libswscale-4
                       opencv

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )