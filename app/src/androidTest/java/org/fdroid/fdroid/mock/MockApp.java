package org.fdroid.fdroid.mock;

import org.fdroid.fdroid.data.App;

public class MockApp extends App {

    public MockApp(String id) {
        this(id, "App " + id);
    }

    public MockApp(String id, String name) {
        this.packageName = id;
        this.name = name;
    }

}
