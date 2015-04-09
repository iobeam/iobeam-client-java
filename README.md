# iobeam Java / Android Library

**[iobeam](http://iobeam.com)** is a data platform for connected devices. 

This is a Java library for sending data to the **iobeam Cloud**, e.g., from within an Android app.
For more information on the iobeam Cloud, please read our [full API documentation](http://docs.iobeam.com).

*Please note that we are currently invite-only. You will need an invite 
to generate a valid token and use our APIs. (Sign up [here](http://iobeam.com for an invite).)*

## Sample apps ##

We've written a couple sample Android apps to illustrate how to use this library:

1. [Android Battery Data App](https://github.com/iobeam/sample-android-battery-data) -
Basic example that tracks the current battery level on your phone. Every time the battery level
changes by more than 1%, the app uploads the timestamp and current level to iobeam.

1. [Android WiFi RSSI App](https://github.com/iobeam/sample-android-wifi-rssi) -
Slightly more advanced example that uses Callbacks. Measures the signal strength of the WiFi on your phone using RSSI
(received signal strength indicator). Measurements are taken every 20 seconds, and are uploaded to iobeam in
batches of 3 or more measurements.

## Before you start ##

Before you can start sending data to the iobeam Cloud, you'll need a `project_id` and 
`project_token` (with write-access enabled) for a valid **iobeam** account.
You can get these easily with our
[Command-line interface tool](https://github.com/iobeam/iobeam).


## Installation ##

To install to your local Maven repository:

    git clone https://github.com/iobeam/iobeam-client-java.git
    cd iobeam-client-java
    mvn install

It will be installed as artifact ```iobeam-client-java``` under the group ```com.iobeam```.

If you are building an Android app, add the following lines to your `app/build.gradle` file:

    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        compile fileTree(dir: 'libs', include: ['*.jar'])
        compile 'com.iobeam:iobeam-client-java:0.1-SNAPSHOT'
    }

## Overview ##

This library allows Java clients to send data to the iobeam Cloud. 

At a high-level, here's how it works:

1. Initialize the `Iobeam` library with your `project_id` and `project_token`

1. Register your device with an auto-generated `device_id` (you can optionally pass a `device_id` in the previous step and skip this step)

1. Create a `DataPoint` object for each time-series data point

1. Add the `DataPoint` under your `series_name` (e.g., "temperature")

1. When you're ready, send your data to the iobeam Cloud 


## Getting Started ##

Here's how to get started, using a basic example that sends temperature data to iobeam. (For simplicitly, 
let's assume that the current temperature can be accessed with `getTemperature()`).

(Reminder: Before you start, create a user account, project, and project_token (with write access) 
using the iobeam APIs or Command-line interface. Write down your new `project_id` and `project_token`.)

### Tracking Time-series Data ###

First, initialize the `Iobeam` library with your `project_id` and `project_token`, and register the device:

    Iobeam.init(this.getFilesDir().getAbsolutePath(), PROJECT_ID, PROJECT_TOKEN);
    Iobeam.registerDeviceAsync(null); // Registers using auto-generated device_id

*Alternately, you can register your device with a specific `device_id` and combine these two steps as follows:*

    Iobeam.init(this.getFilesDir().getAbsolutePath(), PROJECT_ID, PROJECT_TOKEN, DEVICE_ID); // Registers using DEVICE_ID

*Note that the `device_id` must be **globally-unique** and **at least 16 characters long**.*

Next, create a `DataPoint` object for each time-series data point:

    double t = getTemperature();
    DataPoint d = new DataPoint(t);

    // DataPoint d = new DataPoint(System.currentTimeInMillis(), t); is equivalent to above

(The timestamp provided should be in milliseconds since epoch. The value can be integral or real.)

Now, pick a name for your data series (e.g., "temperature"), and add the `DataPoint` under that 
series:

    Iobeam.addData("temperature", d);

Here's another example that takes and stores a temperature reading every second:

    while (true) {
        DataPoint d = new DataPoint(getTemperature());
        Iobeam.addData("temperature", d);
        Thread.sleep(1000);
    }

Note that the `Iobeam` object can hold several series at once. For example, 
if you also had a `getHumidity()` function, you could add both data points to the same
`Import`:

    while (true) {
        DataPoint dt = new DataPoint(getTemperature());
        DataPoint dh = new DataPoint(getHumidity());

        Iobeam.addData("temperature", dt);
        Iobeam.addData("humidity", dh);

        Thread.sleep(1000);
    }


### Connecting to the iobeam Cloud ###

You can send your data to the iobeam Cloud in two ways: synchronously and asynchronously:

    Iobeam.send(); // blocking
    Iobeam.sendAsync(); // non-blocking

If there are problems with the
data as provided, an `ApiException` is thrown (e.g. incorrect project ID or device ID, invalid data,
etc). `IOException` is thrown in the case of network connectivity issues.


### Full Example ###

Here's the full source code for our example:

    // Initialization
    try {
        Iobeam.init(this.getFilesDir().getAbsolutePath(), PROJECT_ID, PROJECT_TOKEN);
        Iobeam.registerDeviceAsync(null); // Registers using auto-generated device_id
    } catch (ApiException e) {
        e.printStackTrace();
    }

    ...

    // Data gathering
    DataPoint dt = new DataPoint(getTemperature());
    DataPoint dh = new DataPoint(getHumidity());

    Iobeam.addData("temperature", dt);
    Iobeam.addData("humidity", dh);

    ...

    // Data transmission
    try {
        Iobeam.sendAsync();
    } catch (ApiException e) {
        e.printStackTrace();
    }


These instructions should hopefully be enough to get you started with the library!