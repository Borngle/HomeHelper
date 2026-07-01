# HomeHelper
**HomeHelper** is an Android-based application developed in Java aimed at helping users manage their home environment.

## Table of Contents
* [Description](#description)
* [Requirements](#requirements)
* [Usage](#usage)

## Description
**HomeHelper** works by setting up a 'sensor node' in a room, which connects through home Wi-Fi to the application and, through smart interpretation of onboard sensor data, will send configurable notifications to the user's phone which recommend certain actions to help regulate the environment of the room the node belongs to.

A sensor node is a breadboard-based, Raspberry Pi Pico W-controlled device that records ambient room temperature, humidity, light level, and motion and sends real-time updates to the application to be processed and reported through notifications. 

### Example Application Recommendations: ###
- Heating:
  - Room is getting cold; turn heating on?
  - It's getting warm; consider opening a window or turning on a fan
  - Heating has been left on in an empty room; consider turning it off
  - You have exceeded your daily heating limit
  - Outside temperature is moderate and heating is on; turn heating off?
- Ventilation:
  - Humidity is high (risk of mould); consider opening a window or turning on a dehumidifer
  - Air is very dry; consider turning on a humidifier
- Lights:
  - It's dark in this room during the day; consider opening the blinds
  - Lights have been left on in an empty room; consider turning them off
  - Lights are on and it's bright outside; consider turning them off
  - Sensor node obstructed

The user will be able to set a heating usage limit in their settings and has the option to turn specific types of notifications on/off if they desire.

## Requirements
- Android device
- Functioning sensor node(s)

## Usage
Place the sensor node in an open area with an optimal 'view' of the room (it is important it is unobstructed to obtain accurate readings). Power the node with a 9V battery, and the built-in LCD will display some recorded metrics as well as the Wi-Fi connection status. Open the application on an Android device connected to the same Wi-Fi as the node, and its status and recorded metrics will be displayed. There are settings that can be configured globally as well as per node.
