# iobeam Java / Android Library

**[iobeam](http://iobeam.com)** is a data platform for connected devices. 

This is a Java library for sending data to the **iobeam Cloud**, e.g., from within an Android app.
For more information on the iobeam Cloud, please read our [full API documentation](http://docs.iobeam.com).

*Please note that we are currently invite-only. You will need an invite 
to generate a valid token and use our APIs. (Sign up [here](http://iobeam.com for an invite).)*


## Before you start ##

Before you can start sending data to the iobeam Cloud, you'll need: an iobeam user account, project/project_id,
and project_token with write-access enabled. You can get these easily with our
[Command-line interface tool](https://github.com/iobeam/iobeam).


## Installation ##

To install to your local Maven repository:

    git clone https://github.com/iobeam/iobeam-client-java.git
    cd iobeam-client-java
    mvn install

It will be installed as artifact ```iobeam-client-java``` under the group ```com.iobeam```.


## Overview ##

This library allows Java clients to send data to the iobeam Cloud. 

At a high-level, here's how it works:

1. Create an `Import` object, which serves a collection of your data sources

1. Create a `DataPoint` object for each time-series data point

1. Add the `DataPoint` to `Import` under your `series_name` (e.g., "temperature")

1. When you're ready, send the `Import` object to the iobeam Cloud using the `Iobeam` client and `Imports` service


## Getting Started ##

Here's how to get started, using a basic example that sends temperature data to iobeam. (For simplicitly, 
let's assume that the current temperature can be accessed with `getTemperature()`).

(Reminder: Before you start, create a user account, project, and project_token (with write access) 
using the iobeam APIs or Command-line interface. Write down your new `project_id` and `project_token`.)

### Tracking Time-series Data ###

First, create an `Import` object that will serve as the collection of all your data points:

    private static final String DEVICE_ID = ...;
    private static final long PROJECT_ID = ...;

    Import imp = new Import(DEVICE_ID, PROJECT_ID);

Next, create a `DataPoint` object for each time-series data point:

    double t = getTemperature();
    DataPoint d = new DataPoint(System.currentTimeInMillis(), t);

    // DataPoint d = new DataPoint(t); is equivalent to above

(The timestamp provided should be in milliseconds since epoch. The value can be integral or real.)

Now, pick a name for your data series (e.g., "temperature"), and add the `DataPoint` under that 
series to the `Import` object.

    imp.addDataPoint("temperature", d);

Here's another example that takes and stores a temperature reading every second:

    Import imp = new Import(DEVICE_ID, PROJECT_ID);
    while (true) {
        DataPoint d = new DataPoint(getTemperature());
        imp.addDataPoint("temperature", d);
        Thread.sleep(1000);
    }

Note that the `Import` object can hold several series at once. For example, 
if you also had a `getHumidity()` function, you could add both data points to the same
`Import`:

    Import imp = new Import(DEVICE_ID, PROJECT_ID);
    while (true) {
        DataPoint dt = new DataPoint(getTemperature());
        DataPoint dh = new DataPoint(getHumidity());

        imp.addDataPoint("temperature", dt);
        imp.addDataPoint("humidity", dh);

        Thread.sleep(1000);
    }


### Connecting to the iobeam Cloud ###

To send this data to the iobeam Cloud, you'll need an `Imports` service object, 
which takes an `Import` object and handles everything needed to communicate with our servers.
The `Imports` service, in turn, requires an `Iobeam` client.

First, initialize an `Iobeam` client and an `Imports` service, using your `project_id` and 
`project_token` (with write-access enabled).

    private static final long PROJECT_ID = ...;
    private static final String PROJECT_TOKEN = ...;

    private Iobeam client = Iobeam.init(PROJECT_ID, PROJECT_TOKEN);
    private Imports service = new Imports(client);

Now, you can start sending data via your `Import` object:

    Import imp = new Import(DEVICE_ID, PROJECT_ID);
    ...
    // Data gathering here
    ...
    Imports.Submit req = service.submit(imp);
    try {
        req.execute();
    } catch (ApiException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }

Data points are submitted when ``req.execute()`` is called. If there are problems with the
data as provided, an `ApiException` is thrown (e.g. incorrect project ID or device ID, invalid data,
etc). `IOException` is thrown in the case of network connectivity issues.


### Full Example ###

Here's the full source code for our example:

    // Initialization
    private static final long PROJECT_ID = ...;
    private static final String PROJECT_TOKEN = ...;
    private static final String DEVICE_ID = ...;

    private Iobeam client = Iobeam.init(PROJECT_ID, PROJECT_TOKEN);
    private Imports service = new Imports(client);

    ...

    // Data gathering
    Import imp = new Import(DEVICE_ID, PROJECT_ID);

    DataPoint dt = new DataPoint(getTemperature());
    DataPoint dh = new DataPoint(getHumidity());

    imp.addDataPoint("temperature", dt);
    imp.addDataPoint("humidity", dh);

    ...

    // Data transmission
    Imports.Submit req = service.submit(imp);

    try {
        req.execute();
    } catch (ApiException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }


These instructions should hopefully be enough to get you started with the library!