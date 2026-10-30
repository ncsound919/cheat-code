cmake_minimum_required(VERSION 3.22)
project(propslab LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The Android Gradle plugin configures this build as Debug, and Debug compiles everything
# at -O0 unless told otherwise. Inference at -O0 is roughly an order of magnitude slower,
# so every native source here — llama.cpp and ggml included — is compiled optimised while
# keeping the debug information.
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O2")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O2")

# llama.cpp lives outside the app module so it never becomes part of the APK's source set.
# Nothing from it is installed: only the shared library below is packaged.
set(LLAMA_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../vendor/llama.cpp" CACHE PATH "llama.cpp checkout")
if (NOT EXISTS "${LLAMA_PATH}/CMakeLists.txt")
    message(FATAL_ERROR "llama.cpp was not found at ${LLAMA_PATH}. Clone it next to this workspace, or set LLAMA_PATH.")
endif ()

set(LLAMA_STANDALONE OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_CURL OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)

# CPU only. The phone is the accelerator it has; anything else would be a claim this app
# cannot make.
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_subdirectory(${LLAMA_PATH} llama_cpp_build)

add_library(propslab_llama SHARED llama_jni.cpp)
target_include_directories(propslab_llama PRIVATE ${LLAMA_PATH}/include)
find_library(LOG_LIB log)
target_link_libraries(propslab_llama PRIVATE llama ${LOG_LIB})
target_compile_options(propslab_llama PRIVATE -O2 -fvisibility=hidden)

# Play requires native libraries to be 16 KB page-size aligned. NDK r26 (pinned
# above) does not default to it — r28+ does — so the linker is told explicitly,
# the same flag NC Phone Beat ships with.
target_link_options(propslab_llama PRIVATE "-Wl,-z,max-page-size=16384")
