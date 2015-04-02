# Java / Android Library for the iobeam API #


## Installation ##

To install to your local Maven repository:

```mvn install```

It will be installed as artifact ```iobeam-client-java``` under the group ```com.iobeam```.

## Getting started ##

This library is meant to allow Java clients to import data into the iobeam Cloud. Applications will
make `DataPoint`s that can be be organized into different series of data in an `Import` resource.
As an example, let's assume we have created an app that measures the current temperature that can
be accessed by `getTemperature()`.

To create a DataPoint for a reading:

    double t = getTemperature();
    DataPoint d = new DataPoint(System.currentTimeInMillis(), t);

    // DataPoint d = new DataPoint(t); is equivalent to above

The timestamp provided should be in milliseconds since epoch. The value can be integral or real.

### Data series and `Import` ###

To organize your data points into series, you'll need an `Import` object. This object can store
multiple points so you do not necessarily need to create one for each `DataPoint`. `Import` objects
are associated with a particular project and device, and therefore need those two items to create
one:

    final String DEVICE_ID = ...;
    final long PROJECT_ID = ...;

    Import imp = new Import(DEVICE_ID, PROJECT_ID);

Now that you have this object you can add data to it:

    Import imp = new Import(DEVICE_ID, PROJECT_ID);
    while (true) {
        DataPoint d = new DataPoint(getTemperature());
        imp.addDataPoint("temp", d);
        Thread.sleep(1000);
    }
Here a new temperature reading is taken every second and added to the `Import` object to a series
called "temp".

The `Import` object can hold several series at once. For example, if you also have a
`getHumidity()` function, you could do:

    Import imp = new Import(DEVICE_ID, PROJECT_ID);
    while (true) {
        DataPoint dt = new DataPoint(getTemperature());
        DataPoint dh = new DataPoint(getHumidity());

        imp.addDataPoint("temp", dt);
        imp.addDataPoint("humidity", dh);

        Thread.sleep(1000);
    }

### Connecting to iobeam Cloud ###

Now, to actually get this data into the iobeam Cloud, you need to use an `Imports` service object.
This service object takes an `Import` object and handles everything needed to get it to our servers.
It requires some one-time setup before it can be used:

    private static final long PROJECT_ID = ...;
    private static final String PROJECT_TOKEN = ...;

    private Iobeam client = Iobeam.init(PROJECT_ID, PROJECT_TOKEN);
    private Imports service = new Imports(client);

You will need to have your project ID and token associated with it in order to submit data. Then
you initialize the `Iobeam` object with those credentials, followed by the `Imports` service with
your `Iobeam` instance. Now the service is ready to import data for you:

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

Here the data points are submitted when ``req.execute()`` is called. If there are problems with the
data as provided, an `ApiException` is thrown (e.g. incorrect project ID or device ID, invalid data,
etc). `IOException` is thrown in the case of network connectivity issues.

These instructions should hopefully be enough to get you started with the library!