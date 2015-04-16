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
        ...
        compile 'com.iobeam:iobeam-client-java:0.2'
    }

It is also available on Maven Central.

## Overview ##

This library allows Java clients to send data to the iobeam Cloud. 

At a high-level, here's how it works:

1. Initialize an `Iobeam` object with your `project_id` and `project_token`

1. Register your device to get an auto-generated `device_id`. Optionally, you can initialize the
 object with a `device_id` in the previous step and skip this step

1. Create a `DataPoint` object for each time-series data point

1. Add the `DataPoint` under your `series_name` (e.g., "temperature")

1. When you're ready, send your data to the iobeam Cloud 


## Getting Started ##

Here's how to get started, using a basic example that sends temperature data to iobeam.
(For simplicity, let's assume that the current temperature can be accessed
with `getTemperature()`).

(Reminder: Before you start, create a user account, project, and project_token (with write access) 
using the iobeam APIs or Command-line interface. Write down your new `project_id` and `project_token`.)

### iobeam Initialization ###

There are several ways to initialize the `Iobeam` library. All require that you have `project_id`
and `project_token` before hand.

**Without a pre-known `device_id`**

If you have not pre-created a `device_id`, you'll need to register one:

    Iobeam iobeam = new Iobeam(PATH, PROJECT_ID, PROJECT_TOKEN);
    if (iobeam.getDeviceId() == null)
        iobeam.registerDeviceAsync(null); // Registers using auto-generated device_id

The `device_id` will be saved to disk at the path `PATH`. On Android, this would be set to something
like `this.getFilesDir().getAbsolutePath()`, which is internal storage for applications. On future
calls, this on-disk storage will be read first. Therefore we check whether the ID is set before
registering a new ID.

**With a known `device_id`**

If you have created a `device_id` (e.g. using our [CLI](https://github.com/iobeam/iobeam)), you can pass this in the constructor
and skip the registration step.

    Iobeam iobeam = new Iobeam(PATH, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);

**Advanced: not saving to disk**

If you don't want the `device_id` to be automatically stored for you, set the `path` parameter in
either constructor to be `null`:

    Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN);  // without known id
    Iobeam iobeam = new Iobeam(null, PROJECT_ID, PROJECT_TOKEN, DEVICE_ID);  // with known id

This is useful for cases where you want to persist the ID yourself (e.g. in a settings file), or if
you are making `Iobeam` objects that are temporary. For example, if the device you are using acts
as a relay or proxy for other devices, it could get the `device_id` from those devices and have
no need to save it.

### Tracking Time-series Data ###

For each time-series data point, create a `DataPoint` object:

    double t = getTemperature();
    DataPoint d = new DataPoint(t);

    // DataPoint d = new DataPoint(System.currentTimeInMillis(), t); is equivalent to above

(The timestamp provided should be in milliseconds since epoch. The value can be integral or real.)

Now, pick a name for your data series (e.g., "temperature"), and add the `DataPoint` under that 
series:

    iobeam.addData("temperature", d);

Here's another example that takes and stores a temperature reading every second:

    while (true) {
        DataPoint d = new DataPoint(getTemperature());
        iobeam.addData("temperature", d);
        Thread.sleep(1000);
    }

Note that the `Iobeam` object can hold several series at once. For example, 
if you also had a `getHumidity()` function, you could add both data points to the same
`Iobeam`:

    while (true) {
        DataPoint dt = new DataPoint(getTemperature());
        DataPoint dh = new DataPoint(getHumidity());

        iobeam.addData("temperature", dt);
        iobeam.addData("humidity", dh);

        Thread.sleep(1000);
    }


### Connecting to the iobeam Cloud ###

You can send your data to the iobeam Cloud in two ways: synchronously and asynchronously:

    iobeam.send(); // blocking
    iobeam.sendAsync(); // non-blocking

If there are problems with the
data as provided, an `ApiException` is thrown (e.g. incorrect project ID or device ID, invalid data,
etc). `IOException` is thrown in the case of network connectivity issues.


### Full Example ###

Here's the full source code for our example:

    // Initialization
    try {
        Iobeam iobeam = new Iobeam(PATH, PROJECT_ID, PROJECT_TOKEN);
        if (iobeam.getDeviceId() == null)
            iobeam.registerDeviceAsync(null); // Registers using auto-generated device_id
    } catch (ApiException e) {
        e.printStackTrace();
    }

    ...

    // Data gathering
    DataPoint dt = new DataPoint(getTemperature());
    DataPoint dh = new DataPoint(getHumidity());

    iobeam.addData("temperature", dt);
    iobeam.addData("humidity", dh);

    ...

    // Data transmission
    try {
        iobeam.sendAsync();
    } catch (ApiException e) {
        e.printStackTrace();
    }


These instructions should hopefully be enough to get you started with the library!
