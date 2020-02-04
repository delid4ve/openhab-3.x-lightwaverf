# lightwaverf Binding

Provides a link between openHab and LightwaveRf through their Api.

## Supported Things

Devices supported:
    Tested: Sockets (gen2)
    Untested: Dimmers, Thermostats, Linkplus, Sockets(gen1)

## Discovery

All devices are avilable for auto discovery.

## Binding Configuration

Add a lightwave account thing and configure your email and password for your online account.
Polling interval can also be configured under the account thing.  
** Due to no streaming data service or working large batch responses, the actual polling interval is: 
pollinginterval multiplied by ((number of linked items / 30) rounded up to the next integer))

## Thing Configuration

_ToDo

## Channels

_Todo
| channel  | type   | description                  |
|----------|--------|------------------------------|
| contr    | Switch | This is the control channel  |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._

## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
