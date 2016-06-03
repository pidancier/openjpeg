#JavaOpenJPEG/CMakeLists.txt

# First thing define the common source:
set(openjpegjni_SRCS
  OpenJPEGJavaDecoder.c
  OpenJPEGJavaDecodeHeader.c
  OpenJPEGJava.c
  ${OPENJPEG_SOURCE_DIR}/src/bin/common/opj_getopt.c
  ${OPENJPEG_SOURCE_DIR}/src/bin/common/color.c

  )

# JNI binding:
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# required header file:
include_directories(
  ${OPENJPEG_SOURCE_DIR}/src/lib/openjp2
  ${OPENJPEG_BINARY_DIR}/src/bin/common # opj_config.h
  ${OPENJPEG_BINARY_DIR}/src/lib/openjp2
  ${OPENJPEG_SOURCE_DIR}/src/bin/common
  ${OPENJPEG_SOURCE_DIR}/src/bin/jp2

  )

add_library(openjpegjni MODULE
  ${openjpegjni_SRCS}
  )
# Java module should not have a SONAME:
set_property(TARGET openjpegjni PROPERTY NO_SONAME 1)

# FIXME (need to use old API):
if(BUILD_MJ2)
  target_link_libraries(openjpegjni openmj2)
endif()
target_link_libraries(openjpegjni openjp2)
target_link_libraries(openjpegjni
  ${PNG_LIBNAME} ${TIFF_LIBNAME} ${LCMS_LIBNAME} ${Z_LIBNAME}
)
if(UNIX)
  target_link_libraries(openjpegjni m)
endif()

install(TARGETS openjpegjni
  EXPORT OpenJPEGTargets
  LIBRARY DESTINATION ${OPENJPEG_INSTALL_JNI_DIR} COMPONENT Libraries
)
################################################################################
if(UNIX)
  add_custom_command(
    TARGET openjpegjni
    POST_BUILD
    COMMAND echo "Re-compile library openjpegjni..."
    COMMAND cp ${OPENJPEG_BINARY_DIR}/bin/libopenjp2.so ${OPENJPEG_BINARY_DIR}/wrapping/java/openjp2
    COMMAND cc -fPIC -shared
      -o ${OPENJPEG_BINARY_DIR}/bin/libopenjpegjni.so
      CMakeFiles/openjpegjni.dir/OpenJPEGJavaDecoder.c.o
      CMakeFiles/openjpegjni.dir/OpenJPEGJava.c.o
      CMakeFiles/openjpegjni.dir/__/__/__/src/bin/common/opj_getopt.c.o
      CMakeFiles/openjpegjni.dir/__/__/__/src/bin/common/color.c.o
      libopenjp2.so
      -lpng -lz -ltiff -llcms2 -lz -lm -ltiff -llcms2 -lm -Wl,-rpath,/home/lambert/lib:.
  )
endif()
