################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../src/CGlobalBuffer.cpp \
../src/CMatFrame.cpp \
../src/Decoder.cpp \
../src/Encoder.cpp \
../src/FullStabilizer.cpp \
../src/PartialStabilizer.cpp \
../src/vs.cpp 

OBJS += \
./src/CGlobalBuffer.o \
./src/CMatFrame.o \
./src/Decoder.o \
./src/Encoder.o \
./src/FullStabilizer.o \
./src/PartialStabilizer.o \
./src/vs.o 

CPP_DEPS += \
./src/CGlobalBuffer.d \
./src/CMatFrame.d \
./src/Decoder.d \
./src/Encoder.d \
./src/FullStabilizer.d \
./src/PartialStabilizer.d \
./src/vs.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: Cross G++ Compiler'
	g++ -I/usr/local/include -O3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@)" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


