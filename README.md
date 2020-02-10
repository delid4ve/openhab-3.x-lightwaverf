# lightwaverf Binding  ![Lightwave RF](logo.png)


This binding integrates Lightwave RF's range of smart devices. (https://lightwaverf.com/).
A registered account is required with Lightwave Rf in order to use the binding.


## Supported Things

The Link Plus or previous generation 1 hub is required as a gateway between devices.
The current 'Smart Series' (gen2) equipment operates on 867mHz and is only accessible throught the provided api, generation 1 equipment is also integrated through this api.

Devices supported currently include:
Generation 2 Sockets and Dimmers.
Generation 1 Socket(single), Thermostat and energy Monitor.


| Device type              | Generation       | ThingType |
|--------------------------|------------------|------------|
| Socket (1 way)           | 1                | s11        |
| Socket (2 way)           | 2                | s22        |
| Dimmer (1 way)           | 2                | d21        |
| Dimmer (2 way)           | 2                | d22        |
| Dimmer (3 way)           | 2                | d23        |
| Dimmer (4 way)           | 2                | d24        |
| Thermostat               | 1                | t11        |
| Energy Monitor           | 1                | e11        |

A single 'Thing' is used for each device type, with channels being allocated into groups.  Ie, a 2 way socket will have 2 channel groups, each containing the relavent controls.

## Discovery

All devices are avilable for auto discovery.
For any other devices not supported, please add these to the binding where they will be discovered as an unknown device.
A list of supported channels will be generated under the 'Thing' properties in order for them to be added to the binding, please submit this list via github.

## Binding Configuration

Add a lightwave account thing and configure your email and password for your online account.
Additional Properties:
Refresh interval: Frequency to get updates from the api.
Number of items to fetch at once: Due to limitations with the api, polling has to be split into groups, this defines how many channels will be fetched at once.
If the amount of channels goes above this, a further fetch will be initiated resulting in an icreased poll time. (refreshinterval x groups = new polling time).
  


## Thing Configuration

The initial configuration is as follows:

| Bridge lightwaverf:lightwaverfaccount:anyname [ username="example@microsoft.com", password="password" ] |

## Devices

Devices are identified by the number between - and - in the deviceId.  This generally starts at 1 for each account.  DeviceId's are not visible in the lightwave app but can be found by running discovery.

Things can be added to a bridge enclosed in {} as follows:

|ThingType UniqueThingName	"name" @ "group" [ sdId="simplifieddeviceId" ] |

| Device type              | Generation       | ThingType |
|--------------------------|------------------|------------|



## Channels

channels can be assigned as follows:

|{ channel="lightwaverf:thingType:anyname:sdId:channelgroup#channel" }|

Therefore a typical layout for a 2 way socket would look as follows:

|Switch  Socket_LeftSwitch  { channel="lightwaverf:s22:anyname:1:1#switch" }|
|Switch  Socket_RightSwitch  { channel="lightwaverf:s22:anyname:1:2#switch" }|

Full list of channels to be added soon.

