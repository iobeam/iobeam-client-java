# iobeam Java / Android Library

**[iobeam](http://iobeam.com)** is a data platform for connected devices.

This is a Java library for sending data to **iobeam**, e.g., from within an
Android app.
For more information on iobeam, please read our [full API
documentation](http://docs.iobeam.com).

*Please note that we are currently invite-only. You will need an invite
to generate a valid token and use our APIs. (Sign up [here](http://iobeam.com) for an invite.)*

## Sample apps

We've written a couple sample Android apps to illustrate how to use this library:

1. [Android Battery Data App](https://github.com/iobeam/sample-android-battery-data) -
Basic example that tracks the current battery level on your phone. Every time the battery level
changes by more than 1%, the app uploads the timestamp and current level to iobeam.

1. [Android WiFi RSSI App](https://github.com/iobeam/sample-android-wifi-rssi) -
Slightly more advanced example that uses Callbacks. Measures the signal strength of the WiFi on your phone using RSSI
(received signal strength indicator). Measurements are taken every 20 seconds, and are uploaded to iobeam in
batches of 3 or more measurements.

## Before you start

Before you can start sending data to iobeam, you'll need a `project_id` and
`project_token` (with write-access enabled) for a valid **iobeam** account.
You can get these easily with our
[Command-line interface tool](https://github.com/iobeam/iobeam).


## Installation

To install to your local Maven repository:

    git clone https://github.com/iobeam/iobeam-client-java.git
    cd iobeam-client-java
    mvn install

It will be installed as artifact ```iobeam-client-java``` under the group ```com.iobeam```.

If you are building an Android app, add the following lines to your `app/build.gradle` file:

    repositories {
        ...
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        ...
        compile('com.iobeam:iobeam-client-java:0.5.1') {
            exclude module: 'json'
        }
    }

It is also available on Maven Central.

## Overview

This library allows Java clients to send data to iobeam.

At a high-level, here's how it works:

1. Initialize an `Iobeam` object with your `project_id` and `project_token`

1. Register your device to get an auto-generated `device_id`. Optionally, you can initialize the
 object with a `device_id` in the previous step and skip this step

1. Create a `DataBatch` for storing data by streams and have the `Iobeam` object
 track it.

1. Add data values to the `DataBatch` as you get them.

1. When you're ready, send your data to iobeam.


## Getting Started

Here's how to get started, using a basic example that sends temperature data to iobeam.
(For simplicity, let's assume that the current temperature can be accessed
with `getTemperature()`).

(Reminder: Before you start, create a user account, project, and project_token (with write access)
using the iobeam APIs or Command-line interface. Write down your new `project_id` and `project_token`.)

### iobeam Initialization

There are several ways to initialize the `Iobeam` library. All require that you have `project_id`
and `project_token` before hand.

**Without a registered `device_id`**

If you have not previously registered a `device_id` with iobeam, either via the CLI or our website,
you will need to register one in code. There are two ways to register a `device_id`:

(1) Let iobeam generate one for you:

```java
Iobeam iobeam = new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN).saveIdToPath(PATH).build();
iobeam.registerDeviceAsync();
```

(2) Provide your own (must be unique to your project):

```java
Iobeam iobeam = new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN).saveIdToPath(PATH).build();
iobeam.registerDeviceWithIdAsync("my_desired_device_id");
```

The `device_id` will be saved to disk at the path `PATH`. On Android, this would be set to something
like `this.getFilesDir().getAbsolutePath()` , which is internal storage for applications. On future
calls, this on-disk storage will be read first. If a `device_id` exists, the
`registerDeviceAsync()` will do nothing; otherwise, it will get a new random ID from us. If you
provide a _different_ `device_id` to `registerDeviceWithIdAsync()`, the old one will be replaced.

**With a registered `device_id`**

If you have registered a `device_id` (e.g. using our [CLI](https://github.com/iobeam/iobeam)),
you can pass this in the constructor and skip the registration step.

```java
Iobeam iobeam = new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN).saveIdToPath(PATH)
    .setDeviceId(DEVICE_ID).build();
```

You *must* have registered some other way (CLI, website, previous installation, etc) for this to
work.

**Advanced: not saving to disk**

If you don't want the `device_id` to be automatically stored for you, set the `path` parameter in
either constructor to be `null`:

```java
// Without registered id:
Iobeam iobeam = new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN).build();

// With registered id:
Iobeam iobeam = new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN)
    .setDeviceId(DEVICE_ID).build();
```

This is useful for cases where you want to persist the ID yourself (e.g. in a settings file), or if
you are making `Iobeam` objects that are temporary. For example, if the device you are using acts
as a relay or proxy for other devices, it could get the `device_id` from those devices and have
no need to save it.

### Tracking Time-series Data

To track time-series data, you need to decide how to break down your data
streams into "batches", a collection of data streams grouped together. You
create a `DataBatch` with a list of stream names that the batch contains.
So if you're tracking just temperature in a batch:

```java
DataBatch batch = new DataBatch(new String[]{"temperature"});
iobeam.trackDataBatch(batch);
```

Then for every data point, you'll want to add it to the batch with a timestamp
when the measurement occurred:

```java
long timestamp = System.currentTimeInMillis();
batch.add(timestamp, new String[]{"temperature"}, new Object[]{getTemperature()});
```

You pass in the values keyed by which column they belong to. In the above format
you do that by providing an array of column names and an equal size `Object` array
of corresponding values. You can create a `Map` that maps columns/streams to
 values:

```java
long timestamp = System.currentTimeInMillis();
Map<String, Object> values = new HashMap<String, Object>();
values.put("temperature", getTemperature());
batch.add(timestamp, values);
```

Note that the `DataBatch` object can hold several streams at once. For
example, if you also had a `getHumidity()` function, you could track both in
the same `DataBatch`:

```java
String[] columns = new String[]{"temperature", "humidity"};
DataBatch batch = new DataBatch(columns);
iobeam.trackDataBatch(batch);

long timestamp = System.currentTimeMillis();
Object[] values = new Object[2];
values[0] = getTemperature();
values[1] = getHumidity();
batch.add(timestamp, columns, values);
```

Not every `add()` call needs all streams to have a value; if a stream is omitted
from both arrays (or from the keys of a `Map`), it will be assumed to be `null`.


### Connecting to iobeam

You can send your data to iobeam in two ways: synchronously and asynchronously:

    iobeam.send(); // blocking
    iobeam.sendAsync(); // non-blocking

If there are problems with the
data as provided, an `ApiException` is thrown (e.g. incorrect project ID or device ID, invalid data,
etc). `IOException` is thrown in the case of network connectivity issues.


### Full Example

Here's the full source code for our example:
```java
// Initialization
try {
    Iobeam iobeam = new Iobeam.Builder(PROJECT_ID, PROJECT_TOKEN).saveIdToPath(PATH).build();

    if (iobeam.getDeviceId() == null)
        iobeam.registerDeviceAsync(null); // Registers using auto-generated device_id
} catch (ApiException e) {
    e.printStackTrace();
}

...

// Data gathering
String[] columns = new String[]{"temperature", "humidity"};
DataBatch batch = new DataBatch(columns);
iobeam.trackDataBatch(batch);

long timestamp = System.currentTimeInMillis();
Object[] values = new Object[2];
values[0] = getTemperature();
values[1] = getHumidity();
batch.add(timestamp, columns, values);

...

// Data transmission
try {
    iobeam.sendAsync();
} catch (ApiException e) {
    e.printStackTrace();
}
```
These instructions should hopefully be enough to get you started with the library!
